package com.example.carsharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotBlank(message = "Это поле не может быть пустым")
    private String username;

    @NotBlank(message = "Телефон не может быть пустым")
    @Pattern(regexp = "^\\+996\\d{9}$", message = "Телефонный номер должен начинаться с +996 и содержать 9 цифр")
    private String phone;

    @NotBlank(message = "Номер водительского удостоверения не может быть пустым")
    private String driverLicense;
}
