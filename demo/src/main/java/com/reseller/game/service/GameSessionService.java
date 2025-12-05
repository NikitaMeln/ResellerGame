package com.reseller.game.service;

import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.session.CarInstance;
import com.reseller.game.session.GameSession;

/**
 * Service interface for managing in-memory game sessions.
 * Each game session is stored in-memory then saved results
 * in base and destroyed when game finishes.
 *
 * Responsible for:
 * - In-memory session lifecycle (create, get, remove)
 * - Atomic game operations (buyCar, buyTuning)
 * - High-level game logic (turn progression, step advancement)
 */
public interface GameSessionService {

    // ===== Session Lifecycle =====

    GameSession createSession(GameRoom room);

    GameSession getSession(Long roomId);

    boolean hasSession(Long roomId);

    void removeSession(Long roomId);

    // ===== Atomic Game Operations =====

    CarInstance buyCar(Long roomId, String telegramId, Long carId);

    void buyTuning(Long roomId, String telegramId, Long tuningId, String carInstanceId);

    // ===== High-Level Game Logic =====

    /**
     * Process player's car purchase action and advance turn step.
     * Buys car and transitions to TUNING_SELECTION step.
     */
    void processBuyCarAction(Long roomId, String telegramId, Long carId);

    /**
     * Process player's tuning purchase action and advance to next player.
     * Buys tuning, applies to car, and moves to next player's turn.
     */
    void processBuyTuningAction(Long roomId, String telegramId, Long tuningId, String carInstanceId);

    /**
     * Process player's skip action and advance to next player.
     * Skips current action and moves to next player's turn.
     */
    void processSkipAction(Long roomId, String telegramId);
}
