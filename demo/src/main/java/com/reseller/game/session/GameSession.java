package com.reseller.game.session;

import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.TurnStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * In-memory game session state.
 * This class holds ALL temporary game state that shouldn't be persisted to database.
 * Only created when game starts, destroyed when game finishes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSession {

    // Room identifier
    private Long roomId;

    // Players in this game session with their in-game state
    @Builder.Default
    private List<PlayerGameState> players = new ArrayList<>();

    // Available cars for this game (templates from DB)
    @Builder.Default
    private List<Car> availableCars = new ArrayList<>();

    // Available clients for this game (templates from DB)
    @Builder.Default
    private List<Client> availableClients = new ArrayList<>();

    // Available positive tunings for this game
    @Builder.Default
    private List<Tuning> availableTunings = new ArrayList<>();

    // Available negative cards (debuffs)
    @Builder.Default
    private List<Tuning> availableNegativeCards = new ArrayList<>();

    // Current turn state
    private int currentPlayerIndex;
    private TurnStep turnStep;

    /**
     * Get current player
     */
    public PlayerGameState getCurrentPlayer() {
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex);
        }
        return null;
    }

    /**
     * Find player by telegram ID
     */
    public PlayerGameState findPlayer(String telegramId) {
        return players.stream()
                .filter(p -> p.getTelegramId().equals(telegramId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find car template by ID
     */
    public Car findCarTemplate(Long carId) {
        return availableCars.stream()
                .filter(c -> c.getId().equals(carId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find tuning by ID
     */
    public Tuning findTuning(Long tuningId) {
        return availableTunings.stream()
                .filter(t -> t.getId().equals(tuningId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove car from available pool (when purchased)
     */
    public boolean removeCarFromPool(Long carId) {
        return availableCars.removeIf(c -> c.getId().equals(carId));
    }

    /**
     * Remove tuning from available pool (when purchased)
     */
    public boolean removeTuningFromPool(Long tuningId) {
        return availableTunings.removeIf(t -> t.getId().equals(tuningId));
    }

    /**
     * Move to next player and reset turn step to the start of the buying phase.
     */
    public void moveToNextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        turnStep = TurnStep.CAR_SELECTION;
    }

    /**
     * Get player queue as list of telegram IDs
     */
    public List<String> getPlayerQueue() {
        return players.stream()
                .map(PlayerGameState::getTelegramId)
                .collect(Collectors.toList());
    }

    /**
     * Check if player is current player
     */
    public boolean isCurrentPlayer(String telegramId) {
        PlayerGameState current = getCurrentPlayer();
        return current != null && current.getTelegramId().equals(telegramId);
    }
}
