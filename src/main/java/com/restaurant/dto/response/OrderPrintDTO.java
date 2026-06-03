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
public class OrderPrintDTO {
    private Long orderId;
    private Long paymentId;
    private String orderCode;
    private String customerName;
    private String tableNumber;
    private String orderType;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionRef;
    private String paymentUrl;
    private String qrContent;
    private LocalDateTime createdAt;
    private BigDecimal subTotal;
    private BigDecimal tax;
    private BigDecimal totalPrice;
    private List<OrderItemDTO> items;
}
