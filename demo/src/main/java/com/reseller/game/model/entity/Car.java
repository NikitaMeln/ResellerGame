package com.reseller.game.model.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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

    @ManyToMany
    private List<Tuning> tuning;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Car car = (Car) o;
        return Objects.equals(model, car.model)
                && Objects.equals(year, car.year)
                && Objects.equals(price, car.price)
                && Objects.equals(tuning, car.tuning);
    }

    @Override
    public int hashCode() {
        return Objects.hash(model, year, price, tuning);
    }
}
