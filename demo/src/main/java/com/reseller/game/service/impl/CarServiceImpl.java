package com.reseller.game.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.reseller.game.model.entity.Car;
import com.reseller.game.repository.CarRepository;
import com.reseller.game.service.CarService;
import com.reseller.game.util.DataInitializationUtil;
import com.reseller.game.util.RandomSelectionUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService{

    private final CarRepository carRepository;

    @Override
    public void initCars(List<Car> cars) {
        DataInitializationUtil.initializeData(carRepository, cars, "cars", true);
    }

    @Override
    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    @Override
    public List<Car> getRandomCars(List<Car> cars, int count) {
        return RandomSelectionUtil.selectMultipleRandom(cars, count);
    }

}
