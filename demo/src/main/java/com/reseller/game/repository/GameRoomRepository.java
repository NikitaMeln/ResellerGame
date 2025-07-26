package com.reseller.game.repository;

import com.reseller.game.model.entity.types.RoomState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.reseller.game.model.entity.GameRoom;

import java.util.Optional;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    @Query("select r from GameRoom r where r.state = :state order by r.startTime asc")
    Optional<GameRoom> findOldestByState(@Param("state") RoomState state);
}
