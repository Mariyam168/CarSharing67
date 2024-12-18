package com.example.carsharing.service;

import com.example.carsharing.controller.WebSocketController;
import com.example.carsharing.dto.CommentResponseDto;
import com.example.carsharing.entity.Comment;
import com.example.carsharing.entity.Car;
import com.example.carsharing.entity.User;
import com.example.carsharing.exception.ResourceNotFoundException;
import com.example.carsharing.repository.CommentRepository;
import com.example.carsharing.repository.CarRepository;
import com.example.carsharing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final WebSocketController webSocketController;

    public CommentService(CarRepository carRepository, UserRepository userRepository, CommentRepository commentRepository, WebSocketController webSocketController) {
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.webSocketController = webSocketController;
    }

    // Добавить комментарий
    public CommentResponseDto addComment(Long carId, Long userId, String content) {
        webSocketController.sendMessage("Запрос на добавление комментария: Пользователь ID " + userId + " для автомобиля ID " + carId);

        if (content == null || content.trim().isEmpty()) {
            String errorMessage = "Содержимое комментария не должно быть пустым.";
            webSocketController.sendMessage(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        Car car = carRepository.findById(carId)
                .orElseThrow(() -> {
                    String errorMessage = "Автомобиль с ID " + carId + " не найден.";
                    webSocketController.sendMessage(errorMessage);
                    return new ResourceNotFoundException(errorMessage);
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    String errorMessage = "Пользователь с ID " + userId + " не найден.";
                    webSocketController.sendMessage(errorMessage);
                    return new ResourceNotFoundException(errorMessage);
                });

        Comment comment = new Comment();
        comment.setCar(car);
        comment.setUser(user);
        comment.setContent(content);

        Comment savedComment = commentRepository.save(comment);

        CommentResponseDto response = new CommentResponseDto();
        response.setId(savedComment.getId());
        response.setContent(savedComment.getContent());
        response.setUserName(user.getUsername());
        response.setCarId(car.getId());

        webSocketController.sendMessage("Комментарий успешно добавлен. " );

        return response;
    }

    // Получить все комментарии к машине
    public List<CommentResponseDto> getCommentsByCarId(Long carId) {
        List<Comment> comments = commentRepository.findByCarIdOrderByCreatedAtDesc(carId);

        return comments.stream()
                .map(comment -> {
                    CommentResponseDto dto = new CommentResponseDto();
                    dto.setId(comment.getId());
                    dto.setContent(comment.getContent());
                    dto.setUserName(comment.getUser().getUsername());
                    dto.setCarId(comment.getCar().getId());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}

