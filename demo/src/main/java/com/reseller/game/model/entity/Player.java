package com.reseller.game.model.entity;

import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "cars")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Player {

    @Id
    private String telegramId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String language;
    
    private Integer garageSize;

    private Integer balance;

    @ManyToMany
    @JoinTable(
            name = "player_cars",
            joinColumns = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "car_id")
    )
    private List<Car> cars;

    private Integer totalProfit;

    private Integer soldCars;
}
