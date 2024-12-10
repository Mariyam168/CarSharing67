package com.example.carsharing.controller;

import com.example.carsharing.dto.LoginRequest;
import com.example.carsharing.dto.LoginResponse;
import com.example.carsharing.dto.UserUpdateRequest;
import com.example.carsharing.entity.User;
import com.example.carsharing.security.JwtUtil;
import com.example.carsharing.service.UserService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserDetailsService userDetailsService;  // для загрузки данных пользователя

    @Autowired
    private PasswordEncoder passwordEncoder;  // для проверки пароля

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // Конструктор с внедрением зависимостей
    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // Получение пользователя по email
    @GetMapping("/getusers")
    public ResponseEntity<User> getUser(@RequestParam String email) {
        System.out.println("Получен запрос с email: " + email);

        User user = userService.getUserByEmail(email);
        if (user == null) {
            System.out.println("Пользователь не найден");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        System.out.println("Пользователь найден: " + user.getEmail());
        return ResponseEntity.ok(user);
    }

    // Логин: получение JWT токена при успешной аутентификации
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            logger.info("Login attempt for user: {}", request.getEmail());

            // Получаем пользователя через userDetailsService
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            if (userDetails == null) {
                throw new UsernameNotFoundException("User not found");
            }

            logger.info("User found: {}", userDetails.getUsername());

            // Проверяем, совпадает ли пароль
            if (passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
                // Если пароль правильный, генерируем токен
                String token = jwtUtil.generateToken(request.getEmail());
                return ResponseEntity.ok(new LoginResponse(token)); // Возвращаем токен
            } else {
                // Если пароль неверный
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
            }
        } catch (UsernameNotFoundException e) {
            logger.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            logger.error("Internal server error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error");
        }
    }

    // Регистрация нового пользователя
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

    // Восстановление пароля
    @PostMapping("/forgot_password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        try {
            // Проверка существования пользователя
            User user = userService.getUserByEmail(email);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            // Генерация токена для сброса пароля
            String resetToken = userService.createPasswordResetToken(user);

            // Отправка email с ссылкой на сброс пароля
            userService.sendPasswordResetEmail(user, resetToken);

            return ResponseEntity.ok("Password reset link sent to your email");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending reset link");
        }
    }

    // Сброс пароля
    @PutMapping("/reset_password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        try {
            User user = userService.findUserByPasswordResetToken(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid or expired token");
            }

            // Хэшируем новый пароль
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userService.restorePassword(user);

            return ResponseEntity.ok("Password successfully reset");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error resetting password");
        }
    }

    // Подтверждение email
    @GetMapping("/confirm")
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        boolean isConfirmed = userService.confirmEmail(token);
        if (isConfirmed) {
            return ResponseEntity.ok("Email successfully confirmed!");
        } else {
            return ResponseEntity.status(400).body("Invalid or expired token.");
        }
    }

//    // Обновление пароля
//    @PutMapping("/updatepassword")
//    public ResponseEntity<String> updatePassword(@RequestBody String newPassword, Authentication authentication) {
//        User user = (User) authentication.getPrincipal();
//        String encodedPassword = passwordEncoder.encode(newPassword);  // Хэшируем новый пароль
//        user.setPassword(encodedPassword);  // Обновляем пароль пользователя
//        userService.restorePassword(user);  // Сохраняем изменения в базе данных
//        return ResponseEntity.ok("Password successfully updated.");
//    }

    // Получить всех пользователей
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/{userId}/avatar/upload")
    public ResponseEntity<String> uploadAvatar(@PathVariable Long userId, @RequestParam("file") MultipartFile file) {
        try {
            userService.saveAvatar(userId, file);
            return ResponseEntity.ok("Аватар успешно загружен");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка загрузки аватара");
        }
    }
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long userId) throws IOException {
        String avatarPath = userService.getAvatarPath(userId);

        if (avatarPath == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // Находим файл по сохраненному пути
        Path filePath = Paths.get("src/main/resources/avatar/").resolve(avatarPath.replace("/avatar/", "")).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest updateRequest) {
        try {
            User updatedUser = userService.updateUserFields(userId, updateRequest);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

}
