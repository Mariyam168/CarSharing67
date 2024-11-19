package com.example.carsharing.repository;

import com.example.carsharing.entity.Car;
import com.example.carsharing.enums.CarStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByModel(String model);

    List<Car> findByCarStatus(CarStatus status);

    Optional<Car> findByLicensePlate(String licensePlate);
    @Query("""
        SELECT c FROM Car c
        WHERE c.tip = :tip
        AND c.carStatus = 'AVAILABLE'
        AND c.id NOT IN (
            SELECT b.car.id FROM Booking b
            WHERE b.startTime < :endTime AND b.endTime > :startTime
        )
    """)
    List<Car> findAvailableCarsByModelAndDates(@Param("model") String tip,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
}
