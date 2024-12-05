package com.example.carsharing.controller;

import com.example.carsharing.dto.BookingRequest;
import com.example.carsharing.entity.Booking;
import com.example.carsharing.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/create")
    public ResponseEntity<Booking> createBooking(@RequestBody BookingRequest bookingRequest) {
        try {
            LocalDate start = LocalDate.parse(bookingRequest.getStartDate());
            LocalDate end = LocalDate.parse(bookingRequest.getEndDate());

            Booking booking = bookingService.createBooking(
                    bookingRequest.getUserId(),
                    bookingRequest.getCarId(),
                    start,
                    end
            );

            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}