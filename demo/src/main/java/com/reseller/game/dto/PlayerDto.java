package com.reseller.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private List<CarDto> cars;
}
