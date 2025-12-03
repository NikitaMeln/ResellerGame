package com.reseller.game.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(int required, int available) {
        super(String.format("Insufficient balance: required %d, but only %d available", required, available));
    }
}
