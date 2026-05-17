package com.reseller.game.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSaleDto {
    private String sellerTelegramId;
    private Long clientId;
    private String carInstanceId;
    private Integer diceValue;
    private Integer threshold;
    private Boolean success;
    private Integer profit;
}
