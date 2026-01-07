package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferralCommissionDto {
    private Long id;
    private Integer paymentYear;
    private Integer paymentMonth;
    private String subscriptionPlan;
    private BigDecimal monthlyAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private Boolean isPaid; // true se já foi pago, false se é futuro esperado
}

