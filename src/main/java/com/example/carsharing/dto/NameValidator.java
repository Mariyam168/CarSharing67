package com.example.carsharing.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NameValidator implements ConstraintValidator<ValidName, String> {

    @Override
    public void initialize(ValidName constraintAnnotation) {
        // обычно тут ничего не нужно
    }

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null || name.isEmpty()) {
            // Пусть @NotBlank или @NotEmpty обработают пустые имена
            return true;
        }

        // Разбиваем по пробелам и проверяем каждую часть
        String[] parts = name.split("\\s+");
        for (String part : parts) {
            // Первая буква заглавная, далее только буквы (русские или латиница)
            if (!part.matches("^[A-ZА-ЯЁ][a-zа-яё]*$")) {
                return false;
            }
        }
        return true;
    }
}
