package com.example.carsharing.service;

import com.example.carsharing.entity.Role;
import com.example.carsharing.entity.User;
import com.example.carsharing.enums.UserStatus;
import com.example.carsharing.repository.RoleRepository;
import com.example.carsharing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
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

        emailService.sendRegistrationEmail(email, subject, text);

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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    public String login(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return "User not found";
        }
        User user = userOptional.get();
        if (passwordEncoder.matches(password, user.getPassword())) {
            return "Login successful for user: " + user.getEmail();
        } else {
            return "Invalid email or password";
        }
    }

}
