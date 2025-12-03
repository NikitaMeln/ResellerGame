package com.reseller.game.mapper;

import com.reseller.game.dto.CarDto;
import com.reseller.game.model.entity.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps Car entity (DB template) to CarDto.
 * Tunings are mapped via GameSessionMapper when converting CarInstance.
 */
@Mapper(componentModel = "spring")
public interface CarMapper {
    // Map car template from DB
    // Tuning field is ignored - it comes from CarInstance.appliedTunings
    @Mapping(target = "tuning", ignore = true)
    CarDto toDto(Car car);
}
