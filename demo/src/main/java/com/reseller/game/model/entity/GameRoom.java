package com.reseller.game.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import com.reseller.game.model.entity.types.RoomState;
import com.reseller.game.model.entity.types.TurnStep;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
public class GameRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToMany
    private List<Player> players;

    @ManyToMany
    @JoinTable(
            name = "room_player_queue",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "player_id")
    )
    @OrderColumn(name = "queue_order")
    private List<Player> playerQueue;

    @ManyToMany
    @JoinTable(
            name = "game_room_clients",
            joinColumns = @JoinColumn(name = "game_room_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    private List<Client> clients;

    @ManyToMany
    @JoinTable(
            name = "game_room_cars",
            joinColumns = @JoinColumn(name = "game_room_id"),
            inverseJoinColumns = @JoinColumn(name = "car_id")
    )
    private List<Car> cars;

    @ManyToMany
    @JoinTable(
            name = "game_room_tunings",
            joinColumns = @JoinColumn(name = "game_room_id"),
            inverseJoinColumns = @JoinColumn(name = "tuning_id")
    )
    private List<Tuning> tunings;

    private LocalDateTime startTime;

    private RoomState state;

    private Integer currentPlayerIndex;

    private TurnStep turnStep;

    @ManyToMany
    private List<Tuning> negativeCards;
}
