package com.example.carsharing.service;

import com.example.carsharing.dto.UserBookingDto;
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
import java.util.List;
import java.util.stream.Collectors;

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

        if (car.getCarStatus() != CarStatus.AVAILABLE) {
            throw new IllegalArgumentException("Автомобиль с ID " + carId + " недоступен для бронирования.");
        }

        LocalDate currentDate = LocalDate.now();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Дата начала и дата окончания не могут быть пустыми.");
        }
        if (startDate.isBefore(currentDate)) {
            throw new IllegalArgumentException("Дата начала бронирования не может быть раньше текущей даты.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Дата окончания бронирования не может быть раньше даты начала.");
        }

        long numberOfDays = ChronoUnit.DAYS.between(startDate, endDate);
        if (numberOfDays <= 0) {
            throw new IllegalArgumentException("Продолжительность бронирования должна быть хотя бы 1 день.");
        }

        BigDecimal totalPrice = car.getPrice().multiply(BigDecimal.valueOf(numberOfDays));
        BigDecimal advancePayment = totalPrice.multiply(BigDecimal.valueOf(0.2)); // 20% предоплаты

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCar(car);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.setAdvancePayment(advancePayment);

        bookingRepository.save(booking);

        System.out.println("Бронирование создано для пользователя ID: " + userId + " и автомобиля ID: " + car.getId());
        System.out.println("Начало аренды: " + startDate + ", окончание аренды: " + endDate);
        System.out.println("Общая стоимость: " + totalPrice + ", предоплата: " + advancePayment);

        return booking;
    }

    public Booking markBookingAsCompleted(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new IllegalArgumentException("Бронирование с ID " + bookingId + " не найдено.")
        );

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Бронирование уже имеет статус CONFIRMED.");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        System.out.println("Бронирование с ID " + bookingId + " отмечено как CONFIRMED.");

        return booking;
    }
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public List<UserBookingDto> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);

        if (bookings.isEmpty()) {
            throw new IllegalArgumentException("У пользователя с ID " + userId + " нет бронирований.");
        }

        return bookings.stream().map(booking -> {
            UserBookingDto dto = new UserBookingDto();
            dto.setBookingId(booking.getId());
            dto.setStartDate(booking.getStartDate());
            dto.setEndDate(booking.getEndDate());
            dto.setStatus(booking.getStatus());
            dto.setTotalPrice(booking.getTotalPrice());
            dto.setAdvancePayment(booking.getAdvancePayment());
            dto.setCarMake(booking.getCar().getMake());
            dto.setCarModel(booking.getCar().getModel());
            return dto;
        }).collect(Collectors.toList());
    }



}
