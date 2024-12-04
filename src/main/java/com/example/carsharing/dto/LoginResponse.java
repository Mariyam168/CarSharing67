package com.example.carsharing.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;

    // Конструктор
    public LoginResponse(String token) {
        this.token = token;
    }


}
