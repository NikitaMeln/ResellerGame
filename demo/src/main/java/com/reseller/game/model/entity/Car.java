package com.reseller.game.model.entity;

import java.math.BigDecimal;
import java.util.Objects;

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
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String model;

    private String year;

    private BigDecimal price;

    // NOTE: This is a template/reference car from database (read-only)
    // Tunings are applied to CarInstance in-memory, not to this entity

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return Objects.equals(model, car.model)
                && Objects.equals(year, car.year)
                && Objects.equals(price, car.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, year, price);
    }
}
