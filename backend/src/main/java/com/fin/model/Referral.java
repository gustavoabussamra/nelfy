package com.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "referrals", indexes = {
    @Index(name = "idx_referrer_id", columnList = "referrer_id"),
    @Index(name = "idx_referred_id", columnList = "referred_id"),
    @Index(name = "idx_referral_code", columnList = "referral_code")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Referral {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer; // Usuário que convidou
    
    @ManyToOne
    @JoinColumn(name = "referred_id", nullable = false, unique = true)
    private User referred; // Usuário que foi convidado
    
    @Column(name = "referral_code", nullable = false, unique = true, length = 20)
    private String referralCode; // Código único de referência
    
    @Column(name = "reward_given", nullable = false)
    private Boolean rewardGiven = false; // Se o prêmio já foi dado
    
    @Column(name = "reward_type")
    @Enumerated(EnumType.STRING)
    private RewardType rewardType; // Tipo de prêmio dado
    
    @Column(name = "reward_value")
    private Integer rewardValue; // Valor do prêmio (ex: 30 dias grátis)
    
    @Column(name = "rewarded_at")
    private LocalDateTime rewardedAt; // Data em que o prêmio foi dado
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum RewardType {
        FREE_MONTH, // 1 mês grátis
        DISCOUNT, // Desconto
        CASHBACK // Cashback
    }
}




