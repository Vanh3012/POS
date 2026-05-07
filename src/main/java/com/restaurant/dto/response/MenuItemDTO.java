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
public class MenuItemDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String description;
    private Long categoryId;
    private String categoryName;
}
