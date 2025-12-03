package com.reseller.game.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.types.RoomState;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    @Query("select r from GameRoom r where r.state = :state order by r.startTime asc limit 1")
    Optional<GameRoom> findOldestByState(@Param("state") RoomState state);

    @EntityGraph(attributePaths = {
            "players",
            "playerQueue"
    })
    @NonNull
    @Override
    Optional<GameRoom> findById(@NonNull Long id);
}
