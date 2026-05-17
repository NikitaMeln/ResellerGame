package com.reseller.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarDto {
    private Long id;
    private String instanceId;
    private String model;
    private String year;
    private BigDecimal price;
    private List<TuningDto> tuning;
    // Hidden negative card data is only set when revealed; otherwise null.
    private TuningDto hiddenNegativeCard;
    private boolean negativeCardRevealed;
}
