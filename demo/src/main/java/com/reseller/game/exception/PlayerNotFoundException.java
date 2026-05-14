package com.reseller.game.exception;

public class PlayerNotFoundException extends GameException {
    public PlayerNotFoundException(String telegramId) {
        super(String.format("Player with telegram ID '%s' not found in session", telegramId));
    }
}
