package com.restaurant.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.restaurant.dto.request.IngredientRequest;
import com.restaurant.dto.request.UpdateIngredientStockRequest;
import com.restaurant.dto.response.IngredientDTO;
import com.restaurant.exception.ApiException;
import com.restaurant.models.entity.Ingredient;
import com.restaurant.repository.IngredientRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IngredientService {
    private final IngredientRepository ingredientRepo;

    @Transactional
    public List<IngredientDTO> viewAllIngredients() {
        return ingredientRepo.findAll()
                .stream()
                .sorted(Comparator.comparing(Ingredient::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toIngredientDTO)
                .toList();
    }

    @Transactional
    public IngredientDTO viewIngredientById(Long ingredientId) {
        Ingredient ingredient = findIngredient(ingredientId);
        return toIngredientDTO(ingredient);
    }

    @Transactional
    public IngredientDTO createIngredient(IngredientRequest request) {
        String name = request.getName().trim();
        if (ingredientRepo.findByNameIgnoreCase(name).isPresent()) {
            throw new ApiException("INGREDIENT_ALREADY_EXISTS", HttpStatus.BAD_REQUEST, "Ingredient da ton tai");
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setImageUrl(blankToNull(request.getImageUrl()));
        ingredient.setUnit(request.getUnit().trim());
        ingredient.setStockQuantity(quantity(request.getStockQuantity()));
        ingredient.setMinQuantity(quantityOrNull(request.getMinQuantity()));

        return toIngredientDTO(ingredientRepo.save(ingredient));
    }

    @Transactional
    public IngredientDTO updateIngredient(Long ingredientId, IngredientRequest request) {
        Ingredient ingredient = findIngredient(ingredientId);
        String name = request.getName().trim();

        ingredientRepo.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(ingredientId))
                .ifPresent(existing -> {
                    throw new ApiException("INGREDIENT_ALREADY_EXISTS", HttpStatus.BAD_REQUEST,
                            "Ingredient da ton tai");
                });

        ingredient.setName(name);
        ingredient.setImageUrl(blankToNull(request.getImageUrl()));
        ingredient.setUnit(request.getUnit().trim());
        ingredient.setStockQuantity(quantity(request.getStockQuantity()));
        ingredient.setMinQuantity(quantityOrNull(request.getMinQuantity()));

        return toIngredientDTO(ingredientRepo.save(ingredient));
    }

    @Transactional
    public IngredientDTO updateStock(Long ingredientId, UpdateIngredientStockRequest request) {
        Ingredient ingredient = findIngredient(ingredientId);
        ingredient.setStockQuantity(quantity(request.getStockQuantity()));
        return toIngredientDTO(ingredientRepo.save(ingredient));
    }

    @Transactional
    public void deleteIngredient(Long ingredientId) {
        Ingredient ingredient = findIngredient(ingredientId);
        ingredientRepo.delete(ingredient);
    }

    private Ingredient findIngredient(Long ingredientId) {
        return ingredientRepo.findById(ingredientId)
                .orElseThrow(() -> new ApiException("INGREDIENT_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Khong tim thay Ingredient"));
    }

    private IngredientDTO toIngredientDTO(Ingredient ingredient) {
        BigDecimal stockQuantity = quantity(ingredient.getStockQuantity());
        BigDecimal minQuantity = quantityOrNull(ingredient.getMinQuantity());

        return IngredientDTO.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .imageUrl(ingredient.getImageUrl())
                .unit(ingredient.getUnit())
                .stockQuantity(stockQuantity)
                .minQuantity(minQuantity)
                .lowStock(minQuantity != null && stockQuantity.compareTo(minQuantity) <= 0)
                .build();
    }

    private BigDecimal quantity(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantityOrNull(BigDecimal value) {
        return value == null ? null : quantity(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
