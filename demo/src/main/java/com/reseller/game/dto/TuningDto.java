package com.reseller.game.dto;

import com.reseller.game.model.entity.types.TuningType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuningDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private TuningType type;
    private Map<String, Object> properties;
}
