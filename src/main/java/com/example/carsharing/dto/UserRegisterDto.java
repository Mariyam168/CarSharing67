package com.example.carsharing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDto {

    // Валидация для имени пользователя (обязательное поле, минимум 3 символа)
    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
    private String username;

    // Валидация для email (должен быть корректный email)
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный email")
    private String email;

    // Валидация для пароля (не менее 6 символов)
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
    private String password;

    // Валидация для номера телефона (с проверкой на кыргызский формат)
    @NotBlank(message = "Телефон не может быть пустым")
    @Pattern(regexp = "^\\+996\\d{9}$", message = "Телефонный номер должен начинаться с +996 и содержать 9 цифр")
    private String phone;

    // Валидация для водительского удостоверения
    @NotBlank(message = "Номер водительского удостоверения не может быть пустым")
    private String driverLicense;

    // Статус пользователя
    private String userStatus;

    // Роль пользователя
    private Long roleId;

}
