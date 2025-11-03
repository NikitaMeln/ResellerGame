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
import com.reseller.game.mapper.GameRoomMapper;
import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.repository.CarRepository;
import com.reseller.game.repository.TuningRepository;
import com.reseller.game.service.PlayerService;
import com.reseller.game.service.RoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final PlayerService playerService;
    private final GameRoomMapper gameRoomMapper;
    private final SimpMessagingTemplate ws;
    private final CarRepository carRepository;
    private final TuningRepository tuningRepository;

    @MessageMapping("/room.join")        // /app/room.join
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

        // Reload room to ensure all collections are initialized
        GameRoom updatedRoom = roomService.getRoomById(room.getId());

        // Notify all players in room about state update
        ws.convertAndSend("/topic/room." + updatedRoom.getId() + ".state",
                gameRoomMapper.toDto(updatedRoom));

        // Send room assignment to the player who just joined
        // Using topic instead of user queue since we don't have authentication
        ws.convertAndSend(
                "/topic/player." + req.getTelegramId() + ".room-assigned",
                Map.of("roomId", updatedRoom.getId(), "roomState", updatedRoom.getState().name())
        );
    }

    @MessageMapping("/room.start")       // /app/room.start
    public void startGame(JoinRoomRequest req) {
        GameRoom room = roomService.getRoomById(Long.parseLong(req.getRoomId()));
        roomService.startGame(room);

        // Notify all players that game started
        ws.convertAndSend("/topic/room." + room.getId() + ".state",
                gameRoomMapper.toDto(room));

        // Notify current player about their turn
        Player currentPlayer = roomService.getCurrentPlayer(room);
        ws.convertAndSend("/topic/room." + room.getId() + ".turn",
                Map.of("currentPlayer", currentPlayer.getTelegramId(),
                        "turnStep", room.getTurnStep()));
    }

    @MessageMapping("/game.buyCar")      // /app/game.buyCar
    public void buyCar(BuyCarRequest req) {
        
        GameRoom room = roomService.getRoomById(req.getRoomId());
       
        Player player = playerService.findByTelegramIdWithCars(req.getTelegramId());
        Car car = carRepository.findById(req.getCarId())
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        roomService.processBuyCar(room, player, car);

        // Reload room to get updated player data with cars
        GameRoom updatedRoom = roomService.getRoomById(req.getRoomId());

        // Notify all players about room state update
        RoomStateDto dto = gameRoomMapper.toDto(updatedRoom);
        ws.convertAndSend("/topic/room." + updatedRoom.getId() + ".state", dto);

        // Notify current player about their turn step
        ws.convertAndSend("/topic/room." + updatedRoom.getId() + ".turn",
                Map.of("currentPlayer", player.getTelegramId(),
                        "turnStep", updatedRoom.getTurnStep(),
                        "negativeCard", player.getCurrentNegativeCard()));
    }

    @MessageMapping("/game.buyTuning")   // /app/game.buyTuning
    public void buyTuning(BuyTuningRequest req) {
        GameRoom room = roomService.getRoomById(req.getRoomId());
        Player player = playerService.findByTelegramId(req.getTelegramId());
        Tuning tuning = tuningRepository.findById(req.getTuningId())
                .orElseThrow(() -> new IllegalArgumentException("Tuning not found"));
        Car car = carRepository.findById(req.getCarId())
                .orElseThrow(() -> new IllegalArgumentException("Car not found"));

        roomService.processBuyTuning(room, player, tuning, car);

        // Notify all players about room state update
        ws.convertAndSend("/topic/room." + room.getId() + ".state",
                gameRoomMapper.toDto(room));

        // Notify next player about their turn
        Player nextPlayer = roomService.getCurrentPlayer(room);
        ws.convertAndSend("/topic/room." + room.getId() + ".turn",
                Map.of("currentPlayer", nextPlayer.getTelegramId(),
                        "turnStep", room.getTurnStep()));
    }

    @MessageMapping("/game.skip")        // /app/game.skip
    public void skipAction(SkipActionRequest req) {
        GameRoom room = roomService.getRoomById(req.getRoomId());
        Player player = playerService.findByTelegramId(req.getTelegramId());

        roomService.processSkipAction(room, player);

        // Notify all players about room state update
        ws.convertAndSend("/topic/room." + room.getId() + ".state",
                gameRoomMapper.toDto(room));

        // Notify next player about their turn
        Player nextPlayer = roomService.getCurrentPlayer(room);
        ws.convertAndSend("/topic/room." + room.getId() + ".turn",
                Map.of("currentPlayer", nextPlayer.getTelegramId(),
                        "turnStep", room.getTurnStep()));
    }
}

