package com.reseller.game.service;

import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.RoomState;

public interface RoomService {

    GameRoom createRoom();

    GameRoom getRoomById(Long id);

    GameRoom getRoomByState(RoomState state);

    void addPlayer(GameRoom room, Player player);

    void startGame(GameRoom room);

    void processBuyCar(GameRoom room, Player player, Car car);

    void processBuyTuning(GameRoom room, Player player, Tuning tuning, Car car);

    void processSkipAction(GameRoom room, Player player);

    Player getCurrentPlayer(GameRoom room);
}
