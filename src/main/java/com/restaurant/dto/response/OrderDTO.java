package com.restaurant.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long tableId;
    private String tableNumber;
    private Long userId;
    private String customerName;
    private BigDecimal subTotal;
    private BigDecimal taxRate;
    private BigDecimal tax;
    private BigDecimal totalPrice;
    private String status;
    private String orderType;
    private String note;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal received;
    private BigDecimal changeAmount;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
