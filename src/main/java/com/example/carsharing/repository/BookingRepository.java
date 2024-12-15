package com.example.carsharing.repository;

import com.example.carsharing.entity.Booking;
import com.example.carsharing.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    boolean existsByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND " +
            "(b.startDate <= :endDate AND b.endDate >= :startDate)")
    List<Booking> findOverlappingBookingsByUserId(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

}
