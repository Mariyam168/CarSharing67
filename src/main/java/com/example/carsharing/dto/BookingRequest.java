package com.example.carsharing.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BookingRequest {
        private Long userId;
        private Long carId;
        private String startDate;
        private String endDate;

    }
