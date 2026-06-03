package com.restaurant.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientDTO {
    private Long id;
    private String name;
    private String imageUrl;
    private String unit;
    private BigDecimal stockQuantity;
    private BigDecimal minQuantity;
    private Boolean lowStock;
}
