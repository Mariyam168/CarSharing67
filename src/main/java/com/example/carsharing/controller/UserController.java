package com.example.carsharing.controller;

import com.example.carsharing.entity.User;
import com.example.carsharing.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("users")
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password) {
        String result = userService.login(email, password);

        if (result.startsWith("Login successful")) {
            return ResponseEntity.ok("http://localhost:8080/home");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
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
