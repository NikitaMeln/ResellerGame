package com.reseller.game.dto.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkipActionRequest {
    private Long roomId;
    private String telegramId;
}
