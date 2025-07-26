package com.reseller.game.controller;

import com.reseller.game.dto.JoinRoomRequest;
import com.reseller.game.dto.RoomStateResponse;
import com.reseller.game.mapper.GameRoomMapper;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.service.PlayerService;
import com.reseller.game.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final PlayerService playerService;
    private final GameRoomMapper gameRoomMapper;
    private final SimpMessagingTemplate ws;

    @MessageMapping("/room.join")        // /app/room.join
    public void join(JoinRoomRequest req, Principal principal) {
        Player player = playerService.findByTelegramId(req.getTelegramId());

        GameRoom room = roomService.getRoomByState(RoomState.PENDING);

        roomService.addPlayer(room, player);
        ws.convertAndSend("/topic/room." + room.getId() + ".state",
                gameRoomMapper.toDto(room));
    }
}

