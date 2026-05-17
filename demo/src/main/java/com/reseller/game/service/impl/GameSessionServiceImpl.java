package com.reseller.game.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.reseller.game.exception.CarNotFoundException;
import com.reseller.game.exception.GarageFullException;
import com.reseller.game.exception.InsufficientBalanceException;
import com.reseller.game.exception.PlayerNotFoundException;
import com.reseller.game.exception.TuningNotFoundException;
import com.reseller.game.exception.IllegalActionException;
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
import com.reseller.game.session.CurrentSale;
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
    private static final int DICE_MIN = 1;
    private static final int DICE_MAX = 6;
    private static final int WIN_SOLD_CARS = 5;
    private static final int WIN_TOTAL_PROFIT = 10000;

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

        // Shuffle each pool once at session creation so every game starts in a different order.
        List<Car> shuffledCars = new ArrayList<>(allCars);
        List<Client> shuffledClients = new ArrayList<>(allClients);
        List<Tuning> shuffledPositive = new ArrayList<>(positiveTunings);
        List<Tuning> shuffledNegative = new ArrayList<>(negativeTunings);
        Collections.shuffle(shuffledCars);
        Collections.shuffle(shuffledClients);
        Collections.shuffle(shuffledPositive);
        Collections.shuffle(shuffledNegative);

        // Build game session
        GameSession session = GameSession.builder()
                .roomId(room.getId())
                .players(players)
                .availableCars(shuffledCars)
                .availableClients(shuffledClients)
                .availableTunings(shuffledPositive)
                .availableNegativeCards(shuffledNegative)
                .currentPlayerIndex(0)
                .turnStep(TurnStep.CAR_SELECTION)
                .build();

        // Draw initial shop window (playerCount + 1 cards of each kind).
        session.initialiseVisible(players.size() + 1);

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

    @Override
    public void processBuyCarAction(Long roomId, String telegramId, Long carId) {
        // Buy car (atomic operation)
        CarInstance carInstance = buyCar(roomId, telegramId, carId);

        // Attach a hidden negative card to this specific car (revealed later, in SHOW_SECRET_CARD).
        GameSession session = getSession(roomId);
        attachHiddenNegativeCard(session, carInstance);

        // Advance turn step to tuning selection
        session.setTurnStep(TurnStep.TUNING_SELECTION);

        log.info("Player {} completed car purchase in room {}, advanced to TUNING_SELECTION", telegramId, roomId);
    }

    /**
     * Pull a random negative card from the pool and attach it (face-down) to the car instance.
     * If the pool is empty, leaves the slot empty - dice resolution will just skip the modifier.
     */
    private void attachHiddenNegativeCard(GameSession session, CarInstance carInstance) {
        List<Tuning> pool = session.getAvailableNegativeCards();
        if (pool == null || pool.isEmpty()) {
            log.warn("Negative card pool is empty in room {}, no card attached to car {}",
                    session.getRoomId(), carInstance.getInstanceId());
            return;
        }
        int idx = ThreadLocalRandom.current().nextInt(pool.size());
        Tuning card = pool.remove(idx);
        carInstance.setHiddenNegativeCard(card);
        carInstance.setNegativeCardRevealed(false);
    }

    @Override
    public void processBuyTuningAction(Long roomId, String telegramId, Long tuningId, String carInstanceId) {
        // Buy and apply tuning (atomic operation)
        buyTuning(roomId, telegramId, tuningId, carInstanceId);

        // Move to next player
        GameSession session = getSession(roomId);
        session.moveToNextPlayer();

        log.info("Player {} completed tuning purchase in room {}, advanced to next player", telegramId, roomId);
    }

    @Override
    public void processSkipAction(Long roomId, String telegramId) {
        GameSession session = getSession(roomId);

        // Verify it's this player's turn
        if (!session.isCurrentPlayer(telegramId)) {
            throw new IllegalActionException("Not this player's turn");
        }

        // Skip - move to next player regardless of turn step
        session.moveToNextPlayer();

        log.info("Player {} skipped turn in room {}, advanced to next player", telegramId, roomId);
    }

    @Override
    public void processChooseClientAndCarAction(Long roomId, String telegramId, Long clientId, String carInstanceId) {
        GameSession session = getSession(roomId);
        requireTurn(session, telegramId, TurnStep.CHOICE_CLIENT_TO_SELL);

        PlayerGameState player = session.findPlayer(telegramId);
        if (player == null) {
            throw new PlayerNotFoundException(telegramId);
        }

        Client client = session.findClient(clientId);
        if (client == null) {
            throw new IllegalActionException("Client " + clientId + " is not available");
        }

        CarInstance car = player.findCarById(carInstanceId);
        if (car == null) {
            throw new CarNotFoundException("Car " + carInstanceId + " not found in player's garage");
        }

        validateClientCarMatch(client, car);

        session.setCurrentSale(CurrentSale.builder()
                .sellerTelegramId(telegramId)
                .clientId(clientId)
                .carInstanceId(carInstanceId)
                .build());

        session.setTurnStep(TurnStep.SHOW_SECRET_CARD);

        log.info("Player {} picked client {} for car {} in room {}, advanced to SHOW_SECRET_CARD",
                telegramId, clientId, carInstanceId, roomId);
    }

    /**
     * Enforce the year-range and stockOrNot filters declared by the client.
     */
    private void validateClientCarMatch(Client client, CarInstance car) {
        int decade = parseDecade(client.getYearForPurchase());
        int carYear;
        try {
            carYear = Integer.parseInt(car.getYear());
        } catch (NumberFormatException e) {
            throw new IllegalActionException("Car year is not a valid integer: " + car.getYear());
        }
        if (carYear < decade || carYear > decade + 9) {
            throw new IllegalActionException(
                    "Client wants a car from " + decade + "-x, but this car is " + carYear);
        }

        boolean wantsStock = Boolean.TRUE.equals(client.getStockOrNot());
        boolean isStock = car.getAppliedTunings() == null || car.getAppliedTunings().isEmpty();
        if (wantsStock && !isStock) {
            throw new IllegalActionException("Client wants a stock car, but this one has tuning");
        }
        if (!wantsStock && isStock) {
            throw new IllegalActionException("Client wants a tuned car, but this one is stock");
        }
    }

    /**
     * Parse a "YYYY-x" decade marker into its start year. "1990-x" -> 1990.
     */
    private int parseDecade(String yearForPurchase) {
        if (yearForPurchase == null) {
            throw new IllegalActionException("Client has no yearForPurchase set");
        }
        String trimmed = yearForPurchase.trim();
        int dash = trimmed.indexOf('-');
        String head = dash > 0 ? trimmed.substring(0, dash) : trimmed;
        try {
            return Integer.parseInt(head);
        } catch (NumberFormatException e) {
            throw new IllegalActionException("Invalid yearForPurchase format: " + yearForPurchase);
        }
    }

    @Override
    public void processRevealSecretCardAction(Long roomId, String telegramId) {
        GameSession session = getSession(roomId);
        requireTurn(session, telegramId, TurnStep.SHOW_SECRET_CARD);
        CurrentSale sale = requireSale(session, telegramId);

        CarInstance car = session.findPlayer(telegramId).findCarById(sale.getCarInstanceId());
        if (car == null) {
            throw new CarNotFoundException("Car " + sale.getCarInstanceId() + " missing during reveal");
        }
        car.setNegativeCardRevealed(true);
        session.setTurnStep(TurnStep.TURN_MULTIPLIER);

        log.info("Player {} revealed secret card on car {} in room {}, advanced to TURN_MULTIPLIER",
                telegramId, sale.getCarInstanceId(), roomId);
    }

    @Override
    public void processRollDiceAction(Long roomId, String telegramId, int dice) {
        if (dice < DICE_MIN || dice > DICE_MAX) {
            throw new IllegalActionException("Dice value must be in [" + DICE_MIN + "," + DICE_MAX + "], got " + dice);
        }

        GameSession session = getSession(roomId);
        requireTurn(session, telegramId, TurnStep.TURN_MULTIPLIER);
        CurrentSale sale = requireSale(session, telegramId);

        PlayerGameState player = session.findPlayer(telegramId);
        Client client = session.findClient(sale.getClientId());
        if (client == null) {
            throw new IllegalActionException("Client " + sale.getClientId() + " no longer available");
        }
        CarInstance car = player.findCarById(sale.getCarInstanceId());
        if (car == null) {
            throw new CarNotFoundException("Car " + sale.getCarInstanceId() + " missing during roll");
        }

        int threshold = computeThreshold(client, car);
        boolean success = dice >= threshold;

        int profit = 0;
        if (success) {
            profit = computeProfit(client, car);
            player.addBalance(profit);
            player.setSoldCars(player.getSoldCars() + 1);
            player.setTotalProfit(player.getTotalProfit() + profit);
            player.removeCar(car);
        }

        sale.setDiceValue(dice);
        sale.setThreshold(threshold);
        sale.setSuccess(success);
        sale.setProfit(profit);

        session.setTurnStep(TurnStep.RESULT);

        checkWinCondition(session, player);

        log.info("Player {} rolled {} (need >= {}) in room {}: success={}, profit={}",
                telegramId, dice, threshold, roomId, success, profit);
    }

    /**
     * Threshold = client.randomCounter + sum(addToRandomModifier) over applied tunings
     * + (revealed negative card's addToRandomModifier, if any).
     * A lower threshold makes success more likely.
     */
    private int computeThreshold(Client client, CarInstance car) {
        int threshold = client.getRandomCounter() != null ? client.getRandomCounter() : 3;
        if (car.getAppliedTunings() != null) {
            for (Tuning t : car.getAppliedTunings()) {
                threshold += readIntProperty(t, "addToRandomModifier");
            }
        }
        Tuning neg = car.getHiddenNegativeCard();
        if (neg != null && car.isNegativeCardRevealed()) {
            threshold += readIntProperty(neg, "addToRandomModifier");
        }
        return threshold;
    }

    /**
     * Profit = client.budget - car.basePrice - sum(tunings.price). Negative results are clamped to 0,
     * which represents a bad bargain that the player simply doesn't get paid for.
     */
    private int computeProfit(Client client, CarInstance car) {
        BigDecimal carTotal = car.getTotalPrice() != null ? car.getTotalPrice() : car.getBasePrice();
        BigDecimal budget = client.getBudget() != null ? client.getBudget() : BigDecimal.ZERO;
        BigDecimal profit = budget.subtract(carTotal);
        if (profit.signum() < 0) {
            return 0;
        }
        return profit.intValue();
    }

    private int readIntProperty(Tuning t, String key) {
        if (t.getProperties() == null) return 0;
        Object v = t.getProperties().get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    @Override
    public void processNextTurnAction(Long roomId, String telegramId) {
        GameSession session = getSession(roomId);
        requireTurn(session, telegramId, TurnStep.RESULT);
        CurrentSale sale = requireSale(session, telegramId);

        // The client is consumed regardless of outcome (they "leave" the market).
        session.removeClientFromPool(sale.getClientId());

        session.setCurrentSale(null);

        // Game already over from this player's win - don't advance turns.
        if (session.getWinnerTelegramId() != null) {
            log.info("Game over in room {}, winner already set ({}), not advancing turn",
                    roomId, session.getWinnerTelegramId());
            return;
        }

        session.moveToNextPlayer();
        log.info("Player {} acknowledged RESULT in room {}, advanced to next player (phase={}, step={})",
                telegramId, roomId, session.getPhase(), session.getTurnStep());
    }

    /**
     * Mark the player as winner if they hit either threshold. Idempotent.
     */
    private void checkWinCondition(GameSession session, PlayerGameState player) {
        if (session.getWinnerTelegramId() != null) return;
        if (player.getSoldCars() >= WIN_SOLD_CARS || player.getTotalProfit() >= WIN_TOTAL_PROFIT) {
            session.setWinnerTelegramId(player.getTelegramId());
            log.info("Player {} won the game in room {} (sold={}, profit={})",
                    player.getTelegramId(), session.getRoomId(),
                    player.getSoldCars(), player.getTotalProfit());
        }
    }

    /**
     * Common precondition check: it's this player's turn AND the expected turn step is active.
     */
    private void requireTurn(GameSession session, String telegramId, TurnStep expected) {
        if (!session.isCurrentPlayer(telegramId)) {
            throw new IllegalActionException("Not this player's turn");
        }
        if (session.getTurnStep() != expected) {
            throw new IllegalActionException("Expected turn step " + expected + " but session is in " + session.getTurnStep());
        }
    }

    private CurrentSale requireSale(GameSession session, String telegramId) {
        CurrentSale sale = session.getCurrentSale();
        if (sale == null || !Objects.equals(sale.getSellerTelegramId(), telegramId)) {
            throw new IllegalActionException("No active sale for this player");
        }
        return sale;
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
