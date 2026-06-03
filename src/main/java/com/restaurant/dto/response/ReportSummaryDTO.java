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
public class ReportSummaryDTO {
    private long totalOrders;
    private long totalCustomers;
    private BigDecimal revenue;
    private BigDecimal moneyIn;
    private BigDecimal moneyOut;
}
