package com.reseller.game.repository;

import com.reseller.game.model.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    @Query("select p from Player p where p.telegram_id = :telegramId")
    Optional<Player> findById(@Param("telegramId") String telegramId);
}
