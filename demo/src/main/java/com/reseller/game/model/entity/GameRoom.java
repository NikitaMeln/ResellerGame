package com.reseller.game.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Queue;
import com.reseller.game.model.entity.types.RoomState;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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
    
    private Queue<Player> playerQueue;

    @ManyToMany
    private List<Client> clients;
    
    @ManyToMany
    private List<Car> cars;
    
    @ManyToMany
    private List<Tuning> tunings;

    private LocalDateTime startTime;

    private RoomState state;
}
