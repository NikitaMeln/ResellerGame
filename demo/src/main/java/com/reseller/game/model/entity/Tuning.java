package com.reseller.game.model.entity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import com.reseller.game.model.entity.types.TuningType;
import com.reseller.game.util.JsonMapConverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Tuning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Name of the tuning
    
    private BigDecimal price; // Price of the tuning 100-300
    
    private TuningType type; // Negative or Positive tuning
    
    /*
    * Tuning can affect the car price by ±100–300.
    * It can modify the random purchase counter by ±2 or ±3.
    * It can temporarily ban a car from one round.
    * It can deactivate all tunings on a specific car.
    * It can influence the client, allowing you to bypass certain selling requirements.
    */
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> properties;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Tuning tuning = (Tuning) o;
        return Objects.equals(name, tuning.name)
                && Objects.equals(price, tuning.price)
                && type == tuning.type
                && Objects.equals(properties, tuning.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price, type, properties);
    }
}
