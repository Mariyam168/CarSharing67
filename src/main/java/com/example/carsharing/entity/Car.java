package com.example.carsharing.entity;

import com.example.carsharing.enums.CarStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Setter
@Getter
@Table(name = "cars")
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String make;
    private String model;
    private String tip;
    private String year;
    private String rule;
    private String licensePlate;
    @Enumerated(EnumType.STRING)
    private CarStatus carStatus;
    private int volume;
    private double probeg;
    private String color;
    private BigDecimal price;
    private String CarImage;


}
