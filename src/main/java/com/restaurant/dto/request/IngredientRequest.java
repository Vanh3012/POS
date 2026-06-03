package com.restaurant.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientRequest {
    @NotBlank(message = "NAME_REQUIRED")
    private String name;

    private String imageUrl;

    @NotBlank(message = "UNIT_REQUIRED")
    private String unit;

    @NotNull(message = "STOCK_QUANTITY_REQUIRED")
    @DecimalMin(value = "0.00", message = "STOCK_QUANTITY_INVALID")
    private BigDecimal stockQuantity;

    @DecimalMin(value = "0.00", message = "MIN_QUANTITY_INVALID")
    private BigDecimal minQuantity;
}
