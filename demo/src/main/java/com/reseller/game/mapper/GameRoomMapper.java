package com.reseller.game.mapper;

import com.reseller.game.dto.RoomStateResponse;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.Player;
import com.reseller.game.model.entity.types.RoomState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.LinkedList;
import java.util.Queue;

@Mapper(componentModel = "spring")
public interface  GameRoomMapper {
    // Entity -> DTO
    @Mapping(target = "roomState", source = "state", qualifiedByName = "stateToString")
    RoomStateResponse toDto(GameRoom room);

    // DTO -> Entity
    @Mapping(target = "state", source = "roomState", qualifiedByName = "stringToState")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "players", ignore = true) // нет в DTO
    GameRoom toEntity(RoomStateResponse dto);


    @Named("stateToString")
    default String stateToString(RoomState state) {
        return state != null ? state.name() : null;
    }

    @Named("stringToState")
    default RoomState stringToState(String state) {
        return state != null ? RoomState.valueOf(state) : null;
    }

    default Queue<Player> copyQueue(Queue<Player> src) {
        return src == null ? null : new LinkedList<>(src);
    }
}
