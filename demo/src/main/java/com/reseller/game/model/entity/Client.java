package com.reseller.game.model.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
}
