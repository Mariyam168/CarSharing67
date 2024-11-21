package com.example.carsharing.controller;

import com.example.carsharing.entity.User;
import com.example.carsharing.enums.UserStatus;
import com.example.carsharing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
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

    /**
     * TODO: Написать метод, который принимает новый пароль и сохраняет его для юзера
     */
    @PutMapping("/restore_password")
    public ResponseEntity<?> restorePassword() {
        return null;
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

    @PatchMapping("/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        User updatedUser = userService.updateUserStatus(id, status);
        return ResponseEntity.ok(updatedUser);
    }
}
