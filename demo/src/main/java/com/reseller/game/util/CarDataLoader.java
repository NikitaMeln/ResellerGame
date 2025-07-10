package com.reseller.game.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reseller.game.model.entity.Car;
import jakarta.annotation.PostConstruct;

@Component
public class CarDataLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadCars() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/data/cars.json");
        if (inputStream == null) throw new IllegalStateException("cars.json not found");

        List<Car> cars = objectMapper.readValue(
            inputStream,
            new TypeReference<List<Car>>() {}
        );

        System.out.printf("Loaded %s cars", cars.size());
    }
}

