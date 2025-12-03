package com.reseller.game.dto.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuyCarRequest {
    private Long roomId;
    private String telegramId;
    private Long carId;
}
