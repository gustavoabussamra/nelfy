package com.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_commissions", indexes = {
    @Index(name = "idx_referral_id", columnList = "referral_id"),
    @Index(name = "idx_payment_month", columnList = "payment_year, payment_month")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReferralCommission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "referral_id", nullable = false)
    private Referral referral; // Referência ao referral
    
    @Column(name = "payment_year", nullable = false)
    private Integer paymentYear; // Ano do pagamento
    
    @Column(name = "payment_month", nullable = false)
    private Integer paymentMonth; // Mês do pagamento (1-12)
    
    @Column(name = "subscription_plan", nullable = false)
    @Enumerated(EnumType.STRING)
    private Subscription.SubscriptionPlan subscriptionPlan; // Plano pago
    
    @Column(name = "monthly_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyAmount; // Valor da mensalidade paga
    
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.valueOf(0.10); // 10% de comissão
    
    @Column(name = "commission_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionAmount; // Valor da comissão (10% da mensalidade)
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate; // Data do pagamento
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}




