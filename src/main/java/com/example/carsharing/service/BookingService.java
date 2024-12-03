package com.example.carsharing.service;

import com.example.carsharing.entity.Booking;
import com.example.carsharing.entity.Car;
import com.example.carsharing.entity.User;
import com.example.carsharing.enums.BookingStatus;
import com.example.carsharing.enums.CarStatus;
import com.example.carsharing.repository.BookingRepository;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, CarRepository carRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
    }

    public Booking createBooking(Long userId, Long carId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("Пользователь с ID " + userId + " не найден.")
        );

        Car car = carRepository.findById(carId).orElseThrow(
                () -> new IllegalArgumentException("Автомобиль с ID " + carId + " не найден.")
        );
        if (car.getCarStatus() == CarStatus.RESERVED) {
            throw new IllegalArgumentException("Автомобиль с ID " + carId + " уже забронирован.");
        }

        LocalDate currentDate = LocalDate.now();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Дата начала и дата окончания не могут быть пустыми.");
        }
        if (startDate.isBefore(currentDate)) {
            throw new IllegalArgumentException("Дата начала не может быть раньше текущей даты.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала.");
        }

        long numberOfDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (numberOfDays <= 0) {
            throw new IllegalArgumentException("Продолжительность бронирования должна быть хотя бы 1 день.");
        }

        car.setCarStatus(CarStatus.RESERVED);
        carRepository.save(car);
        BigDecimal totalPrice = car.getPrice().multiply(BigDecimal.valueOf(numberOfDays));
        BigDecimal advancePayment = totalPrice.multiply(BigDecimal.valueOf(0.2));

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCar(car);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.setAdvancePayment(advancePayment);

        bookingRepository.save(booking);

        System.out.println("Бронирование создано для пользователя: " + user.getId() + " на автомобиль: " + car.getId());
        System.out.println("Начало аренды: " + startDate + ", окончание аренды: " + endDate);
        System.out.println("Общая стоимость: " + totalPrice + ", предоплата: " + advancePayment);

        if (startDate.isBefore(currentDate) || startDate.equals(currentDate)) {
            car.setCarStatus(CarStatus.RENTED);
            carRepository.save(car);
            System.out.println("Статус автомобиля изменен на RENTED.");
        }

        updateCarStatusAfterBookingEnds(car, endDate);

        return booking;
    }

    private void updateCarStatusAfterBookingEnds(Car car, LocalDate endDate) {
        LocalDate currentDate = LocalDate.now();
        if (endDate.isBefore(currentDate)) {
            car.setCarStatus(CarStatus.AVAILABLE);
            carRepository.save(car);
            System.out.println("Статус автомобиля изменен на AVAILABLE после завершения бронирования.");
        }
    }
}
