package com.restaurant.dto.request;

import java.math.BigDecimal;

import com.restaurant.models.enums.PaymentMethod;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseOrderRequest {
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private BigDecimal received;
}
