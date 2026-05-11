package com.restaurant.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.restaurant.models.enums.OrderType;
import com.restaurant.models.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private Long tableId;
    private String customerName;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    private String note;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private BigDecimal received;

    @Valid
    @NotEmpty(message = "Order items are required")
    private List<CreateOrderItemRequest> items;
}
