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
import java.util.stream.Collectors;


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
        if (userRepository.existsByEmail(userRegisterDto.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует.");
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

        return newUser;
    }

    public boolean confirmEmail(String token) {
        User user = userRepository.findByConfirmationToken(token).orElse(null);
        if (user != null) {
            user.setUserStatus(UserStatus.ACTIVE);
            user.setConfirmationToken(null);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public User updateUserStatus(Long userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с ID " + userId + " не найден."));
        user.setUserStatus(newStatus);
        return userRepository.save(user);
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    public void restorePassword(User user) {
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiration(System.currentTimeMillis() + 3600000);
        userRepository.save(user);

        String url = "http://localhost:8080/users/reset_password?token=" + resetToken;
        emailService.sendEmail(user.getEmail(),
                "Восстановление пароля",
                "Перейдите по ссылке для восстановления пароля:\n" + url);
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
        String projectDir = System.getProperty("user.dir");
        Path avatarDir = Paths.get(projectDir, "src/main/resources/avatar");

        if (!Files.exists(avatarDir)) {
            Files.createDirectories(avatarDir);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new IOException("Имя файла отсутствует.");
        }

        String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9.]", "_");
        String uniqueFileName = UUID.randomUUID().toString() + "_" + safeFileName;
        Path filePath = avatarDir.resolve(uniqueFileName);

        file.transferTo(filePath.toFile());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь с ID " + userId + " не найден."));

        user.setAvatarPath("/avatar/" + uniqueFileName);
        userRepository.save(user);
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
    public User register(UserRegisterDto userRegisterDto) {
        if (userRepository.existsByEmail(userRegisterDto.getEmail())) {
            throw new RuntimeException("Пользователь с таким email уже существует.");
        }

        // Если ID роли не указано, присваиваем роль MANAGER по умолчанию
        Role role;
        if (userRegisterDto.getRoleId() == null) {
            role = roleRepository.findByName("MANAGER")
                    .orElseThrow(() -> new RuntimeException("Роль MANAGER не найдена."));
        } else {
            role = roleRepository.findById(userRegisterDto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Роль с ID " + userRegisterDto.getRoleId() + " не найдена."));
        }

        User newUser = new User();
        newUser.setUsername(userRegisterDto.getUsername());
        newUser.setEmail(userRegisterDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(userRegisterDto.getPassword()));
        newUser.setDriverLicense(userRegisterDto.getDriverLicense());
        newUser.setPhone(userRegisterDto.getPhone());
        newUser.setRole(role);
        newUser.setUserStatus(UserStatus.ACTIVE); // Новый пользователь активен сразу

        userRepository.save(newUser);

        logger.info("Пользователь с ролью {} и email {} успешно зарегистрирован.", role.getName(), userRegisterDto.getEmail());
        return newUser;
    }


    public List<User> getUsersByRoles(List<String> roleNames) {
        // Получаем роли по именам
        List<Role> roles = roleRepository.findByNameIn(roleNames);

        if (roles.isEmpty()) {
            throw new RuntimeException("Роли не найдены: " + roleNames);
        }

        // Возвращаем пользователей, у которых роли соответствуют заданным
        return userRepository.findAll().stream()
                .filter(user -> roles.contains(user.getRole()))
                .collect(Collectors.toList());
    }


}
