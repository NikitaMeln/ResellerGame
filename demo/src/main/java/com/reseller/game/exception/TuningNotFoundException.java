package com.reseller.game.exception;

public class TuningNotFoundException extends GameException {
    public TuningNotFoundException(String message) {
        super(message);
    }

    public TuningNotFoundException(Long tuningId) {
        super(String.format("Tuning with ID %d not found", tuningId));
    }
}
