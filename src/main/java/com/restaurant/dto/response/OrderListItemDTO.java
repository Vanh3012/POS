package com.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListItemDTO {
    private Long id;
    private String orderCode;
    private LocalDateTime createdAt;
    private String customerName;
    private String menuName;
    private String orderType;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
    private String paymentMethod;
    private BigDecimal received;
    private BigDecimal changeAmount;
}
