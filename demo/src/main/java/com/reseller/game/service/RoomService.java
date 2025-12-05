package com.reseller.game.service;

import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;

/**
 * Service interface for managing persistent game room data in database.
 *
 * Responsible for:
 * - Room lifecycle (create, retrieve)
 * - Player management (add to room, player queue)
 * - Room state transitions (PENDING -> STARTED -> FINISHED)
 * - Database persistence of room metadata
 *
 * NOT responsible for:
 * - In-memory game session management (see GameSessionService)
 * - Game logic and turn progression (see GameSessionService)
 */
public interface RoomService {

    /**
     * Creates a new game room in PENDING state.
     */
    GameRoom createRoom();

    /**
     * Retrieves a room by its ID.
     */
    GameRoom getRoomById(Long id);

    /**
     * Finds the oldest room in the specified state, or creates a new one if none exists.
     */
    GameRoom getRoomByState(RoomState state);

    /**
     * Adds a player to the room and player queue.
     * Triggers auto-start logic if conditions are met.
     */
    void addPlayer(GameRoom room, Player player);

    /**
     * Starts the game: creates in-memory session and updates room state to STARTED.
     */
    void startGame(GameRoom room);
}
