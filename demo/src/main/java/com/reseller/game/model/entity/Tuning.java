package com.reseller.game.model.entity;

import java.math.BigDecimal;
import java.util.Map;

import org.h2.util.json.JSONObject;
import com.reseller.game.model.entity.types.TuningType;
import com.reseller.game.util.JsonMapConverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Tuning {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    
    private BigDecimal price;
    
    private TuningType type;
    
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> properties;
}
