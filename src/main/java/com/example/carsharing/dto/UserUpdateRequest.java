package com.example.carsharing.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String username;
    private String phone;
    private String driverLicense;
}
