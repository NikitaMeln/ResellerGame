package com.reseller.game.service;

import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.session.CarInstance;
import com.reseller.game.session.GameSession;

/**
 * Service interface for managing in-memory game sessions.
 * Each game session is stored in-memory then saved results 
 * in base and destroyed when game finishes.
 */
public interface GameSessionService {

    GameSession createSession(GameRoom room);

    GameSession getSession(Long roomId);

    boolean hasSession(Long roomId);

    void removeSession(Long roomId);

    CarInstance buyCar(Long roomId, String telegramId, Long carId);

    void buyTuning(Long roomId, String telegramId, Long tuningId, String carInstanceId);
}
