package com.reseller.game.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.reseller.game.mapper.GameSessionMapper;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.model.entity.types.TurnStep;
import com.reseller.game.repository.GameRoomRepository;
import com.reseller.game.service.GameSessionService;
import com.reseller.game.service.RoomService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {
    private final static Integer TOTAL_TO_SOLD = 5;
    private final static Integer TOTAL_PROFIT = 10000;
    private final static Integer MAX_PLAYERS = 5;
    private final static Integer MIN_PLAYERS = 1; // For prod, must be 2
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final GameRoomRepository gameRoomRepository;
    private final SimpMessagingTemplate ws;
    private final GameSessionMapper gameSessionMapper;
    private final TransactionTemplate transactionTemplate;
    private final GameSessionService gameSessionService;

    @Override
    @Transactional
    public GameRoom createRoom() {
        GameRoom room = new GameRoom();
        room.setState(RoomState.PENDING);
        room.setStartTime(LocalDateTime.now());

        // Initialize empty player lists
        room.setPlayers(new java.util.ArrayList<>());
        room.setPlayerQueue(new java.util.ArrayList<>());

        gameRoomRepository.save(room);

        scheduleStart(room);

        return room;
    }

    private void scheduleStart(GameRoom room) {
        scheduler.schedule(() -> {
            transactionTemplate.execute(status -> {
                try {
                    // Reload room to get latest player count with all associations eagerly loaded
                    GameRoom managedRoom = reloadRoom(room.getId());
                    if (managedRoom.getPlayers().size() >= MIN_PLAYERS) {
                        initializeRoom(managedRoom);
                    } else {
                        log.warn("Not enough players in room {}, closing room", room.getId());
                    }
                } catch (EntityNotFoundException e) {
                    log.warn("Room {} not found during scheduled start", room.getId());
                }
                return null;
            });
        }, 10, TimeUnit.SECONDS);
    }

    @Transactional
    private void initializeRoom(GameRoom room) {
        try {
            // Set game to STARTED state in DB
            room.setState(RoomState.STARTED);
            gameRoomRepository.save(room);

            // Create in-memory game session with all game data
            com.reseller.game.session.GameSession session = gameSessionService.createSession(room);

            // Reload room with all collections for WebSocket message
            GameRoom updatedRoom = reloadRoom(room.getId());

            // Notify all players in room that game has started (with full game session state)
            com.reseller.game.dto.RoomStateDto roomStateDto = gameSessionMapper.toDto(session, updatedRoom);
            ws.convertAndSend("/topic/room." + updatedRoom.getId() + ".state", roomStateDto);

            // Notify each player individually that game has started (for lobby navigation)
            for (Player p : updatedRoom.getPlayers()) {
                ws.convertAndSend("/topic/player." + p.getTelegramId() + ".room-assigned",
                        java.util.Map.of("roomId", updatedRoom.getId(),
                                         "roomState", updatedRoom.getState().name()));
            }

            log.info("Game started for room {}", updatedRoom.getId());

            // Notify current player about their turn (using GameSession state)
            com.reseller.game.session.PlayerGameState currentPlayer = session.getCurrentPlayer();
            if (currentPlayer != null) {
                ws.convertAndSend("/topic/room." + updatedRoom.getId() + ".turn",
                        java.util.Map.of("currentPlayer", currentPlayer.getTelegramId(),
                                "turnStep", session.getTurnStep()));
            }
        } catch (Exception e) {
            log.error("initializeRoom - ERROR initializing room {}: {}", room.getId(), e.getMessage(), e);
            throw e;
        }
    }

    // NOTE: Old helper methods removed (getRandomTuning, checkWinCondition, finishGame, calculateStats)
    // Game logic is now in GameSessionManager

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
        GameRoom managedRoom = reloadRoom(room.getId());

        List<Player> players = managedRoom.getPlayers();
        log.info("addPlayer - Room {} current players: {}", managedRoom.getId(), players.size());
        boolean isFirstPlayer = players.isEmpty();
        log.info("addPlayer - isFirstPlayer: {}", isFirstPlayer);

        if (!players.contains(player)) {
            players.add(player);
            log.info("addPlayer - Added player {} to room {}", player.getTelegramId(), managedRoom.getId());
        } else {
            log.info("addPlayer - Player {} already in room {}", player.getTelegramId(), managedRoom.getId());
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
        // Schedule auto-start when first player joins
        if (isFirstPlayer) {
            log.info("First player joined room {}, scheduling auto-start", managedRoom.getId());
            scheduleStart(managedRoom);
        } else {
            log.info("Not scheduling - room {} already has {} players", managedRoom.getId(), players.size());
        }
    }

    @Transactional
    @Override
    public void startGame(GameRoom room) {
        // Create in-memory game session with all game data
        gameSessionService.createSession(room);

        // Update room state in DB
        room.setState(RoomState.STARTED);
        gameRoomRepository.save(room);

        log.info("Game started for room {} with {} players", room.getId(), room.getPlayerQueue().size());
    }

    @Transactional
    @Override
    public void processBuyCar(Long roomId, String telegramId, Long carId) {
        // Use GameSessionService for in-memory game state
        gameSessionService.buyCar(roomId, telegramId, carId);

        // Update turn state in session
        com.reseller.game.session.GameSession session = gameSessionService.getSession(roomId);
        session.setTurnStep(TurnStep.TUNING_SELECTION);

        log.info("Player {} bought car {} in room {}", telegramId, carId, roomId);
    }

    @Transactional
    @Override
    public void processBuyTuning(Long roomId, String telegramId, Long tuningId, String carInstanceId) {
        // Use GameSessionService for in-memory game state
        gameSessionService.buyTuning(roomId, telegramId, tuningId, carInstanceId);

        // Move to next player
        com.reseller.game.session.GameSession session = gameSessionService.getSession(roomId);
        session.moveToNextPlayer();

        log.info("Player {} bought tuning {} for car {} in room {}",
                telegramId, tuningId, carInstanceId, roomId);
    }

    @Transactional
    @Override
    public void processSkipAction(Long roomId, String telegramId) {
        com.reseller.game.session.GameSession session = gameSessionService.getSession(roomId);

        // Verify it's this player's turn
        if (!session.isCurrentPlayer(telegramId)) {
            throw new IllegalStateException("Not this player's turn");
        }

        // Skip - move to next player regardless of turn step
        session.moveToNextPlayer();

        log.info("Player {} skipped turn in room {}", telegramId, roomId);
    }

    @Override
    public com.reseller.game.session.GameSession getGameSession(Long roomId) {
        return gameSessionService.getSession(roomId);
    }

    /**
     * Reloads a room from the database to ensure all collections are initialized.
     * This is necessary in JPA/Hibernate to avoid LazyInitializationException.
     *
     * @param roomId the ID of the room to reload
     * @return the reloaded room with all collections eagerly loaded
     * @throws EntityNotFoundException if the room is not found
     */
    private GameRoom reloadRoom(Long roomId) {
        return gameRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room with ID %d not found".formatted(roomId)));
    }
}

