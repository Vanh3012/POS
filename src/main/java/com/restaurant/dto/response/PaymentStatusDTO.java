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
public class PaymentStatusDTO {
    private Long orderId;
    private Long paymentId;
    private BigDecimal amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionRef;
    private String providerTransactionNo;
    private String providerResponseCode;
    private String bankCode;
    private LocalDateTime paidAt;
}
