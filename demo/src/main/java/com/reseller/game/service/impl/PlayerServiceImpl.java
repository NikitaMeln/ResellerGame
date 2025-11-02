package com.reseller.game.service.impl;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.reseller.game.model.entity.Player;
import com.reseller.game.repository.PlayerRepository;
import com.reseller.game.service.PlayerService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;

    @Override
    public Player findByTelegramId(String telegramId) {
        return playerRepository.findByTelegramId(telegramId).orElseThrow();
    }

    @Override
    @Transactional
    public Player createOrGetPlayer(String telegramId, String username, String language) {
        return playerRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    Player newPlayer = new Player();
                    newPlayer.setTelegramId(telegramId);
                    newPlayer.setUsername(username);
                    newPlayer.setLanguage(language);
                    newPlayer.setBalance(2300); // Starting balance
                    newPlayer.setGarageSize(3);
                    newPlayer.setTotalProfit(0);
                    newPlayer.setSoldCars(0);
                    newPlayer.setCars(new ArrayList<>());
                    return playerRepository.save(newPlayer);
                });
    }
}
