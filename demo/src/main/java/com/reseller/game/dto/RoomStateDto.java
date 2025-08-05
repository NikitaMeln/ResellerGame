package com.reseller.game.dto;

import com.reseller.game.model.entity.*;
import com.reseller.game.model.entity.types.Phase;
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

    private List<Client> clients;

    private List<Player> playerQueue;

    private List<Car> cars;

    private List<Tuning> tunings;

    private LocalDateTime startTime;

    private Phase phase;

}
