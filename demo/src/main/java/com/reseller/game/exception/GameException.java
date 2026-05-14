package com.reseller.game.exception;

/**
 * Base class for all expected, recoverable game-rule violations
 * (insufficient balance, garage full, invalid target, etc.).
 * The WebSocket controller maps these to a player-targeted error message
 * instead of letting them bubble up as 500 Unhandled exceptions.
 */
public abstract class GameException extends RuntimeException {
    protected GameException(String message) {
        super(message);
    }
}
