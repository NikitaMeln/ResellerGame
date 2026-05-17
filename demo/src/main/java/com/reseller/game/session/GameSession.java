package com.reseller.game.session;

import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.model.entity.types.RoomPhase;
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

    // Full pool of cards (templates from DB). Cards leave the pool only when purchased/sold permanently.
    // The "shop window" is the visible* lists below; pool* lists hold the rest.
    @Builder.Default
    private List<Car> availableCars = new ArrayList<>();      // pool: cars not yet drawn into the window
    @Builder.Default
    private List<Client> availableClients = new ArrayList<>();// pool: clients not yet drawn into the window
    @Builder.Default
    private List<Tuning> availableTunings = new ArrayList<>();// pool: tunings not yet drawn into the window

    // What is currently shown on the board (size = playerCount + 1, set at session creation).
    // Players can only interact with cards from these lists.
    @Builder.Default
    private List<Car> visibleCars = new ArrayList<>();
    @Builder.Default
    private List<Client> visibleClients = new ArrayList<>();
    @Builder.Default
    private List<Tuning> visibleTunings = new ArrayList<>();

    // Available negative cards (debuffs) - drawn directly into car instances, no "window".
    @Builder.Default
    private List<Tuning> availableNegativeCards = new ArrayList<>();

    // Current turn state
    private int currentPlayerIndex;
    private TurnStep turnStep;

    // Round phase: BUYING (forward order) or SELLING (reverse order)
    @Builder.Default
    private RoomPhase phase = RoomPhase.BUYING;

    // Winner of the game (set when win condition is met). Null while in progress.
    private String winnerTelegramId;

    // Per-turn transient state during the selling phase. Null in BUYING.
    private CurrentSale currentSale;

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

    /** Find a car template by id within the currently visible window. */
    public Car findCarTemplate(Long carId) {
        return visibleCars.stream()
                .filter(c -> c.getId().equals(carId))
                .findFirst()
                .orElse(null);
    }

    /** Find a tuning by id within the currently visible window. */
    public Tuning findTuning(Long tuningId) {
        return visibleTunings.stream()
                .filter(t -> t.getId().equals(tuningId))
                .findFirst()
                .orElse(null);
    }

    /** Find a client by id within the currently visible window. */
    public Client findClient(Long clientId) {
        return visibleClients.stream()
                .filter(c -> c.getId().equals(clientId))
                .findFirst()
                .orElse(null);
    }

    /** Remove a car from the visible window when a player buys it. */
    public boolean removeCarFromPool(Long carId) {
        return visibleCars.removeIf(c -> c.getId().equals(carId));
    }

    /** Remove a tuning from the visible window when a player buys it. */
    public boolean removeTuningFromPool(Long tuningId) {
        return visibleTunings.removeIf(t -> t.getId().equals(tuningId));
    }

    /** Remove a client from the visible window after a sale attempt (success or failure). */
    public boolean removeClientFromPool(Long clientId) {
        return visibleClients.removeIf(c -> c.getId().equals(clientId));
    }

    /**
     * Advance turn to the next player according to current phase order.
     * - BUYING goes forward (0 -> N-1). After N-1: switch to SELLING, index = N-1, step = CHOICE_CLIENT_TO_SELL.
     * - SELLING goes backward (N-1 -> 0). After 0: switch to BUYING, index = 0, step = CAR_SELECTION (new round).
     */
    public void moveToNextPlayer() {
        int last = players.size() - 1;
        if (phase == RoomPhase.BUYING) {
            if (currentPlayerIndex >= last) {
                phase = RoomPhase.SELLING;
                currentPlayerIndex = last;
                turnStep = TurnStep.CHOICE_CLIENT_TO_SELL;
            } else {
                currentPlayerIndex++;
                turnStep = TurnStep.CAR_SELECTION;
            }
        } else { // SELLING
            if (currentPlayerIndex <= 0) {
                phase = RoomPhase.BUYING;
                currentPlayerIndex = 0;
                turnStep = TurnStep.CAR_SELECTION;
                rotateVisible();
            } else {
                currentPlayerIndex--;
                turnStep = TurnStep.CHOICE_CLIENT_TO_SELL;
            }
        }
    }

    /**
     * Refresh the shop window between rounds: leftover visible cards go to the end of the pool,
     * fresh cards from the top of the pool replace them. Window size stays the same
     * (playerCount + 1, captured at session creation via initialiseVisible).
     */
    public void rotateVisible() {
        int windowSize = players.size() + 1;
        visibleCars = rotate(visibleCars, availableCars, windowSize);
        visibleClients = rotate(visibleClients, availableClients, windowSize);
        visibleTunings = rotate(visibleTunings, availableTunings, windowSize);
    }

    /**
     * Draw a fresh visible window: leftovers (passed-over cards) go to the tail of the pool,
     * then the head of the pool refills the window up to windowSize.
     */
    private <T> List<T> rotate(List<T> visible, List<T> pool, int windowSize) {
        pool.addAll(visible);
        visible.clear();
        int take = Math.min(windowSize, pool.size());
        for (int i = 0; i < take; i++) {
            visible.add(pool.remove(0));
        }
        return visible;
    }

    /**
     * Called by the session factory after pools are loaded.
     * Moves the first windowSize cards from each pool into the visible window.
     */
    public void initialiseVisible(int windowSize) {
        visibleCars = drawInitial(availableCars, windowSize);
        visibleClients = drawInitial(availableClients, windowSize);
        visibleTunings = drawInitial(availableTunings, windowSize);
    }

    private <T> List<T> drawInitial(List<T> pool, int windowSize) {
        int take = Math.min(windowSize, pool.size());
        List<T> drawn = new ArrayList<>(pool.subList(0, take));
        pool.subList(0, take).clear();
        return drawn;
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
