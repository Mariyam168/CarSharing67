package com.example.carsharing.service;

import com.example.carsharing.entity.Car;
import com.example.carsharing.enums.CarStatus;
import com.example.carsharing.repository.CarRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    private final CarRepository carRepository;
    private final FileUploadService fileUploadService;

    // Исправлен конструктор: передаем только один экземпляр fileUploadService
    public CarService(CarRepository carRepository, FileUploadService fileUploadService) {
        this.carRepository = carRepository;
        this.fileUploadService = fileUploadService;
    }

    // Метод для сохранения автомобиля с изображением
    public Car saveCarWithImage(Car car, MultipartFile image) throws IOException {
        // Сохраняем изображение и получаем путь
        String imagePath = fileUploadService.storeFile(image);

        // Сохраняем путь к изображению в объекте car
        car.setCarImage(imagePath);

        // Сохраняем объект Car в базе данных
        return carRepository.save(car);
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

    public String getPhotoByCarId(Long id) {
        // Получаем автомобиль по ID
        Optional<Car> car = carRepository.findById(id);

        // Если автомобиль найден, возвращаем путь к его изображению
        return car.map(Car::getCarImage)
                .orElseThrow(() -> new RuntimeException("Car not found with ID: " + id));
    }
}
