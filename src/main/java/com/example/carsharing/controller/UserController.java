package com.example.carsharing.controller;

import com.example.carsharing.dto.LoginRequest;
import com.example.carsharing.dto.LoginResponse;
import com.example.carsharing.dto.UserRegisterDto;
import com.example.carsharing.dto.UserUpdateRequest;
import com.example.carsharing.entity.User;
import com.example.carsharing.security.JwtUtil;
import com.example.carsharing.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/getusers")
    public ResponseEntity<User> getUser(@RequestParam String email) {
        logger.info("Получен запрос с email: {}", email);
        User user = userService.getUserByEmail(email);
        if (user == null) {
            logger.warn("Пользователь с email {} не найден", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            logger.info("Login attempt for user: {}", request.getEmail());
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            if (passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
                String token = jwtUtil.generateToken(request.getEmail());
                return ResponseEntity.ok(new LoginResponse(token));
            } else {
                logger.warn("Invalid password for user: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
            }
        } catch (UsernameNotFoundException e) {
            logger.error("User not found: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            logger.error("Internal server error during login: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegisterDto userRegisterDto, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().toString());
        }
        try {
            userService.registerUser(userRegisterDto);
            return ResponseEntity.ok("Пользователь успешно зарегистрирован");
        } catch (Exception e) {
            logger.error("Error registering user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при регистрации пользователя");
        }
    }

    @PostMapping("/forgot_password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        try {
            User user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }
            String resetToken = userService.createPasswordResetToken(user);
            userService.sendPasswordResetEmail(user, resetToken);
            return ResponseEntity.ok("Password reset link sent to your email");
        } catch (Exception e) {
            logger.error("Error sending reset link: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending reset link");
        }
    }

    @PutMapping("/reset_password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        try {
            User user = userService.findUserByPasswordResetToken(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid or expired token");
            }
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userService.restorePassword(user);
            return ResponseEntity.ok("Password successfully reset");
        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error resetting password");
        }
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        try {
            boolean isConfirmed = userService.confirmEmail(token);
            if (isConfirmed) {
                return ResponseEntity.ok("Email successfully confirmed!");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token.");
            }
        } catch (Exception e) {
            logger.error("Error confirming email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error confirming email");
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error retrieving users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{userId}/avatar/upload")
    public ResponseEntity<String> uploadAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Файл не может быть пустым");
        }
        try {
            userService.saveAvatar(userId, file);
            return ResponseEntity.ok("Аватар успешно загружен");
        } catch (IOException e) {
            logger.error("Error uploading avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка загрузки аватара");
        }
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long userId) {
        try {
            String avatarPath = userService.getAvatarPath(userId);
            if (avatarPath == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            Path filePath = Paths.get("src/main/resources/avatar/").resolve(avatarPath.replace("/avatar/", "")).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
        } catch (IOException e) {
            logger.error("Error retrieving avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest updateRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bindingResult.getAllErrors());
        }
        try {
            User updatedUser = userService.updateUserFields(userId, updateRequest);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update user");
        }
    }
    @PostMapping("/registerUser")
    public ResponseEntity<User> registerUser(@RequestBody @Valid UserRegisterDto userRegisterDto) {
        User newUser = userService.register(userRegisterDto);
        return ResponseEntity.ok(newUser);
    }

    @GetMapping("/managers")
    public ResponseEntity<List<User>> getManagers() {
        List<String> roles = Collections.singletonList("MANAGER");
        List<User> managers = userService.getUsersByRoles(roles);

        if (managers.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(managers);
    }


}
