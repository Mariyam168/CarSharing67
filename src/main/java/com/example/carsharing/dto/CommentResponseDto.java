package com.example.carsharing.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentResponseDto {
    private Long id;
    private String content;
    private String userName; // Имя пользователя
    private Long carId;

    // Геттеры и сеттеры
}

