package com.reseller.game.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.reseller.game.model.entity.Tuning;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory representation of a car instance in a game session.
 * Each purchased car gets its own CarInstance with unique ID.
 * Tunings are applied to this specific instance, not to the template Car entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarInstance {

    // Unique ID for this car instance in the game
    private String instanceId;

    // Reference to the original Car template in database
    private Long originalCarId;

    // Car properties (copied from template)
    private String model;
    private String year;
    private BigDecimal basePrice;

    // Tunings applied to THIS specific car instance
    @Builder.Default
    private List<Tuning> appliedTunings = new ArrayList<>();

    /**
     * Create a new car instance from a template Car
     */
    public static CarInstance fromTemplate(Long carId, String model, String year, BigDecimal price) {
        return CarInstance.builder()
                .instanceId(UUID.randomUUID().toString())
                .originalCarId(carId)
                .model(model)
                .year(year)
                .basePrice(price)
                .appliedTunings(new ArrayList<>())
                .build();
    }

    /**
     * Calculate total price including tunings
     */
    public BigDecimal getTotalPrice() {
        BigDecimal total = basePrice;
        for (Tuning tuning : appliedTunings) {
            total = total.add(tuning.getPrice());
        }
        return total;
    }

    /**
     * Add a tuning to this car instance
     */
    public void addTuning(Tuning tuning) {
        if (tuning != null && !appliedTunings.contains(tuning)) {
            appliedTunings.add(tuning);
        }
    }
}
