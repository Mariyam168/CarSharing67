package com.example.carsharing.repository;

import com.example.carsharing.entity.Booking;
import com.example.carsharing.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    boolean existsByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);
}
