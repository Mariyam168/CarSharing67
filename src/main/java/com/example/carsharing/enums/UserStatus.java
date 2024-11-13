package com.example.carsharing.enums;

public enum UserStatus {
    ACTIVE,      // Пользователь активен
    INACTIVE,    // Пользователь неактивен (например, временно отключён)
    SUSPENDED,   // Аккаунт заблокирован за нарушение
    PENDING      // Аккаунт ожидает верификации  (например, после регистрации)
}
