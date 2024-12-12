package com.example.carsharing.controller;

import com.example.carsharing.dto.CommentRequestDto;
import com.example.carsharing.dto.CommentResponseDto;
import com.example.carsharing.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/car/{carId}/")
    public ResponseEntity<CommentResponseDto> addComment(@PathVariable Long carId,
                                                         @RequestBody CommentRequestDto commentRequestDto) {
        Long userId = commentRequestDto.getUserId();
        String content = commentRequestDto.getContent();

        CommentResponseDto comment = commentService.addComment(carId, userId, content);
        return ResponseEntity.ok(comment);
    }


    // Получить все комментарии к машине
    @GetMapping("/car/{carId}/")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByCar(@PathVariable Long carId) {
        List<CommentResponseDto> comments = commentService.getCommentsByCarId(carId);
        return ResponseEntity.ok(comments);
    }

}
