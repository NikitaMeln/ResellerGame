package com.reseller.game.controller;

import com.reseller.game.dto.RoomStateDto;
import com.reseller.game.dto.req.BuyCarRequest;
import com.reseller.game.dto.req.BuyTuningRequest;
import com.reseller.game.dto.req.JoinRoomRequest;
import com.reseller.game.dto.req.SkipActionRequest;
import com.reseller.game.mapper.GameSessionMapper;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.service.GameSessionService;
import com.reseller.game.service.PlayerService;
import com.reseller.game.service.RoomService;
import com.reseller.game.session.GameSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Game API", description = "REST endpoints for testing game logic")
public class GameRestController {

    private final RoomService roomService;
    private final GameSessionService gameSessionService;
    private final PlayerService playerService;
    private final GameSessionMapper gameSessionMapper;

    @PostMapping("/room/join")
    @Operation(summary = "Join a game room", description = "Player joins an available room or creates a new one")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined room",
                    content = @Content(schema = @Schema(implementation = RoomStateDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RoomStateDto> joinRoom(@RequestBody JoinRoomRequest request) {
        log.info("REST: Player {} joining room", request.getTelegramId());

        Player player = playerService.createOrGetPlayer(
                request.getTelegramId(),
                request.getUsername(),
                request.getLanguage()
        );

        GameRoom room = roomService.getRoomByState(RoomState.PENDING);
        if (room == null) {
            room = roomService.createRoom();
        }

        roomService.addPlayer(room, player);
        GameRoom updatedRoom = roomService.getRoomById(room.getId());
        RoomStateDto dto = gameSessionMapper.toDtoFromRoom(updatedRoom);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/room/{roomId}/start")
    @Operation(summary = "Start a game", description = "Manually start a game in a room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Game started successfully",
                    content = @Content(schema = @Schema(implementation = RoomStateDto.class))),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<RoomStateDto> startGame(
            @Parameter(description = "Room ID") @PathVariable Long roomId) {
        log.info("REST: Starting game in room {}", roomId);

        GameRoom room = roomService.getRoomById(roomId);
        roomService.startGame(room);

        GameSession session = gameSessionService.getSession(roomId);
        GameRoom updatedRoom = roomService.getRoomById(roomId);
        RoomStateDto dto = gameSessionMapper.toDto(session, updatedRoom);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get room state", description = "Retrieve current state of a game room")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Room state retrieved",
                    content = @Content(schema = @Schema(implementation = RoomStateDto.class))),
            @ApiResponse(responseCode = "404", description = "Room not found")
    })
    public ResponseEntity<RoomStateDto> getRoomState(
            @Parameter(description = "Room ID") @PathVariable Long roomId) {
        log.info("REST: Getting state for room {}", roomId);

        GameRoom room = roomService.getRoomById(roomId);

        if (room.getState() == RoomState.STARTED) {
            GameSession session = gameSessionService.getSession(roomId);
            RoomStateDto dto = gameSessionMapper.toDto(session, room);
            return ResponseEntity.ok(dto);
        } else {
            RoomStateDto dto = gameSessionMapper.toDtoFromRoom(room);
            return ResponseEntity.ok(dto);
        }
    }

    @PostMapping("/action/buy-car")
    @Operation(summary = "Buy a car", description = "Player buys a car from the market")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Car purchased successfully",
                    content = @Content(schema = @Schema(implementation = RoomStateDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    })
    public ResponseEntity<RoomStateDto> buyCar(@RequestBody BuyCarRequest request) {
        log.info("REST: Player {} buying car {} in room {}",
                request.getTelegramId(), request.getCarId(), request.getRoomId());

        gameSessionService.processBuyCarAction(request.getRoomId(), request.getTelegramId(), request.getCarId());

        GameSession session = gameSessionService.getSession(request.getRoomId());
        GameRoom room = roomService.getRoomById(request.getRoomId());
        RoomStateDto dto = gameSessionMapper.toDto(session, room);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/action/buy-tuning")
    @Operation(summary = "Buy tuning", description = "Player applies tuning to a car")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tuning applied successfully",
                    content = @Content(schema = @Schema(implementation = RoomStateDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    })
    public ResponseEntity<RoomStateDto> buyTuning(@RequestBody BuyTuningRequest request) {
        log.info("REST: Player {} buying tuning {} for car {} in room {}",
                request.getTelegramId(), request.getTuningId(), request.getCarId(), request.getRoomId());

        gameSessionService.processBuyTuningAction(
                request.getRoomId(),
                request.getTelegramId(),
                request.getTuningId(),
                request.getCarId()
        );

        GameSession session = gameSessionService.getSession(request.getRoomId());
        GameRoom room = roomService.getRoomById(request.getRoomId());
        RoomStateDto dto = gameSessionMapper.toDto(session, room);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/action/skip")
    @Operation(summary = "Skip action", description = "Player skips current action (e.g., skip tuning)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Action skipped successfully",
                    content = @Content(schema = @Schema(implementation = RoomStateDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<RoomStateDto> skipAction(@RequestBody SkipActionRequest request) {
        log.info("REST: Player {} skipping action in room {}",
                request.getTelegramId(), request.getRoomId());

        gameSessionService.processSkipAction(request.getRoomId(), request.getTelegramId());

        GameSession session = gameSessionService.getSession(request.getRoomId());
        GameRoom room = roomService.getRoomById(request.getRoomId());
        RoomStateDto dto = gameSessionMapper.toDto(session, room);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if API is running")
    @ApiResponse(responseCode = "200", description = "API is healthy")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Reseller Game API",
                "version", "1.0"
        ));
    }
}
