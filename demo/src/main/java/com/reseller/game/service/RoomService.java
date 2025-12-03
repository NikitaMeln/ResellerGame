package com.reseller.game.service;

import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.session.GameSession;

public interface RoomService {

    GameRoom createRoom();

    GameRoom getRoomById(Long id);

    GameRoom getRoomByState(RoomState state);

    void addPlayer(GameRoom room, Player player);

    void startGame(GameRoom room);

    void processBuyCar(Long roomId, String telegramId, Long carId);

    void processBuyTuning(Long roomId, String telegramId, Long tuningId, String carInstanceId);

    void processSkipAction(Long roomId, String telegramId);

    GameSession getGameSession(Long roomId);
}
