package com.example.carsharing.enums;

public enum CarStatus {
    RESERVED,//Забронирован, но еще не забран.
    AVAILABLE,//Свободен для бронирования.
    RENTED,//В аренде, используется пользователем.
    UNAVAILABLE//Недоступен (например, на обслуживании).
}
