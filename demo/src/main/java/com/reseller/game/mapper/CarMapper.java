package com.reseller.game.mapper;

import com.reseller.game.dto.CarDto;
import com.reseller.game.model.entity.Car;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = TuningMapper.class)
public interface CarMapper {
    CarDto toDto(Car car);
}
