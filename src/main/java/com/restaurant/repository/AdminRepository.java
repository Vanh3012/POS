package com.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.restaurant.models.entity.User;

@Repository
public interface AdminRepository extends JpaRepository<User, Long> {
    List<User> findAll();

    Optional<User> findByEmail(String email);

    List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email);
}
