package com.reseller.game.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import com.reseller.game.model.entity.Car;
import com.reseller.game.model.entity.Tuning;
import com.reseller.game.repository.CarRepository;

@Service
public class CarServiceImpl {

    private final CarRepository carRepository;

     public CarServiceImpl(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public void initCars(List<Car> cars) {
        try {
            carRepository.deleteAllInBatch();
            carRepository.saveAll(cars);
        } catch (Exception e) {
            System.err.println("Error initializing cars: " + e.getMessage());
        }
    }

    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    public List<Car> getRandomCars(List<Car> cars, int count) {
        List<Car> randomCars = new ArrayList<>();
        if (cars == null || cars.isEmpty()) {
            return null;
        }

        for (int i = 0; i < count; i++) {
            int randomIndex = (int) (Math.random() * cars.size());
            if (cars.get(randomIndex) == null) {
                randomCars.add(cars.get(randomIndex));
            }
        }
        return randomCars;
    }

    public Car setTuning(Car car, Tuning tuning) {
        if (car != null) {
        List<Tuning> existedTuning = car.getTuning();
            existedTuning.add(tuning);
            car.setTuning(existedTuning);
        }
        return car;
    }
}
