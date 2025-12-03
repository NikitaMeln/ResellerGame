package com.reseller.game.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.reseller.game.dto.RoomStateDto;
import com.reseller.game.model.entity.GameRoom;
import com.reseller.game.model.entity.types.RoomState;

/**
 * Maps GameRoom entity (only basic info from DB).
 * For game state (cars, clients, tunings, etc.), use GameSessionMapper instead.
 */
@Mapper(componentModel = "spring", uses = {PlayerMapper.class})
public interface  GameRoomMapper {

    @Mapping(target = "roomState", source = "state", qualifiedByName = "stateToString")
    @Mapping(target = "playerQueue", source = "playerQueue")
    @Mapping(target = "startTime", source = "startTime")
    // Game state fields (cars, clients, tunings, etc.) are NOT mapped here
    // They come from GameSession via GameSessionMapper
    @Mapping(target = "cars", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "tunings", ignore = true)
    @Mapping(target = "negativeCards", ignore = true)
    @Mapping(target = "currentPlayerIndex", ignore = true)
    @Mapping(target = "turnStep", ignore = true)
    RoomStateDto toDto(GameRoom room);

    @Named("stateToString")
    default String stateToString(RoomState state) {
        return state != null ? state.name() : null;
    }
}
