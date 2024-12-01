package com.example.carsharing.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**","/users/register", "/users/confirm","/users/**","/cars/**").permitAll()
//                        .requestMatchers("/users/**", "/cars", "/cars/license/", "/cars/model/", "/cars/status/").hasAuthority("USER")
//                        .requestMatchers("/users","/users/{id}/status","/register","/swagger-ui/**", "/v3/api-docs/**").hasAuthority("ADMIN")
                                .anyRequest().permitAll()  // Разрешить доступ всем
                )
                .formLogin(AbstractAuthenticationFilterConfigurer::permitAll)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));  // Новый способ настройки CORS

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Настройка CORS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Разрешение на доступ с других доменов
        config.setAllowedOrigins(List.of("http://localhost:3000")); // Добавьте нужные домены
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Разрешенные HTTP методы
        config.setAllowedHeaders(List.of("*")); // Разрешить все заголовки
        config.setAllowCredentials(true); // Разрешить учетные данные (если нужно)

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
