package com.example.carsharing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

   // Ленивое внедрение, чтобы избежать циклической зависимости
    private final UserDetailsService userDetailsService;

    // Конструктор для внедрения зависимостей
    public JwtFilter(JwtUtil jwtUtil, @Lazy  UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Проверка наличия токена
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7); // Извлекаем токен из заголовка
        username = jwtUtil.extractUsername(jwt); // Извлекаем имя пользователя из токена

        // Проверяем, существует ли имя пользователя и нет ли аутентификации
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Загружаем детали пользователя
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Проверка валидности токена
                if (jwtUtil.isTokenValid(jwt, userDetails.getUsername())) {
                    // Создание аутентификации с использованием кастомного JwtAuthenticationToken
                    JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    // Устанавливаем аутентификацию в контекст безопасности
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // В случае ошибки аутентификации (например, пользователь не найден)
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Authentication failed: " + e.getMessage());
                return; // Останавливаем дальнейшую обработку
            }
        }

        // Передаем запрос и ответ в следующую часть фильтра
        filterChain.doFilter(request, response);
    }
}
