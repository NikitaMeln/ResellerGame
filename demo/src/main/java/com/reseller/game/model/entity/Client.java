package com.reseller.game.model.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Client's name

    private BigDecimal budget; // Client's budget for purchasing cars

    private String yearForPurchase; // Year when the client plans to purchase a car e.g., 1990-x or 2000-x or 2010-x

    private Integer randomCounter; // Counter for winner from 3 to 6

    private Boolean stockOrNot; // Indicates if the client is interested in stock cars or with tuning

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return Objects.equals(name, client.name)
                && Objects.equals(budget, client.budget)
                && Objects.equals(yearForPurchase, client.yearForPurchase)
                && Objects.equals(randomCounter, client.randomCounter)
                && Objects.equals(stockOrNot, client.stockOrNot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, budget, yearForPurchase, randomCounter, stockOrNot);
    }
}
