package com.example.carsharing.dto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NameValidator implements ConstraintValidator<ValidName, String> {

    @Override
    public void initialize(ValidName constraintAnnotation) {}

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        if (name == null || name.isEmpty()) {
            return true;  // Если имя пустое, то аннотация @NotEmpty или другая аннотация должна обработать.
        }

        // Проверяем, чтобы имя начиналось с заглавной буквы и не содержало цифр
        String[] parts = name.split(" ");
        for (String part : parts) {
            if (!part.matches("^[A-ZА-ЯЁ][a-zа-яё]*$")) {  // Первая буква заглавная, далее только буквы
                return false;
            }
        }
        return true;
    }
}
