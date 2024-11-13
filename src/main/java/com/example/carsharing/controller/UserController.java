package com.example.carsharing.controller;

import com.example.carsharing.entity.User;
import com.example.carsharing.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
