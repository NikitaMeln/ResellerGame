package com.reseller.game.dto;

import com.reseller.game.model.entity.types.TurnStep;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Data
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
