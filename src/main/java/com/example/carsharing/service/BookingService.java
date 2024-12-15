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

        // Проверка на активное бронирование у пользователя
        boolean hasActiveBooking = bookingRepository.existsByUserIdAndStatusIn(userId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (hasActiveBooking) {
            throw new IllegalArgumentException("У пользователя уже есть активное бронирование. Завершите текущее, чтобы создать новое.");
        }

        Car car = carRepository.findById(carId).orElseThrow(
                () -> new IllegalArgumentException("Автомобиль с ID " + carId + " не найден.")
        );

        // Проверка статуса машины
        if (car.getCarStatus() != CarStatus.AVAILABLE) {
            throw new IllegalArgumentException("Автомобиль уже забронирован и недоступен.");
        }

        // Проверка дат
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
        BigDecimal totalPrice = car.getPrice().multiply(BigDecimal.valueOf(numberOfDays));
        BigDecimal advancePayment = totalPrice.multiply(BigDecimal.valueOf(0.2)); // 20% предоплаты

        // Создание и сохранение бронирования
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCar(car);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.setAdvancePayment(advancePayment);

        // Обновление статуса машины на BOOKED
        car.setCarStatus(CarStatus.RESERVED);
        carRepository.save(car);

        bookingRepository.save(booking);
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
