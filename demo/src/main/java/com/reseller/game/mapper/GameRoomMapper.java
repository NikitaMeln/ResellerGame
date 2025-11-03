package com.reseller.game.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.reseller.game.dto.RoomStateDto;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.types.RoomState;

@Mapper(componentModel = "spring", uses = {PlayerMapper.class, CarMapper.class, TuningMapper.class, ClientMapper.class})
public interface  GameRoomMapper {
    // Entity -> DTO
    @Mapping(target = "roomState", source = "state", qualifiedByName = "stateToString")
    @Mapping(target = "playerQueue", source = "playerQueue")
    @Mapping(target = "cars", source = "cars")
    @Mapping(target = "tunings", source = "tunings")
    @Mapping(target = "clients", source = "clients")
    @Mapping(target = "negativeCards", source = "negativeCards")
    RoomStateDto toDto(GameRoom room);

    // DTO -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "players", ignore = true)
    @Mapping(target = "state", source = "roomState", qualifiedByName = "stringToState")
    @Mapping(target = "playerQueue", ignore = true)
    @Mapping(target = "clients", source = "clients")
    @Mapping(target = "cars", source = "cars")
    @Mapping(target = "tunings", source = "tunings")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "currentPlayerIndex", source = "currentPlayerIndex")
    @Mapping(target = "turnStep", source = "turnStep")
    @Mapping(target = "negativeCards", source = "negativeCards")
    GameRoom toEntity(RoomStateDto dto);


    @Named("stateToString")
    default String stateToString(RoomState state) {
        return state != null ? state.name() : null;
    }

    @Named("stringToState")
    default RoomState stringToState(String state) {
        return state != null ? RoomState.valueOf(state) : null;
    }

}
