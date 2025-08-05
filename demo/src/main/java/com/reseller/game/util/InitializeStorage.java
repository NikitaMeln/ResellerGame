package com.reseller.game.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.service.impl.CarServiceImpl;
import com.reseller.game.service.impl.ClientServiceImpl;
import com.reseller.game.service.impl.TuningServiceImpl;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@AllArgsConstructor
public class InitializeStorage {

    private final TuningServiceImpl tuningServiceImpl;
    private final CarServiceImpl carServiceImpl;
    private final ClientServiceImpl clientServiceImpl;
    private final JsonToDataParser jsonToDataParser;
    
    @PostConstruct
    private void init() {
        try (InputStream tuningStream = getClass().getResourceAsStream("/data/tuning-data-en.json");
         InputStream carStream = getClass().getResourceAsStream("/data/cars.json");
         InputStream clientStream = getClass().getResourceAsStream("/data/client.json")) {

            if (tuningStream != null) {
                List<Tuning> tunings = jsonToDataParser.loader(tuningStream, Tuning.class);
                tuningServiceImpl.initTuning(tunings);
            }

            if (carStream != null) {
                List<Car> cars = jsonToDataParser.loader(carStream, Car.class);
                carServiceImpl.initCars(cars);
            }

            if (clientStream != null) {
                List<Client> clients = jsonToDataParser.loader(clientStream, Client.class);
                clientServiceImpl.initClients(clients);
            }
        } catch (IOException e) {
            System.err.println("Error initializing storage: " + e.getMessage());
        }
    }

}
