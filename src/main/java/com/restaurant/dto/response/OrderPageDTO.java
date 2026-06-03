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
public class OrderPageDTO {
    private List<OrderListItemDTO> orders;
    private int page;
    private int size;
    private int totalPages;
    private long totalItems;
}
