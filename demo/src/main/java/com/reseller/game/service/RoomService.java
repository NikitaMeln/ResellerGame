package com.reseller.game.service;

import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;

public interface RoomService {

    GameRoom createRoom();

    GameRoom getRoomById(Long id);

    GameRoom getRoomByState(RoomState state);

    void addPlayer(GameRoom room, Player player);
}
