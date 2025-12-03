package com.reseller.game.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.reseller.game.dto.PlayerDto;
import com.reseller.game.model.entity.Player;

/**
 * Maps Player entity (DB, career stats only) to PlayerDto.
 * In-game state (balance, cars, etc.) is mapped via GameSessionMapper.
 */
@Mapper(componentModel = "spring")
public interface PlayerMapper {
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "garageSize", ignore = true)
    @Mapping(target = "cars", ignore = true)
    @Mapping(target = "currentNegativeCard", ignore = true)
    PlayerDto toDto(Player player);
}
