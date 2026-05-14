package com.reseller.game.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.reseller.game.dto.RoomStateDto;
import com.reseller.game.dto.req.BuyCarRequest;
import com.reseller.game.dto.req.BuyTuningRequest;
import com.reseller.game.dto.req.JoinRoomRequest;
import com.reseller.game.dto.req.SkipActionRequest;
import com.reseller.game.exception.GameException;
import com.reseller.game.mapper.GameSessionMapper;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.service.GameSessionService;
import com.reseller.game.service.PlayerService;
import com.reseller.game.service.RoomService;
import com.reseller.game.session.GameSession;
import com.reseller.game.session.PlayerGameState;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket controller for game room operations.
 *
 * Architecture:
 * - Uses RoomService for persistent room management (DB operations)
 * - Uses GameSessionService for in-memory game logic (session operations)
 * - Broadcasts game state updates via WebSocket after each action
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final GameSessionService gameSessionService;
    private final PlayerService playerService;
    private final GameSessionMapper gameSessionMapper;
    private final SimpMessagingTemplate ws;

    @MessageMapping("/room.join")
    @org.springframework.transaction.annotation.Transactional
    public void join(JoinRoomRequest req, Principal principal) {
        // Create or get player
        Player player = playerService.createOrGetPlayer(
                req.getTelegramId(),
                req.getUsername() != null ? req.getUsername() : "default-user",
                req.getLanguage() != null ? req.getLanguage() : "eng"
        );

        GameRoom room = roomService.getRoomByState(RoomState.PENDING);

        roomService.addPlayer(room, player);

        GameRoom updatedRoom = roomService.getRoomById(room.getId());

        RoomStateDto dto = gameSessionMapper.toDtoFromRoom(updatedRoom);

        ws.convertAndSend("/topic/room." + updatedRoom.getId() + ".state", dto);

        ws.convertAndSend(
                "/topic/player." + req.getTelegramId() + ".room-assigned",
                Map.of("roomId", updatedRoom.getId(), "roomState", updatedRoom.getState().name())
        );
    }

    @MessageMapping("/room.start")
    public void startGame(JoinRoomRequest req) {
        GameRoom room = roomService.getRoomById(Long.parseLong(req.getRoomId()));
        roomService.startGame(room);

        broadcastGameState(room.getId());
    }

    @MessageMapping("/game.buyCar")
    public void buyCar(BuyCarRequest req) {
        log.info("buyCar - RoomId: {}, TelegramId: {}, CarId: {}",
                req.getRoomId(), req.getTelegramId(), req.getCarId());

        runGameAction(req.getRoomId(), req.getTelegramId(), () ->
                gameSessionService.processBuyCarAction(req.getRoomId(), req.getTelegramId(), req.getCarId()));
    }

    @MessageMapping("/game.buyTuning")
    public void buyTuning(BuyTuningRequest req) {
        log.info("buyTuning - RoomId: {}, TelegramId: {}, TuningId: {}, CarInstanceId: {}",
                req.getRoomId(), req.getTelegramId(), req.getTuningId(), req.getCarId());

        runGameAction(req.getRoomId(), req.getTelegramId(), () ->
                gameSessionService.processBuyTuningAction(req.getRoomId(), req.getTelegramId(),
                        req.getTuningId(), req.getCarId()));
    }

    @MessageMapping("/game.skip")
    public void skipAction(SkipActionRequest req) {
        log.info("skipAction - RoomId: {}, TelegramId: {}", req.getRoomId(), req.getTelegramId());

        runGameAction(req.getRoomId(), req.getTelegramId(), () ->
                gameSessionService.processSkipAction(req.getRoomId(), req.getTelegramId()));
    }

    /**
     * Run an in-session game action. Expected rule violations (GameException) are sent
     * back to the player who triggered the action via /topic/player.{id}.error, and the
     * room state is NOT re-broadcast (no state change happened). Successful actions
     * broadcast the new state to everyone.
     */
    private void runGameAction(Long roomId, String telegramId, Runnable action) {
        try {
            action.run();
        } catch (GameException e) {
            log.info("Game rule violation for player {} in room {}: {}", telegramId, roomId, e.getMessage());
            ws.convertAndSend("/topic/player." + telegramId + ".error",
                    Map.of("message", e.getMessage(), "type", e.getClass().getSimpleName()));
            return;
        }
        broadcastGameState(roomId);
    }

    private void broadcastGameState(Long roomId) {
        GameSession session = gameSessionService.getSession(roomId);
        GameRoom room = roomService.getRoomById(roomId);

        RoomStateDto dto = gameSessionMapper.toDto(session, room);
        ws.convertAndSend("/topic/room." + room.getId() + ".state", dto);

        PlayerGameState currentPlayer = session.getCurrentPlayer();
        if (currentPlayer != null) {
            ws.convertAndSend("/topic/room." + room.getId() + ".turn",
                    Map.of("currentPlayer", currentPlayer.getTelegramId(),
                            "turnStep", session.getTurnStep()));
        }
    }
}

