package com.reseller.game.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.reseller.game.model.entity.GameRoom;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    
}
