package com.example.carsharing.service;

import com.example.carsharing.entity.Car;
import com.example.carsharing.enums.CarStatus;
import com.example.carsharing.repository.BookingRepository;
import com.example.carsharing.repository.CarRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    private final CarRepository carRepository;
    private final FileUploadService fileUploadService;
    private final BookingRepository bookingRepository;

    // Исправлен конструктор: передаем только один экземпляр fileUploadService
    public CarService(CarRepository carRepository, FileUploadService fileUploadService,BookingRepository bookingRepository ) {
        this.carRepository = carRepository;
        this.fileUploadService = fileUploadService;
        this.bookingRepository = bookingRepository;
    }

    // Метод для сохранения автомобиля с изображением
    public Car saveCarWithImage(Car car, MultipartFile image) {
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

}
