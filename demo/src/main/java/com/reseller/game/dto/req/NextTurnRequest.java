package com.reseller.game.dto.req;

import lombok.Getter;
import lombok.Setter;

/**
 * Sent by the player to acknowledge the RESULT screen and pass the turn
 * to the next player in the current phase order.
 */
@Getter
@Setter
public class NextTurnRequest {
    private Long roomId;
    private String telegramId;
}
