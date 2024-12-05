package com.example.carsharing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@Entity
@Setter
@Getter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String token;

    @ManyToOne(cascade = CascadeType.ALL)
    private User user;
    public PasswordResetToken() {
    }
    public PasswordResetToken(String token, User user) {
        this.token = token;
        this.user = user;
    }
}