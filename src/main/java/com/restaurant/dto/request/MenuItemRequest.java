package com.restaurant.dto.request;

import java.math.BigDecimal;

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
public class MenuItemRequest {
    @NotBlank(message = "NAME_REQUIRED")
    private String name;

    @NotBlank(message = "DESCRIPTION_REQUIRED")
    private String description;

    @NotNull(message = "PRICE_REQUIRED")
    private BigDecimal price;

    @NotBlank(message = "IMAGE_URL_REQUIRED")
    private String imageUrl;

    private Long categoryId;
}
