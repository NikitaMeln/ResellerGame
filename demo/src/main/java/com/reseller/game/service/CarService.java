package com.reseller.game.service;

import java.util.List;

import com.reseller.game.model.entity.Car;

public interface CarService {
    void initCars(List<Car> cars);

    List<Car> getAllCars();

    List<Car> getRandomCars(List<Car> cars, int count);
}
