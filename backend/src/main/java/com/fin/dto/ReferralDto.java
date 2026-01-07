package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferralDto {
    private Long id;
    private Long referrerId;
    private String referrerName;
    private Long referredId;
    private String referredName;
    private String referralCode;
    private Boolean rewardGiven;
    private String rewardType;
    private Integer rewardValue;
    private LocalDateTime rewardedAt;
    private LocalDateTime createdAt;
    
    // Novos campos para comissões
    private String referredPlan; // Plano atual do indicado
    private Double referredPlanPrice; // Preço do plano do indicado
    private List<ReferralCommissionDto> commissions; // Lista de pagamentos e comissões (passadas e futuras)
    private Integer totalPayments; // Total de pagamentos realizados
    private BigDecimal totalCommission; // Total de comissões recebidas deste indicado
    private BigDecimal expectedFutureCommission; // Total de comissões futuras esperadas
    private LocalDateTime subscriptionEndDate; // Data de término da assinatura do indicado
}

