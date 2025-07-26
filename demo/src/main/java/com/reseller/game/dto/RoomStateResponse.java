package com.reseller.game.dto;

import com.reseller.game.model.entity.*;
import com.reseller.game.model.entity.types.Phase;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;

@Getter
@Setter
public class RoomStateResponse {

    private String roomState;

    private Queue<Player> playerQueue;

    private List<Client> clients;

    private List<Car> cars;

    private List<Tuning> tunings;

    private LocalDateTime startTime;

    private Phase phase;

}
