package com.example.carsharing.controller;

import com.example.carsharing.dto.BookingIdRequest;
import com.example.carsharing.dto.BookingRequest;
import com.example.carsharing.dto.UserBookingDto;
import com.example.carsharing.entity.Booking;
import com.example.carsharing.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
    @PutMapping("/confirmed")
    public ResponseEntity<Booking> markBookingAsCompleted(@RequestBody BookingIdRequest bookingIdRequest) {
        try {
            Booking updatedBooking = bookingService.markBookingAsCompleted(bookingIdRequest.getBookingId());
            return ResponseEntity.ok(updatedBooking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings() {
        List<Booking> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserBookingDto>> getUserBookings(@PathVariable Long userId) {
        try {
            List<UserBookingDto> userBookings = bookingService.getUserBookings(userId);
            return ResponseEntity.ok(userBookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}