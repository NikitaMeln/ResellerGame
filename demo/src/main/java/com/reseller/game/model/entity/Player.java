package com.reseller.game.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Player {

    @Id
    @EqualsAndHashCode.Include
    private String telegramId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String language;

    // Career statistics (updated after each game)
    private Integer totalProfit;
    private Integer soldCars;

    // NOTE: In-game state (balance, cars, garage) is stored in-memory in PlayerGameState
}
