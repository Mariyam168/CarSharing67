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
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.id <> :bookingId AND " +
            "(b.startDate <= :endDate AND b.endDate >= :startDate)")
    List<Booking> findOverlappingBookingsByUserIdAndExcludeBooking(@Param("userId") Long userId,
                                                                   @Param("bookingId") Long bookingId,
                                                                   @Param("startDate") LocalDate startDate,
                                                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND " +
            "(b.startDate <= :endDate AND b.endDate >= :startDate)")
    List<Booking> findOverlappingBookingsByUserId(@Param("userId") Long userId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
    @Query("SELECT b FROM Booking b WHERE b.car.id = :carId " +
            "AND b.endDate >= :startDate " +
            "AND b.startDate <= :endDate " +
            "AND b.status = 'CONFIRMED'")
    List<Booking> findOverlappingBookingsByCarId(@Param("carId") Long carId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);


}
