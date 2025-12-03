package com.reseller.game.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.reseller.game.exception.CarNotFoundException;
import com.reseller.game.exception.GarageFullException;
import com.reseller.game.exception.InsufficientBalanceException;
import com.reseller.game.exception.PlayerNotFoundException;
import com.reseller.game.exception.TuningNotFoundException;
import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.TurnStep;
import com.reseller.game.repository.CarRepository;
import com.reseller.game.repository.ClientRepository;
import com.reseller.game.repository.TuningRepository;
import com.reseller.game.service.GameSessionService;
import com.reseller.game.session.CarInstance;
import com.reseller.game.session.GameSession;
import com.reseller.game.session.PlayerGameState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of GameSessionService.
 * Manages active game sessions in memory.
 * Each game session is stored in-memory and destroyed when game finishes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionServiceImpl implements GameSessionService {

    private static final Integer STARTING_BALANCE = 1300;
    private static final Integer GARAGE_CAPACITY = 3;

    private final Map<Long, GameSession> activeSessions = new ConcurrentHashMap<>();

    private final CarRepository carRepository;
    private final ClientRepository clientRepository;
    private final TuningRepository tuningRepository;

    @Override
    public GameSession createSession(GameRoom room) {
        if (activeSessions.containsKey(room.getId())) {
            log.warn("Session already exists for room {}", room.getId());
            return activeSessions.get(room.getId());
        }

        log.info("Creating new game session for room {}", room.getId());

        // Load all available cards from database (templates)
        List<Car> allCars = carRepository.findAll();
        List<Client> allClients = clientRepository.findAll();
        List<Tuning> allTunings = tuningRepository.findAll();

        // Separate positive and negative tunings
        List<Tuning> positiveTunings = allTunings.stream()
                .filter(t -> "POSITIVE".equals(t.getType().name()))
                .collect(Collectors.toList());
        List<Tuning> negativeTunings = allTunings.stream()
                .filter(t -> "NEGATIVE".equals(t.getType().name()))
                .collect(Collectors.toList());

        // Create player game states
        List<PlayerGameState> players = new ArrayList<>();
        for (Player player : room.getPlayerQueue()) {
            PlayerGameState pgs = PlayerGameState.builder()
                    .telegramId(player.getTelegramId())
                    .username(player.getUsername())
                    .balance(STARTING_BALANCE)
                    .garageCapacity(GARAGE_CAPACITY)
                    .garage(new ArrayList<>())
                    .build();
            players.add(pgs);
        }

        // Build game session
        GameSession session = GameSession.builder()
                .roomId(room.getId())
                .players(players)
                .availableCars(new ArrayList<>(allCars))
                .availableClients(new ArrayList<>(allClients))
                .availableTunings(new ArrayList<>(positiveTunings))
                .availableNegativeCards(new ArrayList<>(negativeTunings))
                .currentPlayerIndex(0)
                .turnStep(TurnStep.CAR_SELECTION)
                .build();

        activeSessions.put(room.getId(), session);
        log.info("Game session created for room {} with {} players", room.getId(), players.size());

        return session;
    }

    @Override
    public GameSession getSession(Long roomId) {
        GameSession session = activeSessions.get(roomId);
        if (session == null) {
            throw new IllegalStateException("No active game session for room " + roomId);
        }
        return session;
    }

    @Override
    public boolean hasSession(Long roomId) {
        return activeSessions.containsKey(roomId);
    }

    @Override
    public void removeSession(Long roomId) {
        GameSession session = activeSessions.remove(roomId);
        if (session != null) {
            log.info("Game session removed for room {}", roomId);
        }
    }

    @Override
    public CarInstance buyCar(Long roomId, String telegramId, Long carId) {
        GameSession session = getSession(roomId);
        PlayerGameState player = session.findPlayer(telegramId);

        if (player == null) {
            throw new PlayerNotFoundException(telegramId);
        }

        // Find car template
        Car carTemplate = session.findCarTemplate(carId);
        if (carTemplate == null) {
            throw new CarNotFoundException(carId);
        }

        // Validate player has sufficient balance
        int carPrice = carTemplate.getPrice().intValue();
        validatePlayerBalance(player, carPrice);

        // Create car instance from template
        CarInstance carInstance = CarInstance.fromTemplate(
                carTemplate.getId(),
                carTemplate.getModel(),
                carTemplate.getYear(),
                carTemplate.getPrice()
        );

        // Add to player garage
        if (!player.addCar(carInstance)) {
            throw new GarageFullException(player.getGarageCapacity());
        }

        // Deduct balance
        player.deductBalance(carPrice);

        // Remove from available pool
        session.removeCarFromPool(carId);

        log.info("Player {} bought car {} (instance: {})", telegramId, carId, carInstance.getInstanceId());

        return carInstance;
    }

    @Override
    public void buyTuning(Long roomId, String telegramId, Long tuningId, String carInstanceId) {
        GameSession session = getSession(roomId);
        PlayerGameState player = session.findPlayer(telegramId);

        if (player == null) {
            throw new PlayerNotFoundException(telegramId);
        }

        // Find tuning
        Tuning tuning = session.findTuning(tuningId);
        if (tuning == null) {
            throw new TuningNotFoundException(tuningId);
        }

        // Find car instance in player's garage
        CarInstance carInstance = player.findCarById(carInstanceId);
        if (carInstance == null) {
            throw new CarNotFoundException("Car not found in player's garage");
        }

        // Validate player has sufficient balance
        int tuningPrice = tuning.getPrice().intValue();
        validatePlayerBalance(player, tuningPrice);

        // Apply tuning to car instance
        carInstance.addTuning(tuning);

        // Deduct balance
        player.deductBalance(tuningPrice);

        // Remove from available pool
        session.removeTuningFromPool(tuningId);

        log.info("Player {} applied tuning {} to car {}", telegramId, tuningId, carInstanceId);
    }

    /**
     * Validates that a player has sufficient balance for a purchase.
     * Throws InsufficientBalanceException if the player cannot afford the price.
     *
     * @param player the player making the purchase
     * @param price the price of the item
     * @throws InsufficientBalanceException if player cannot afford the price
     */
    private void validatePlayerBalance(PlayerGameState player, int price) {
        if (!player.canAfford(price)) {
            throw new InsufficientBalanceException(price, player.getBalance());
        }
    }
}
