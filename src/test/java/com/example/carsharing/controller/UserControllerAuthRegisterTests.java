package com.example.carsharing.controller;

import com.example.carsharing.dto.LoginRequest;
import com.example.carsharing.dto.UserRegisterDto;
import com.example.carsharing.security.JwtUtil;
import com.example.carsharing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerAuthRegisterTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private PasswordEncoder passwordEncoder;

    private UserRegisterDto validRegisterDto;
    private LoginRequest validLoginRequest;

    @BeforeEach
    void setUp() {
        validRegisterDto = new UserRegisterDto();
        validRegisterDto.setUsername("ivan123");
        validRegisterDto.setEmail("newuser@example.com");
        validRegisterDto.setPassword("Password123!");
        validRegisterDto.setPhone("+996701234567");
        validRegisterDto.setDriverLicense("AB1234567");
        validRegisterDto.setUserStatus("ACTIVE");
        validRegisterDto.setRoleId(2L);

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("existing@example.com");
        validLoginRequest.setPassword("Secret");
    }

    @Test
    @DisplayName("Успешная регистрация нового пользователя")
    void registerUser_Success() throws Exception {
        doNothing().when(userService).registerUser(any(UserRegisterDto.class));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "ivan123",
                              "email": "newuser@example.com",
                              "password": "Password123!",
                              "phone": "+996701234567",
                              "driverLicense": "AB1234567",
                              "userStatus": "ACTIVE",
                              "roleId": 2
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(content().string("Пользователь успешно зарегистрирован"));

        verify(userService, times(1)).registerUser(any(UserRegisterDto.class));
    }

    @Test
    @DisplayName("Регистрация с уже существующим email приводит к ошибке 500")
    void registerUser_ServiceThrowsException() throws Exception {
        doThrow(new RuntimeException("duplicate"))
                .when(userService).registerUser(any(UserRegisterDto.class));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "username": "ivan123",
                              "email": "newuser@example.com",
                              "password": "Password123!",
                              "phone": "+996701234567",
                              "driverLicense": "AB1234567",
                              "userStatus": "ACTIVE",
                              "roleId": 2
                            }
                            """))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Ошибка при регистрации пользователя"));
    }

    @Test
    @DisplayName("Успешный логин и получение JWT")
    void login_Success() throws Exception {
        UserDetails mockDetails = org.springframework.security.core.userdetails.User
                .withUsername("existing@example.com")
                .password("encodedSecret")
                .roles("USER")
                .build();

        when(userDetailsService.loadUserByUsername("existing@example.com"))
                .thenReturn(mockDetails);
        when(passwordEncoder.matches("Secret", "encodedSecret"))
                .thenReturn(true);
        when(jwtUtil.generateToken("existing@example.com"))
                .thenReturn("jwt-token-123");

        mockMvc.perform(post("/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "existing@example.com",
                              "password": "Secret"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"));
    }

    @Test
    @DisplayName("Логин с неверным паролем возвращает 401")
    void login_InvalidPassword() throws Exception {
        UserDetails mockDetails = org.springframework.security.core.userdetails.User
                .withUsername("existing@example.com")
                .password("encodedSecret")
                .roles("USER")
                .build();

        when(userDetailsService.loadUserByUsername("existing@example.com"))
                .thenReturn(mockDetails);
        when(passwordEncoder.matches("WrongPass", "encodedSecret"))
                .thenReturn(false);

        mockMvc.perform(post("/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "existing@example.com",
                              "password": "WrongPass"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid password"));
    }
}
