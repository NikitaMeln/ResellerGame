package com.reseller.game.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.reseller.game.model.entity.types.TurnStep;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomStateDto {
    private String roomState;
    private List<ClientDto> clients;
    private List<PlayerDto> playerQueue;
    private List<CarDto> cars;
    private List<TuningDto> tunings;
    private LocalDateTime startTime;
    private Integer currentPlayerIndex;
    private TurnStep turnStep;
    private List<TuningDto> negativeCards;

}
