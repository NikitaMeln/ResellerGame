package com.reseller.game.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transient per-turn state for the selling phase.
 * Lives from CHOICE_CLIENT_TO_SELL through RESULT, then cleared on next turn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentSale {
    private String sellerTelegramId;
    private Long clientId;
    private String carInstanceId;
    private Integer diceValue;       // null until rolled
    private Integer threshold;       // computed at roll time
    private Boolean success;         // null until rolled
    private Integer profit;          // 0 on failure, positive on success
}
