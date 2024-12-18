package com.example.carsharing.service;

import com.example.carsharing.controller.WebSocketController;
import com.example.carsharing.dto.UserRegisterDto;
import com.example.carsharing.dto.UserUpdateRequest;
import com.example.carsharing.entity.PasswordResetToken;
import com.example.carsharing.entity.Role;
import com.example.carsharing.entity.User;
import com.example.carsharing.enums.UserStatus;
import com.example.carsharing.repository.PasswordResetTokenRepository;
import com.example.carsharing.repository.RoleRepository;
import com.example.carsharing.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebSocketController webSocketController;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, WebSocketController webSocketController) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.webSocketController = webSocketController;
    }

    @Autowired
    private EmailService emailService;

    public User registerUser(UserRegisterDto userRegisterDto) {
        webSocketController.sendMessage("Регистрация пользователя с email: " + userRegisterDto.getEmail());

        if (userRepository.existsByEmail(userRegisterDto.getEmail())) {
            String errorMessage = "Пользователь с таким email уже существует.";
            webSocketController.sendMessage(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("USER");
            return roleRepository.save(newRole);
        });

        String confirmationToken = UUID.randomUUID().toString();

        User newUser = new User();
        newUser.setUsername(userRegisterDto.getUsername());
        newUser.setEmail(userRegisterDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userRegisterDto.getPassword()));
        newUser.setDriverLicense(userRegisterDto.getDriverLicense());
        newUser.setPhone(userRegisterDto.getPhone());
        newUser.setRole(userRole);
        newUser.setUserStatus(UserStatus.PENDING);
        newUser.setConfirmationToken(confirmationToken);

        userRepository.save(newUser);

        String subject = "Подтверждение email";
        String confirmationUrl = "http://localhost:8080/users/confirm?token=" + confirmationToken;
        String text = "Здравствуйте, " + userRegisterDto.getUsername() + "!\n\nПодтвердите ваш email по ссылке:\n" + confirmationUrl;

        emailService.sendEmail(userRegisterDto.getEmail(), subject, text);

        webSocketController.sendMessage("Пользователь успешно зарегистрирован. Для активации аккаунта подтвердите email.");
        return newUser;
    }


    public boolean confirmEmail(String token) {
        webSocketController.sendMessage("Попытка подтверждения email с токеном: " + token);

        User user = userRepository.findByConfirmationToken(token).orElse(null);
        if (user != null) {
            user.setUserStatus(UserStatus.ACTIVE);
            user.setConfirmationToken(null);
            userRepository.save(user);

            webSocketController.sendMessage("Email успешно подтверждён.");
            return true;
        }

        webSocketController.sendMessage("Неверный или просроченный токен подтверждения.");
        return false;
    }

    public User updateUserStatus(Long userId, UserStatus newStatus) {
        webSocketController.sendMessage("Обновление статуса пользователя ID: " + userId + " на " + newStatus);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    String errorMessage = "Пользователь с ID " + userId + " не найден.";
                    webSocketController.sendMessage(errorMessage);
                    return new IllegalArgumentException(errorMessage);
                });

        user.setUserStatus(newStatus);
        User updatedUser = userRepository.save(user);

        webSocketController.sendMessage("Статус пользователя обновлён на " + newStatus);
        return updatedUser;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    public void restorePassword(User user) {
        webSocketController.sendMessage("Инициализация восстановления пароля для пользователя с email: " + user.getEmail());

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiration(System.currentTimeMillis() + 3600000);
        userRepository.save(user);

        String url = "http://localhost:8080/users/reset_password?token=" + resetToken;
        emailService.sendEmail(user.getEmail(),
                "Восстановление пароля",
                "Перейдите по ссылке для восстановления пароля:\n" + url);

        webSocketController.sendMessage("Ссылка для восстановления пароля отправлена на email: " + user.getEmail());
    }


    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    public boolean resetPassword(String token, String newPassword) {
        webSocketController.sendMessage("Попытка сброса пароля с токеном: " + token);

        User user = userRepository.findByPasswordResetToken(token).orElse(null);
        if (user != null && user.getPasswordResetTokenExpiration() > System.currentTimeMillis()) {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setPasswordResetToken(null);
            user.setPasswordResetTokenExpiration(0);
            userRepository.save(user);

            webSocketController.sendMessage("Пароль успешно сброшен.");
            return true;
        }

        webSocketController.sendMessage("Неверный или просроченный токен сброса пароля.");
        return false;
    }
    public String createPasswordResetToken(User user) {
        // Генерируем токен
        String token = UUID.randomUUID().toString();

        // Сохраняем токен в базе данных
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        return token;
    }
    public User findUserByPasswordResetToken(String token) {
        // Находим токен в базе данных
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken == null) {
            return null; // Если токен не найден, возвращаем null
        }

        // Возвращаем пользователя, связанную с токеном
        return resetToken.getUser();
    }

    public void sendPasswordResetEmail(User user, String token) {
        String resetLink = "http://localhost:3000/reset_password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.info("Searching user by email: {}", email);

        // Ищем пользователя по email
        User user = userRepository.getByEmail(email);  // без Optional

        // Проверяем, найден ли пользователь
        if (user == null) {
            logger.error("User not found with email: {}", email);
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        logger.info("User found: {}", user.getEmail());

        // Преобразуем объект User в UserDetails без использования ролей
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList()  // Пустой список authorities, так как роли не используются
        );
    }

    public User getUserByEmail(String email) {
        User user = userRepository.getByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    public void saveAvatar(Long userId, MultipartFile file) throws IOException {
        webSocketController.sendMessage("Попытка загрузки аватара для пользователя ID: " + userId);

        String projectDir = System.getProperty("user.dir");
        Path avatarDir = Paths.get(projectDir, "src/main/resources/avatar");

        if (!Files.exists(avatarDir)) {
            Files.createDirectories(avatarDir);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            String errorMessage = "Имя файла отсутствует.";
            webSocketController.sendMessage(errorMessage);
            throw new IOException(errorMessage);
        }

        String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9.]", "_");
        String uniqueFileName = UUID.randomUUID().toString() + "_" + safeFileName;
        Path filePath = avatarDir.resolve(uniqueFileName);

        file.transferTo(filePath.toFile());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    String errorMessage = "Пользователь с ID " + userId + " не найден.";
                    webSocketController.sendMessage(errorMessage);
                    return new RuntimeException(errorMessage);
                });

        user.setAvatarPath("/avatar/" + uniqueFileName);
        userRepository.save(user);

        webSocketController.sendMessage("Аватар успешно сохранён для пользователя ID: " + userId);
    }

    public String getAvatarPath(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        return user.getAvatarPath();
    }
    public User updateUserFields(Long userId, UserUpdateRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateRequest.getUsername() != null) {
            user.setUsername(updateRequest.getUsername());
        }
        if (updateRequest.getPhone() != null) {
            user.setPhone(updateRequest.getPhone());
        }
        if (updateRequest.getDriverLicense() != null) {
            user.setDriverLicense(updateRequest.getDriverLicense());
        }

        return userRepository.save(user);
    }

}
