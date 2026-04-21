package com.restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.restaurant.models.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllByOrderByNameAsc();

    List<User> findByActiveTrueOrderByNameAsc();

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
