package com.example.carsharing.repository;

import com.example.carsharing.entity.Comment;
import com.example.carsharing.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByCarId(Long carId);
    List<Comment> findByCarIdOrderByCreatedAtDesc(Long carId);

}
