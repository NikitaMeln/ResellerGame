package com.reseller.game.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.service.CarService;
import com.reseller.game.service.ClientService;
import com.reseller.game.service.TuningService;

import jakarta.annotation.PostConstruct;

@Component
public class InitializeStorage {

    private final TuningService tuningService;
    private final CarService carService;
    private final ClientService clientService;
    private final JsonToDataParser jsonToDataParser;

    public InitializeStorage(
        TuningService tuningService,
        CarService carService,
        ClientService clientService,
        JsonToDataParser jsonToDataParser) {
        this.tuningService = tuningService;
        this.carService = carService;
        this.clientService = clientService;
        this.jsonToDataParser = jsonToDataParser;
    }
    
    @PostConstruct
    private void init() {
        try (InputStream tuningStream = getClass().getResourceAsStream("/data/tuning-data-en.json");
         InputStream carStream = getClass().getResourceAsStream("/data/cars.json");
         InputStream clientStream = getClass().getResourceAsStream("/data/client.json")) {

        if (tuningStream != null) {
            List<Tuning> tunings = jsonToDataParser.loader(tuningStream, Tuning.class);
            tuningService.initTuning(tunings);
        }

        if (carStream != null) {
            List<Car> cars = jsonToDataParser.loader(carStream, Car.class);
            carService.initCars(cars);
        }

        if (clientStream != null) {
            List<Client> clients = jsonToDataParser.loader(clientStream, Client.class);
            clientService.initClients(clients);
        }
    } catch (IOException e) {
        System.err.println("Error initializing storage: " + e.getMessage());
    }
    }

}
