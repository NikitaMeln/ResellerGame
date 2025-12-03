package com.reseller.game.mapper;

import com.reseller.game.dto.ClientDto;
import com.reseller.game.model.entity.Client;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClientMapper {
    ClientDto toDto(Client client);
}
