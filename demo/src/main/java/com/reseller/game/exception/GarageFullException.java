package com.reseller.game.exception;

public class GarageFullException extends RuntimeException {
    public GarageFullException(String message) {
        super(message);
    }

    public GarageFullException(int maxSize) {
        super(String.format("Garage is full (maximum capacity: %d)", maxSize));
    }
}
