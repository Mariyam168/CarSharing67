package com.example.carsharing.service;

import com.example.carsharing.controller.WebSocketController;
import com.example.carsharing.dto.UserBookingDto;
import com.example.carsharing.entity.Booking;
import com.example.carsharing.entity.Car;
import com.example.carsharing.entity.User;
import com.example.carsharing.enums.BookingStatus;
import com.example.carsharing.enums.CarStatus;
import com.example.carsharing.enums.UserStatus;
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
        User user = userRepository.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("Пользователь не найден. Пожалуйста, проверьте данные.")
        );

        // Проверка статуса пользователя
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Ваш профиль не активирован. Обратитесь в службу поддержки.");
        }

        // Проверка на перекрывающиеся бронирования пользователя
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsByUserId(userId, startDate, endDate);
        if (!overlappingBookings.isEmpty()) {
            boolean hasPendingBooking = overlappingBookings.stream()
                    .anyMatch(booking -> booking.getStatus() == BookingStatus.PENDING);
            if (hasPendingBooking) {
                throw new IllegalArgumentException("У вас есть ожидающее подтверждение бронирование на эти даты.");
            }

            boolean hasConfirmedBooking = overlappingBookings.stream()
                    .anyMatch(booking -> booking.getStatus() == BookingStatus.CONFIRMED);
            if (hasConfirmedBooking) {
                throw new IllegalArgumentException("У вас уже есть подтвержденное бронирование на эти даты.");
            }
        }

        // Проверка автомобиля
        Car car = carRepository.findById(carId).orElseThrow(() ->
                new IllegalArgumentException("Автомобиль не найден. Выберите другой.")
        );

        // Проверка на перекрывающиеся бронирования автомобиля
        List<Booking> overlappingCarBookings = bookingRepository.findOverlappingBookingsByCarId(carId, startDate, endDate);
        if (!overlappingCarBookings.isEmpty()) {
            throw new IllegalArgumentException("Этот автомобиль уже забронирован на выбранные даты.");
        }

        // Проверка дат
        LocalDate currentDate = LocalDate.now();
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Укажите даты начала и окончания бронирования.");
        }
        if (startDate.isBefore(currentDate)) {
            throw new IllegalArgumentException("Дата начала не может быть раньше текущей даты.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Дата окончания не может быть раньше даты начала.");
        }

        // Расчет стоимости
        long numberOfDays = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal totalPrice = car.getPrice().multiply(BigDecimal.valueOf(numberOfDays));
        BigDecimal advancePayment = totalPrice.multiply(BigDecimal.valueOf(0.2)); // 20% предоплаты

        // Создание бронирования
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCar(car);
        booking.setStartDate(startDate);
        booking.setEndDate(endDate);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.setAdvancePayment(advancePayment);

        // Обновление статуса автомобиля
        car.setCarStatus(CarStatus.RESERVED);
        carRepository.save(car);

        bookingRepository.save(booking);

        // Уведомление пользователя
        webSocketController.sendMessage("Бронирование успешно создано! Общая стоимость: " + totalPrice + " руб.");

        return booking;
    }

    public void deleteBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new IllegalArgumentException("Бронирование не найдено. Возможно, оно уже удалено.")
        );

        // Освобождение машины, если статус бронирования был подтверждён
        Car car = booking.getCar();
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.PENDING) {
            car.setCarStatus(CarStatus.AVAILABLE);
            carRepository.save(car);
        }

        bookingRepository.delete(booking);

        // Отправляем сообщение пользователю только об успешном удалении
        webSocketController.sendMessage("Ваше бронирование успешно удалено.");
    }

    public Booking markBookingAsCompleted(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new IllegalArgumentException("Бронирование не найдено.")
        );

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Сообщение только об успешном завершении
        webSocketController.sendMessage("Бронирование успешно завершено. Спасибо за использование нашего сервиса!");

        return booking;
    }

    public List<Booking> getAllBookings() {
        // Убираем ненужные сообщения
        return bookingRepository.findAll();
    }

    public List<UserBookingDto> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        if (bookings.isEmpty()) {
            throw new IllegalArgumentException("У вас пока нет активных бронирований.");
        }

        LocalDate currentDate = LocalDate.now();

        return bookings.stream().map(booking -> {
            Car car = booking.getCar();

            // Обновляем статус автомобиля, если требуется
            if (booking.getEndDate().isBefore(currentDate) && car.getCarStatus() != CarStatus.AVAILABLE) {
                car.setCarStatus(CarStatus.AVAILABLE);
                carRepository.save(car);
            } else if (!booking.getStartDate().isAfter(currentDate) && !booking.getEndDate().isBefore(currentDate)
                    && car.getCarStatus() != CarStatus.RENTED) {
                car.setCarStatus(CarStatus.RENTED);
                carRepository.save(car);
            }

            // Создаем DTO для отправки клиенту
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
