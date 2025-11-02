package com.reseller.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDto {
    private String telegramId;
    private String username;
    private Integer balance;
    private Integer garageSize;
    private Integer totalProfit;
    private Integer soldCars;
}
