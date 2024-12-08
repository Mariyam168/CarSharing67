package com.example.carsharing.controller;

import com.example.carsharing.entity.Car;
import com.example.carsharing.enums.CarStatus;
import com.example.carsharing.service.CarService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/cars")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    // Добавление машины без изображения
    @PostMapping
    public ResponseEntity<Car> addCar(@RequestBody Car car) {
        Car savedCar = carService.save(car);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCar);
    }

    // Добавление машины с изображением
    @PostMapping(value = "/add", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Car> addCarWithImage(@RequestParam("make") String make,
                                               @RequestParam("model") String model,
                                               @RequestParam("tip") String tip,
                                               @RequestParam("year") String year,
                                               @RequestParam("rule") String rule,
                                               @RequestParam("licensePlate") String licensePlate,
                                               @RequestParam("carStatus") CarStatus carStatus,
                                               @RequestParam("volume") int volume,
                                               @RequestParam("probeg") double probeg,
                                               @RequestParam("color") String color,
                                               @RequestParam("price") BigDecimal price,
                                               @RequestParam("image") MultipartFile image) throws IOException {
        Car car = new Car();
        car.setMake(make);
        car.setModel(model);
        car.setTip(tip);
        car.setYear(year);
        car.setRule(rule);
        car.setLicensePlate(licensePlate);
        car.setCarStatus(carStatus);
        car.setVolume(volume);
        car.setProbeg(probeg);
        car.setColor(color);
        car.setPrice(price);

        // Сохранение автомобиля с изображением
        Car savedCar = carService.saveCarWithImage(car, image);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedCar);
    }

    // Получение всех машин
    @GetMapping
    public ResponseEntity<List<Car>> getAllCars() {
        List<Car> cars = carService.getAllCars();
        return ResponseEntity.ok(cars);
    }

    // Получение машины по ID
    @GetMapping("/{id}")
    public ResponseEntity<Car> getCarById(@PathVariable Long id) {
        Optional<Car> car = carService.getCarById(id);
        return car.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<Resource> getCarPhoto(@PathVariable Long id) {
        // Получаем путь к изображению из сервиса
        String imagePath = carService.getPhotoByCarId(id);

        try {
            // Преобразуем путь к ресурсу
            Path filePath = Paths.get(imagePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Возвращаем файл с правильным типом содержимого
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, "image/jpeg") // Укажите нужный тип файла
                        .body(resource);
            } else {
                throw new RuntimeException("File not found or not readable: " + imagePath);
            }
        } catch (Exception e) {
            // Обработка ошибки при загрузке файла
            return ResponseEntity.internalServerError()
                    .body(null); // Можно добавить более подробное сообщение об ошибке
        }
    }
    // Удаление машины по ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCarById(@PathVariable Long id) {
        carService.deleteCarById(id);
        return ResponseEntity.noContent().build();
    }

    // Получение машин по модели
    @GetMapping("/model/{model}")
    public ResponseEntity<List<Car>> getCarsByModel(@PathVariable String model) {
        List<Car> cars = carService.getCarsByModel(model);
        return ResponseEntity.ok(cars);
    }

    // Получение машин по статусу
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Car>> getCarsByStatus(@PathVariable CarStatus status) {
        List<Car> cars = carService.getCarsByStatus(status);
        return ResponseEntity.ok(cars);
    }

    // Получение машины по номеру
    @GetMapping("/license/{licensePlate}")
    public ResponseEntity<Car> getCarByLicensePlate(@PathVariable String licensePlate) {
        Optional<Car> car = carService.getCarByLicensePlate(licensePlate);
        return car.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PatchMapping("/{id}")
    public ResponseEntity<Car> updateCar(
            @PathVariable Long id,
            @RequestParam(required = false) String make,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String tip,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String rule,
            @RequestParam(required = false) String licensePlate,
            @RequestParam(required = false) CarStatus carStatus,
            @RequestParam(required = false) Integer volume,
            @RequestParam(required = false) Double probeg,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(required = false) MultipartFile image
    ) {
        try {
            Car updatedCar = carService.updateCar(id, make, model, tip, year, rule, licensePlate, carStatus,
                    volume, probeg, color, price, image);
            return ResponseEntity.ok(updatedCar);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(500).body(null); // Ошибка при загрузке файла
        }
    }
}
