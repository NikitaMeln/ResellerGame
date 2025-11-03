package com.reseller.game.repository;

import com.reseller.game.model.entity.types.RoomState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.reseller.game.model.entity.GameRoom;

import java.util.Optional;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
    @Query("select r from GameRoom r where r.state = :state order by r.startTime asc limit 1")
    Optional<GameRoom> findOldestByState(@Param("state") RoomState state);

    @EntityGraph(attributePaths = {
            "players",
            "playerQueue",
            "playerQueue.cars",
            "playerQueue.cars.tuning",
            "cars",
            "cars.tuning",
            "tunings",
            "clients",
            "negativeCards"
    })
    @NonNull
    Optional<GameRoom> findById(@NonNull Long id);
}
