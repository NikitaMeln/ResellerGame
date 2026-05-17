package com.reseller.game.dto.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChooseClientAndCarRequest {
    private Long roomId;
    private String telegramId;
    private Long clientId;
    private String carInstanceId;
}
