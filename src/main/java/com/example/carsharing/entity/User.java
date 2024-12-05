package com.example.carsharing.entity;

import com.example.carsharing.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;


@Entity
@Setter
@Getter
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String username;
    private String email;
    private String password;
    private String driverLicense;
    private String phone;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    private String confirmationToken;
    private boolean isEmailConfirmed = false;

    // Поля для восстановления пароля
    private String passwordResetToken;
    @Column(name = "password_reset_token_expiration")
    private long passwordResetTokenExpiration=0;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(role);
    }
}
