package com.restaurant.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIngredientStockRequest {
    @NotNull(message = "STOCK_QUANTITY_REQUIRED")
    @DecimalMin(value = "0.00", message = "STOCK_QUANTITY_INVALID")
    private BigDecimal stockQuantity;
}
