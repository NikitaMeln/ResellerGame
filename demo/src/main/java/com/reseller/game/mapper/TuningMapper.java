package com.reseller.game.mapper;

import com.reseller.game.dto.TuningDto;
import com.reseller.game.model.entity.Tuning;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TuningMapper {
    TuningDto toDto(Tuning tuning);
}
