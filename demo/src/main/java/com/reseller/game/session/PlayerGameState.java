package com.reseller.game.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory representation of a player's state during a game session.
 * Contains player's balance, garage (cars), and other temporary game state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameState {

    // Player identifier
    private String telegramId;
    private String username;

    // In-game balance (temporary, not saved to DB)
    @Builder.Default
    private int balance = 1000;

    // Player's garage (cars purchased during this game)
    @Builder.Default
    private List<CarInstance> garage = new ArrayList<>();

    // Cars successfully sold during this game session (win condition: >= 5)
    @Builder.Default
    private int soldCars = 0;

    // Total profit accumulated from successful sales (win condition: >= 10000)
    @Builder.Default
    private int totalProfit = 0;

    // Garage capacity
    @Builder.Default
    private int garageCapacity = 3;

    /**
     * Add a car to player's garage
     */
    public boolean addCar(CarInstance car) {
        if (garage.size() < garageCapacity) {
            garage.add(car);
            return true;
        }
        return false;
    }

    /**
     * Remove a car from garage (when sold)
     */
    public boolean removeCar(CarInstance car) {
        return garage.remove(car);
    }

    /**
     * Find car in garage by instance ID
     */
    public CarInstance findCarById(String instanceId) {
        return garage.stream()
                .filter(car -> car.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if player can afford a price
     */
    public boolean canAfford(int price) {
        return balance >= price;
    }

    /**
     * Deduct balance
     */
    public void deductBalance(int amount) {
        balance -= amount;
    }

    /**
     * Add balance
     */
    public void addBalance(int amount) {
        balance += amount;
    }
}
