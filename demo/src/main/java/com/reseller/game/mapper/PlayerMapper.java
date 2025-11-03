package com.reseller.game.mapper;

import com.reseller.game.dto.PlayerDto;
import com.reseller.game.model.entity.Player;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CarMapper.class)
public interface PlayerMapper {
    PlayerDto toDto(Player player);
}
