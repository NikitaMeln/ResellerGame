package com.reseller.game.model.entity;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder.Default;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
public class Player {

    @Id
    private String telegramId;

    @NonNull
    private String username;

    @NonNull
    private String language;
    
    private Integer garageSize;

    private BigDecimal balance;

    @ManyToMany
    private List<Car> cars;

    private Integer totalProfit;

    private Integer soldCars;
}
