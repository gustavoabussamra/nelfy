package com.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_user_id", columnList = "user_id"),
    @Index(name = "idx_account_type", columnList = "type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name; // Nome da conta (ex: "Conta Corrente BB", "Carteira Pix")
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type; // Tipo da conta
    
    @Column(precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO; // Saldo atual
    
    @Column(length = 50)
    private String bankName; // Nome do banco (se aplicável)
    
    @Column(length = 20)
    private String accountNumber; // Número da conta (se aplicável)
    
    @Column(length = 20)
    private String agency; // Agência (se aplicável)
    
    @Column
    private String description; // Descrição opcional
    
    @Column(nullable = false)
    private Boolean isActive = true; // Se a conta está ativa
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum AccountType {
        CHECKING_ACCOUNT, // Conta Corrente
        SAVINGS_ACCOUNT,  // Poupança
        CREDIT_CARD,      // Cartão de Crédito
        DIGITAL_WALLET,  // Carteira Digital (Pix, etc)
        INVESTMENT,      // Investimento
        CASH,            // Dinheiro em espécie
        OTHER            // Outro
    }
}




