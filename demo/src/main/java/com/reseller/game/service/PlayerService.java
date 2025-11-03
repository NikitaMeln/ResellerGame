package com.reseller.game.service;

import com.reseller.game.model.entity.Player;

public interface PlayerService {
    Player findByTelegramId(String telegramId);
    Player findByTelegramIdWithCars(String telegramId);
    Player createOrGetPlayer(String telegramId, String username, String language);
}
