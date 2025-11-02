package com.reseller.game.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.model.entity.types.TuningType;
import com.reseller.game.model.entity.types.TurnStep;
import com.reseller.game.repository.GameRoomRepository;
import com.reseller.game.service.RoomService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final static Integer TOTAL_TO_SOLD = 5;
    private final static Integer TOTAL_PROFIT = 10000;
    private final static Integer MAX_PLAYERS = 5;
    private final static Integer MIN_PLAYERS = 1; // For testing, can be 2 for production


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();

    private final GameRoomRepository gameRoomRepository;
    private final CarServiceImpl carServiceImpl;
    private final TuningServiceImpl tuningServiceImpl;
    private final ClientServiceImpl clientServiceImpl;
    private final SimpMessagingTemplate ws;
    private final com.reseller.game.mapper.GameRoomMapper gameRoomMapper;

    @Override
    @Transactional
    public GameRoom createRoom() {
        GameRoom room = new GameRoom();
        room.setState(RoomState.PENDING);
        room.setStartTime(LocalDateTime.now());

        // Initialize empty player lists
        room.setPlayers(new java.util.ArrayList<>());
        room.setPlayerQueue(new java.util.ArrayList<>());

        // Load all game data immediately so players can see cars while waiting
        List<Car> availableCars = carServiceImpl.getAllCars();
        List<Tuning> availableTunings = tuningServiceImpl.getAllTunings();
        List<Client> clients = clientServiceImpl.getAllClients();
        room.setCars(availableCars);
        room.setTunings(availableTunings);
        room.setClients(clients);

        gameRoomRepository.save(room);

        scheduleStart(room);

        return room;
    }

    private void scheduleStart(GameRoom room) {
        scheduler.schedule(() -> {
            // Reload room to get latest player count
            GameRoom managedRoom = gameRoomRepository.findById(room.getId()).orElse(null);
            if (managedRoom != null && managedRoom.getPlayers().size() >= 1) {
                log.info("Starting game for room {} with {} players", managedRoom.getId(), managedRoom.getPlayers().size());
                initializeRoom(managedRoom);
            } else {
                log.warn("Not enough players in room {}, closing room", room.getId());
            }
        }, 10, TimeUnit.SECONDS);
    }

    @Transactional
    private void initializeRoom(GameRoom room) {
        // Set game to STARTED state
        room.setState(RoomState.STARTED);
        room.setCurrentPlayerIndex(0);
        room.setTurnStep(TurnStep.CAR_SELECTION);

        // Initialize negative cards (debuff cards from tuning with NEGATIVE type)
        List<Tuning> negativeCards = tuningServiceImpl.getTuningsByType(TuningType.NEGATIVE);
        room.setNegativeCards(negativeCards);

        gameRoomRepository.save(room);

        // Reload room with all collections for WebSocket message
        GameRoom updatedRoom = gameRoomRepository.findById(room.getId()).orElseThrow();

        // Notify all players in room that game has started
        ws.convertAndSend("/topic/room." + updatedRoom.getId() + ".state",
                gameRoomMapper.toDto(updatedRoom));

        log.info("Game started for room {}", updatedRoom.getId());
    }

    private void startGameLoop(GameRoom room) {
        while (room.getState() == RoomState.STARTED) {

            Player currentPlayer = room.getPlayerQueue().isEmpty() ? null : room.getPlayerQueue().remove(0);
            if (currentPlayer == null) break;
            
            
            List<Car> carPull = carServiceImpl.getRandomCars(room.getCars(), (room.getPlayers().size() + 1));

            // Player turn logic
            // simulatePlayerTurn(currentPlayer, carPull);

            // Return player to Queue
            room.getPlayerQueue().add(currentPlayer); // Вернуть игрока в очередь
        }
    }

    private Tuning getRandomTuning(List<Tuning> tunings, TuningType type) {
        List<Tuning> filtered = tunings.stream()
            .filter(t -> t.getType() == type)
            .toList();
        return filtered.get(random.nextInt(filtered.size()));
    }

    // private void simulatePlayerTurn(Player player, List<Car> pull) {
    //     // Пример симуляции: игрок всегда покупает первую машину
    //     Car selected = pull.get(0);

    //     player.getCars().add(selected);

    //     // Случайная продажа с прибылью
    //     Integer profit = selected.getPrice().add(200);
    //     player.setTotalProfit(player.getTotalProfit().add(profit));
    //     player.setSoldCars(player.getSoldCars() + 1);

    //     // Статистика по тюнингу
    //     GameRoom.RoomStatistics stats = player.getRoom().getStatistics();
    //     stats.incrementTuning(selected.getPositiveTuning());
    //     stats.incrementTuning(selected.getNegativeTuning());
    // }

    private boolean checkWinCondition(Player player) {
        return player.getSoldCars() >= TOTAL_TO_SOLD ||
               player.getTotalProfit() >= TOTAL_PROFIT;
    }

    private void finishGame(GameRoom room) {
        room.setState(RoomState.RESULT);
        calculateStats(room);
        System.out.println("Game finished. Winner: ");
    }

    private void calculateStats(GameRoom room) {
    
        Integer totalProfit = 0;
        int totalSales = 0;

        for (Player p : room.getPlayers()) {
            totalProfit = totalProfit + (p.getTotalProfit());
            totalSales += p.getSoldCars();
        }

    }

    @Transactional
    @Override
    public GameRoom getRoomById(Long id) {
        return gameRoomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("GameRoom %d not found".formatted(id)));
    }

    @Transactional
    @Override
    public GameRoom getRoomByState(RoomState state) {
        return gameRoomRepository.findOldestByState(state).orElse(createRoom());
    }

    @Transactional
    @Override
    public void addPlayer(GameRoom room, Player player) {
        // Reload room to ensure players collection is initialized
        GameRoom managedRoom = gameRoomRepository.findById(room.getId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        List<Player> players = managedRoom.getPlayers();
        if (!players.contains(player)) {
            players.add(player);
        }

        // Also add to player queue
        List<Player> playerQueue = managedRoom.getPlayerQueue();
        if (!playerQueue.contains(player)) {
            playerQueue.add(player);
        }

        if(players.size() >= MAX_PLAYERS) {
            managedRoom.setState(RoomState.STARTED);
        }

        gameRoomRepository.save(managedRoom);
    }

    @Transactional
    @Override
    public void startGame(GameRoom room) {
        // Load all game data if not already loaded
        if (room.getCars() == null || room.getCars().isEmpty()) {
            List<Car> availableCars = carServiceImpl.getAllCars();
            room.setCars(availableCars);
        }

        if (room.getTunings() == null || room.getTunings().isEmpty()) {
            List<Tuning> availableTunings = tuningServiceImpl.getAllTunings();
            room.setTunings(availableTunings);
        }

        if (room.getClients() == null || room.getClients().isEmpty()) {
            List<Client> clients = clientServiceImpl.getAllClients();
            room.setClients(clients);
        }

        room.setState(RoomState.STARTED);
        room.setCurrentPlayerIndex(0);
        room.setTurnStep(TurnStep.CAR_SELECTION);

        // Initialize negative cards (debuff cards from tuning with NEGATIVE type)
        List<Tuning> negativeCards = tuningServiceImpl.getTuningsByType(TuningType.NEGATIVE);
        room.setNegativeCards(negativeCards);

        gameRoomRepository.save(room);
    }

    @Transactional
    @Override
    public void processBuyCar(GameRoom room, Player player, Car car) {
        if (!isCurrentPlayer(room, player)) {
            throw new IllegalStateException("Not this player's turn");
        }

        if (room.getTurnStep() != TurnStep.CAR_SELECTION) {
            throw new IllegalStateException("Cannot buy car at this step");
        }

        // Add car to player's garage
        player.getCars().add(car);

        // Deduct balance
        player.setBalance(player.getBalance() - car.getPrice().intValue());

        // Assign random negative card (debuff)
        Tuning negativeCard = getRandomTuning(room.getNegativeCards(), TuningType.NEGATIVE);
        player.setCurrentNegativeCard(negativeCard);

        // Move to tuning selection
        room.setTurnStep(TurnStep.TUNING_SELECTION);

        gameRoomRepository.save(room);
    }

    @Transactional
    @Override
    public void processBuyTuning(GameRoom room, Player player, Tuning tuning) {
        if (!isCurrentPlayer(room, player)) {
            throw new IllegalStateException("Not this player's turn");
        }

        if (room.getTurnStep() != TurnStep.TUNING_SELECTION) {
            throw new IllegalStateException("Cannot buy tuning at this step");
        }

        // Get the last car (recently bought)
        List<Car> playerCars = player.getCars();
        if (playerCars.isEmpty()) {
            throw new IllegalStateException("Player has no cars");
        }

        Car lastCar = playerCars.get(playerCars.size() - 1);

        // Add tuning to car
        carServiceImpl.setTuning(lastCar, tuning);

        // Deduct balance
        player.setBalance(player.getBalance() - tuning.getPrice().intValue());

        // Move to next player
        moveToNextPlayer(room);

        gameRoomRepository.save(room);
    }

    @Transactional
    @Override
    public void processSkipAction(GameRoom room, Player player) {
        if (!isCurrentPlayer(room, player)) {
            throw new IllegalStateException("Not this player's turn");
        }

        if (room.getTurnStep() == TurnStep.CAR_SELECTION) {
            // Skip entire turn - move to next player
            moveToNextPlayer(room);
        } else if (room.getTurnStep() == TurnStep.TUNING_SELECTION) {
            // Skip only tuning - move to next player
            moveToNextPlayer(room);
        }

        gameRoomRepository.save(room);
    }

    @Override
    public Player getCurrentPlayer(GameRoom room) {
        if (room.getPlayerQueue() == null || room.getPlayerQueue().isEmpty()) {
            return null;
        }

        Integer index = room.getCurrentPlayerIndex();
        if (index == null || index >= room.getPlayerQueue().size()) {
            return null;
        }

        return room.getPlayerQueue().get(index);
    }

    private boolean isCurrentPlayer(GameRoom room, Player player) {
        Player current = getCurrentPlayer(room);
        return current != null && current.getTelegramId().equals(player.getTelegramId());
    }

    private void moveToNextPlayer(GameRoom room) {
        Integer currentIndex = room.getCurrentPlayerIndex();
        int playerCount = room.getPlayerQueue().size();

        // Move to next player
        int nextIndex = (currentIndex + 1) % playerCount;
        room.setCurrentPlayerIndex(nextIndex);

        // Reset to car selection step
        room.setTurnStep(TurnStep.CAR_SELECTION);

        // If we completed a full round, check if we should move to next phase
        if (nextIndex == 0) {
            // All players completed their turn in BUY_FORWARD phase
            if (room.setTurnStep() == TurnStep.CAR_SELECTION) {
                room.setState(TurnStep.S);
                // Reverse the player order for selling phase
                List<Player> reversedQueue = new java.util.ArrayList<>(room.getPlayerQueue());
                java.util.Collections.reverse(reversedQueue);
                room.setPlayerQueue(reversedQueue);
            }
        }
    }
}

