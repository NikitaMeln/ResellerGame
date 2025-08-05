package com.reseller.game.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;

import com.reseller.game.model.entity.types.Phase;
import com.reseller.game.model.entity.types.RoomState;
import jakarta.persistence.*;
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
    private List<Client> clients;
    
    @ManyToMany
    private List<Car> cars;
    
    @ManyToMany
    private List<Tuning> tunings;

    private LocalDateTime startTime;

    private RoomState state;

    private Phase phase;
}
