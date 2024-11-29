package com.example.carsharing.controller;

import com.example.carsharing.entity.User;
import com.example.carsharing.enums.UserStatus;
import com.example.carsharing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody User user) {
        User newUser = userService.registerUser(
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getDriverLicense(),
                user.getPhone()
        );
        return ResponseEntity.ok(newUser);
    }

    @PostMapping("/restore_password")
    public ResponseEntity<String> forgetPassword(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        userService.restorePassword(user);
        return ResponseEntity.ok("Link sent to your email");
    }
    @PutMapping("/restore_password")
    public ResponseEntity<String> restorePassword(@RequestParam String newPassword) {
        // Пример реализации обновления пароля
        return ResponseEntity.ok("Пароль успешно обновлен.");
    }
    @GetMapping("/confirm")
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        boolean isConfirmed = userService.confirmEmail(token);
        if (isConfirmed) {
            return ResponseEntity.ok("Email successfully confirmed!");
        } else {
            return ResponseEntity.status(400).body("Invalid or expired token.");
        }
    }
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

}
