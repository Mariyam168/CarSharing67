package com.example.carsharing.repository;

import com.example.carsharing.entity.Car;
import com.example.carsharing.enums.CarStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByModel(String model);

    List<Car> findByCarStatus(CarStatus status);

    Optional<Car> findByLicensePlate(String licensePlate);
}
