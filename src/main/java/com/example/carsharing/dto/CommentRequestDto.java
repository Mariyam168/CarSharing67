package com.example.carsharing.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommentRequestDto {
    private Long userId;
    private String content;

}
