package com.restaurant.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.dto.request.IngredientRequest;
import com.restaurant.dto.request.UpdateIngredientStockRequest;
import com.restaurant.dto.response.IngredientDTO;
import com.restaurant.service.IngredientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {
    private final IngredientService ingredientService;

    @GetMapping
    public List<IngredientDTO> viewAllIngredients() {
        return ingredientService.viewAllIngredients();
    }

    @GetMapping("/{ingredientId}")
    public IngredientDTO viewIngredientById(@PathVariable Long ingredientId) {
        return ingredientService.viewIngredientById(ingredientId);
    }

    @PostMapping
    public IngredientDTO createIngredient(@Valid @RequestBody IngredientRequest request) {
        return ingredientService.createIngredient(request);
    }

    @PutMapping("/{ingredientId}")
    public IngredientDTO updateIngredient(@PathVariable Long ingredientId,
            @Valid @RequestBody IngredientRequest request) {
        return ingredientService.updateIngredient(ingredientId, request);
    }

    @PatchMapping("/{ingredientId}/stock")
    public IngredientDTO updateStock(@PathVariable Long ingredientId,
            @Valid @RequestBody UpdateIngredientStockRequest request) {
        return ingredientService.updateStock(ingredientId, request);
    }

    @DeleteMapping("/{ingredientId}")
    public void deleteIngredient(@PathVariable Long ingredientId) {
        ingredientService.deleteIngredient(ingredientId);
    }
}
