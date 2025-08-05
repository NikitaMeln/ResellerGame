package com.reseller.game.repository;

import com.reseller.game.model.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    @Query("SELECT p FROM Player p WHERE p.telegramId = :telegramId")
    Optional<Player> findByTelegramId(@Param("telegramId") String telegramId);
}
