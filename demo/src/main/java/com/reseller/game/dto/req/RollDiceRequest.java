package com.reseller.game.dto.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RollDiceRequest {
    private Long roomId;
    private String telegramId;
    private Integer diceValue;
}
