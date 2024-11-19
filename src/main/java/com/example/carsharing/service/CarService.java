package com.example.carsharing.service;

import com.example.carsharing.entity.Car;
import com.example.carsharing.entity.User;
import com.example.carsharing.enums.CarStatus;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public Car save(Car car) {
        return carRepository.save(car);
    }

    public List<Car> getAllCars() {
        return carRepository.findAll();
    }

    public Optional<Car> getCarById(Long id) {
        return carRepository.findById(id);
    }

    public void deleteCarById(Long id) {
        carRepository.deleteById(id);
    }
    public List<Car> getCarsByModel(String model) {
        return carRepository.findByModel(model);
    }
    public List<Car> getCarsByStatus(CarStatus status) {
        return carRepository.findByCarStatus(status);
    }
    public Optional<Car> getCarByLicensePlate(String licensePlate) {
        return carRepository.findByLicensePlate(licensePlate);
    }
    public List<Car> findAvailableCarsByTipAndDates(String tip, LocalDateTime startTime, LocalDateTime endTime) {
        return carRepository.findAvailableCarsByTipAndDates(tip, startTime, endTime);

    }


}
