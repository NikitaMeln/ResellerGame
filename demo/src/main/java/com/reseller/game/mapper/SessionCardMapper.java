package com.reseller.game.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.reseller.game.dto.CarDto;
import com.reseller.game.dto.CurrentSaleDto;
import com.reseller.game.session.CarInstance;
import com.reseller.game.session.CurrentSale;

/**
 * Maps in-memory session objects to DTOs.
 * hiddenNegativeCard is deliberately left unmapped: GameSessionMapper only fills it once the
 * card has been revealed, so mapping it here would leak the secret before SHOW_SECRET_CARD.
 */
@Mapper(componentModel = "spring", uses = {TuningMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface SessionCardMapper {

    CurrentSaleDto toDto(CurrentSale sale);

    @Mapping(target = "id", source = "originalCarId")
    @Mapping(target = "price", source = "basePrice")
    @Mapping(target = "tuning", source = "appliedTunings")
    @Mapping(target = "hiddenNegativeCard", ignore = true)
    CarDto toDto(CarInstance instance);
}
