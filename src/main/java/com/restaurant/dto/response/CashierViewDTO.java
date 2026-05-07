package com.restaurant.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashierViewDTO {
    private List<CategoryDTO> categories;
    private List<MenuItemDTO> menuItems;
    private List<TableDTO> tables;
    private List<OrderDTO> orders;
}
