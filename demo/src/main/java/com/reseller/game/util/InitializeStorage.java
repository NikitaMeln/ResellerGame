package com.reseller.game.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Component;

import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Client;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.service.impl.CarServiceImpl;
import com.reseller.game.service.impl.ClientServiceImpl;
import com.reseller.game.service.impl.TuningServiceImpl;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        try {
            // Check if data already exists - load only if tables are empty
            // Call each service method once and store the result
            List<Car> existingCars = carServiceImpl.getAllCars();
            List<Tuning> existingTunings = tuningServiceImpl.getAllTunings();
            List<Client> existingClients = clientServiceImpl.getAllClients();

            boolean carsExist = existingCars != null && !existingCars.isEmpty();
            boolean tuningsExist = existingTunings != null && !existingTunings.isEmpty();
            boolean clientsExist = existingClients != null && !existingClients.isEmpty();

            if (carsExist && tuningsExist && clientsExist) {
                log.info("Data already exists in database, skipping initialization");
                return;
            }

            log.info("Initializing database with data from JSON files...");

            try (InputStream tuningStream = getClass().getResourceAsStream("/data/tuning-data-en.json");
             InputStream carStream = getClass().getResourceAsStream("/data/cars.json");
             InputStream clientStream = getClass().getResourceAsStream("/data/client.json")) {

                if (tuningStream != null && !tuningsExist) {
                    List<Tuning> tunings = jsonToDataParser.loader(tuningStream, Tuning.class);
                    tuningServiceImpl.initTuning(tunings);
                    log.info("Loaded {} tunings", tunings.size());
                }

                if (carStream != null && !carsExist) {
                    List<Car> cars = jsonToDataParser.loader(carStream, Car.class);
                    carServiceImpl.initCars(cars);
                    log.info("Loaded {} cars", cars.size());
                }

                if (clientStream != null && !clientsExist) {
                    List<Client> clients = jsonToDataParser.loader(clientStream, Client.class);
                    clientServiceImpl.initClients(clients);
                    log.info("Loaded {} clients", clients.size());
                }
            }

            log.info("Database initialization complete");
        } catch (IOException e) {
            log.error("Error initializing storage: {}", e.getMessage(), e);
        }
    }

}
