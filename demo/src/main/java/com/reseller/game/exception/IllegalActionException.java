package com.reseller.game.exception;

/**
 * Thrown when a player triggers an action that violates the current turn/phase rules:
 * acting out of turn, wrong turn step, mismatched dice value, etc.
 */
public class IllegalActionException extends GameException {
    public IllegalActionException(String message) {
        super(message);
    }
}
