package com.reseller.game.service;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import com.reseller.game.repository.GameRoomRepository;

@Service
public class RoomService {
    private final static Integer TOTAL_TO_SOLD = 5;
    private final static Integer TOTAL_PROFIT = 10000;


    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Random random = new Random();

    private final GameRoomRepository gameRoomRepository;
    private final CarService carService;
    private final TuningService tuningService;
    private final ClientService clientService;

    public RoomService(CarService carService, TuningService tuningService, ClientService clientService, GameRoomRepository gameRoomRepository) {
        this.gameRoomRepository = gameRoomRepository;
        this.carService = carService;
        this.tuningService = tuningService;
        this.clientService = clientService;
    }

    public GameRoom createRoom(List<Player> players) {
        GameRoom room = new GameRoom();
        room.setPlayers(players);
        room.setState(RoomState.PENDING);
        room.setStartTime(LocalDateTime.now());
        //room.setStatistics(new Room.RoomStatistics());
        gameRoomRepository.save(room);

        scheduleStart(room);

        return room;
    }

    private void scheduleStart(GameRoom room) {
        scheduler.schedule(() -> {
            if (room.getPlayers().size() >= 1) {
                room.setState(RoomState.CONNECTING);
                initializeRoom(room);
            } else {
                System.out.println("Not enough players, room must closed.");
            }
        }, 10, TimeUnit.SECONDS);
    }

    private void initializeRoom(GameRoom room) {
        // Prepare all items for game
        List<Car> availableCars = carService.getAllCars();
        List<Tuning> availableTunings = tuningService.getAllTunnings();
        List<Client> clients = clientService.getAllClients();
        room.setCars(availableCars);
        room.setTunings(availableTunings);
        room.setClients(clients);
        room.setPlayerQueue(new LinkedList<>(room.getPlayers()));
        room.setState(RoomState.STARTED);
        startGameLoop(room);
    }

    private void startGameLoop(GameRoom room) {
        while (room.getState() == RoomState.STARTED) {
            Player currentPlayer = room.getPlayerQueue().poll();
            if (currentPlayer == null) break;
            
            
            List<Car> carPull = carService.getRandomCars(room.getCars(), (room.getPlayers().size() + 1));

            // Обработка хода игрока
            // simulatePlayerTurn(currentPlayer, carPull);

            room.getPlayerQueue().offer(currentPlayer); // Вернуть игрока в очередь
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

    public Optional<GameRoom> getRoomById(Long id) {
        return Optional.ofNullable(gameRoomRepository.findById(id)).orElseThrow();
    }
}

