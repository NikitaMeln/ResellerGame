package com.reseller.game.dto.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuyTuningRequest {
    private Long roomId;
    private String telegramId;
    private Long tuningId;
}
