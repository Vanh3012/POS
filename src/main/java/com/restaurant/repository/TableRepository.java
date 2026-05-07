package com.restaurant.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.restaurant.models.entity.RestaurantTable;

import org.springframework.stereotype.Repository;

@Repository
public interface TableRepository extends JpaRepository<RestaurantTable, Long> {
    boolean existsByTableNumber(String tableNumber);
}
