package com.example.carsharing.dto;

import com.example.carsharing.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Getter
@Setter
public class UserBookingDto {
    private Long bookingId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private BigDecimal totalPrice;
    private BigDecimal advancePayment;
    private String carMake;
    private String carModel;


}
