package com.reseller.game.exception;

public class CarNotFoundException extends GameException {
    public CarNotFoundException(String message) {
        super(message);
    }

    public CarNotFoundException(Long carId) {
        super(String.format("Car with ID %d not found", carId));
    }
}
