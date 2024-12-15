package com.example.carsharing.service;

import com.example.carsharing.entity.Car;
import com.example.carsharing.enums.CarStatus;
import com.example.carsharing.repository.CarRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    public Car updateCar(Long id, String make, String model, String tip, String year, String rule,
                         String licensePlate, CarStatus carStatus, Integer volume, Double probeg,
                         String color, BigDecimal price, MultipartFile image) throws IOException {
        Optional<Car> carOptional = carRepository.findById(id);

        if (carOptional.isPresent()) {
            Car car = carOptional.get();

            // Обновляем только те поля, которые не равны null
            if (make != null) {
                car.setMake(make);
            }
            if (model != null) {
                car.setModel(model);
            }
            if (tip != null) {
                car.setTip(tip);
            }
            if (year != null) {
                car.setYear(year);
            }
            if (rule != null) {
                car.setRule(rule);
            }
            if (licensePlate != null) {
                car.setLicensePlate(licensePlate);
            }
            if (carStatus != null) {
                car.setCarStatus(carStatus);
            }
            if (volume != null) {
                car.setVolume(volume);
            }
            if (probeg != null) {
                car.setProbeg(probeg);
            }
            if (color != null) {
                car.setColor(color);
            }
            if (price != null) {
                car.setPrice(price);
            }
            // Если передан новый файл изображения, обновляем его
            if (image != null && !image.isEmpty()) {
                String imagePath = fileUploadService.storeFile(image);
                car.setCarImage(imagePath);
            }

            // Сохраняем обновленный объект
            return carRepository.save(car);
        } else {
            throw new RuntimeException("Car not found with ID: " + id);
        }
    }
    public List<Car> findAvailableCars(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Дата начала и окончания не могут быть пустыми.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала.");
        }
        return carRepository.findAvailableCarsByDates(startDate, endDate);
    }

}
