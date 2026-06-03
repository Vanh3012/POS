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
public class VnpayPaymentDTO {
    private Long orderId;
    private Long paymentId;
    private String orderCode;
    private BigDecimal amount;
    private String status;
    private String transactionRef;
    private String paymentUrl;
    private String qrContent;
    private LocalDateTime expiredAt;
}
