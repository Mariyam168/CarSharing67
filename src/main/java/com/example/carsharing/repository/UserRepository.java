package com.example.carsharing.repository;

import com.example.carsharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByConfirmationToken(String token);

    User getByEmail(String email);
    Optional<User> findByPasswordResetToken(String token);
}

