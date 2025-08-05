package com.reseller.game.service.impl;

import com.reseller.game.model.entity.Player;
import com.reseller.game.repository.PlayerRepository;
import com.reseller.game.service.PlayerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    @Override
    public Player findByTelegramId(String telegramId) {
        return playerRepository.findByTelegramId(telegramId).orElseThrow();
    }
}
