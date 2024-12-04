package com.example.carsharing.service;

import com.example.carsharing.entity.Role;
import com.example.carsharing.entity.User;
import com.example.carsharing.enums.UserStatus;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Service
public class UserService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    private EmailService emailService;

    public User registerUser(String username, String email, String password, String driverLicense, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("USER");
            return roleRepository.save(newRole);
        });

        String confirmationToken = UUID.randomUUID().toString();

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setDriverLicense(driverLicense);
        newUser.setPhone(phone);
        newUser.setRole(userRole);
        newUser.setUserStatus(UserStatus.PENDING);
        newUser.setConfirmationToken(confirmationToken);

        userRepository.save(newUser);

        String subject = "Confirm your email";
        String confirmationUrl = "http://localhost:8080/users/confirm?token=" + confirmationToken;
        String text = "Dear " + username + ",\n\nPlease confirm your email by clicking the link below:\n" + confirmationUrl;

        emailService.sendEmail(email, subject, text);

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
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        user.setUserStatus(newStatus);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void restorePassword(User user) {
        String url = "http://localhost:8080/users/restore";
        emailService.sendEmail(user.getEmail(),
                "Восстановление пароля",
                "Перейдите по ссылке для восстановления пароля \n" + url);
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



}
