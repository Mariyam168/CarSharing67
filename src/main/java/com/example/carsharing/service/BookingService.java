package com.example.carsharing.service;

import com.example.carsharing.controller.WebSocketController;
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
    private final WebSocketController webSocketController;


    public BookingService(BookingRepository bookingRepository, CarRepository carRepository, UserRepository userRepository, WebSocketController webSocketController) {
        this.bookingRepository = bookingRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.webSocketController = webSocketController;
    }

    public Booking createBooking(Long userId, Long carId, LocalDate startDate, LocalDate endDate) {
        webSocketController.sendMessage("Попытка создать бронирование: userId=" + userId + ", carId=" + carId
                + ", startDate=" + startDate + ", endDate=" + endDate);

        User user = userRepository.findById(userId).orElseThrow(() -> {
            String errorMessage = "Пользователь с ID " + userId + " не найден.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        });

        webSocketController.sendMessage("Пользователь с ID " + userId + " найден.");

        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsByUserId(userId, startDate, endDate);

        if (!overlappingBookings.isEmpty()) {
            boolean hasPendingBooking = overlappingBookings.stream()
                    .anyMatch(booking -> booking.getStatus() == BookingStatus.PENDING);
            if (hasPendingBooking) {
                String errorMessage = "У вас уже есть бронирование в ожидании на выбранные даты.";
                webSocketController.sendMessage(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            boolean hasConfirmedBooking = overlappingBookings.stream()
                    .anyMatch(booking -> booking.getStatus() == BookingStatus.CONFIRMED);
            if (hasConfirmedBooking) {
                String errorMessage = "У вас уже есть подтвержденное бронирование на выбранные даты.";
                webSocketController.sendMessage(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        }

        webSocketController.sendMessage("Проверка бронирований пользователя завершена успешно.");

        Car car = carRepository.findById(carId).orElseThrow(() -> {
            String errorMessage = "Автомобиль с ID " + carId + " не найден.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        });

        webSocketController.sendMessage("Автомобиль с ID " + carId + " найден.");

        // Проверка пересечения бронирований для автомобиля
        List<Booking> overlappingCarBookings = bookingRepository.findOverlappingBookingsByCarId(carId, startDate, endDate);
        if (!overlappingCarBookings.isEmpty()) {
            String errorMessage = "Данный автомобиль уже забронирован на указанные даты.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        webSocketController.sendMessage("Проверка бронирований автомобиля завершена успешно.");

        // Проверка дат
        LocalDate currentDate = LocalDate.now();
        if (startDate == null || endDate == null) {
            String errorMessage = "Дата начала и дата окончания не могут быть пустыми.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        if (startDate.isBefore(currentDate)) {
            String errorMessage = "Дата начала бронирования не может быть раньше текущей даты.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        if (endDate.isBefore(startDate)) {
            String errorMessage = "Дата окончания бронирования не может быть раньше даты начала.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        webSocketController.sendMessage("Проверка дат завершена успешно.");

        long numberOfDays = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal totalPrice = car.getPrice().multiply(BigDecimal.valueOf(numberOfDays));
        BigDecimal advancePayment = totalPrice.multiply(BigDecimal.valueOf(0.2)); // 20% предоплаты

        webSocketController.sendMessage("Стоимость бронирования рассчитана: общая стоимость=" + totalPrice
                + ", предоплата=" + advancePayment);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCar(car);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.setAdvancePayment(advancePayment);

        car.setCarStatus(CarStatus.RESERVED);
        carRepository.save(car);

        bookingRepository.save(booking);

        webSocketController.sendMessage("Бронирование успешно создано с ID=" + booking.getId());
        return booking;
    }

    public void deleteBookingById(Long bookingId) {
        webSocketController.sendMessage("Попытка удалить бронирование с ID=" + bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> {
            String errorMessage = "Бронирование с ID " + bookingId + " не найдено.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        });

        webSocketController.sendMessage("Бронирование с ID " + bookingId + " найдено.");

        // Освобождение машины, если статус бронирования был подтверждён
        Car car = booking.getCar();
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.PENDING) {
            car.setCarStatus(CarStatus.AVAILABLE);
            carRepository.save(car);
            webSocketController.sendMessage("Автомобиль с ID=" + car.getId() + " освобождён.");
        }

        bookingRepository.delete(booking);
        webSocketController.sendMessage("Бронирование с ID=" + bookingId + " удалено.");
    }

    public Booking markBookingAsCompleted(Long bookingId) {
        webSocketController.sendMessage("Попытка отметить бронирование с ID=" + bookingId + " как завершённое.");

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> {
            String errorMessage = "Бронирование с ID " + bookingId + " не найдено.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        });

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        webSocketController.sendMessage("Бронирование с ID=" + bookingId + " отмечено как CONFIRMED.");

        return booking;
    }

    public List<Booking> getAllBookings() {
        webSocketController.sendMessage("Получение всех бронирований.");

        List<Booking> bookings = bookingRepository.findAll();
        webSocketController.sendMessage("Всего найдено " + bookings.size() + " бронирований.");

        return bookings;
    }

    public List<UserBookingDto> getUserBookings(Long userId) {
        webSocketController.sendMessage("Получение бронирований для пользователя с ID=" + userId);

        List<Booking> bookings = bookingRepository.findByUserId(userId);
        if (bookings.isEmpty()) {
            String errorMessage = "У пользователя с ID " + userId + " нет бронирований.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LocalDate currentDate = LocalDate.now();
        webSocketController.sendMessage("Найдено " + bookings.size() + " бронирований для пользователя с ID=" + userId);

        return bookings.stream().map(booking -> {
            Car car = booking.getCar();

            if (booking.getEndDate().isBefore(currentDate) && car.getCarStatus() != CarStatus.AVAILABLE) {
                car.setCarStatus(CarStatus.AVAILABLE);
                carRepository.save(car);
                webSocketController.sendMessage("Автомобиль с ID=" + car.getId() + " отмечен как AVAILABLE.");
            }

            if (!booking.getStartDate().isAfter(currentDate) && !booking.getEndDate().isBefore(currentDate)
                    && car.getCarStatus() != CarStatus.RENTED) {
                car.setCarStatus(CarStatus.RENTED);
                carRepository.save(car);
                webSocketController.sendMessage("Автомобиль с ID=" + car.getId() + " отмечен как RENTED.");
            }

            UserBookingDto dto = new UserBookingDto();
            dto.setBookingId(booking.getId());
            dto.setStartDate(booking.getStartDate());
            dto.setEndDate(booking.getEndDate());
            dto.setStatus(booking.getStatus());
            dto.setTotalPrice(booking.getTotalPrice());
            dto.setAdvancePayment(booking.getAdvancePayment());
            dto.setCarMake(car.getMake());
            dto.setCarModel(car.getModel());
            return dto;
        }).collect(Collectors.toList());
    }

}
