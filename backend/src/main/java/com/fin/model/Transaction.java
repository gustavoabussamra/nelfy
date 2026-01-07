package com.fin.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_due_date", columnList = "due_date"),
    @Index(name = "idx_is_paid", columnList = "is_paid"),
    @Index(name = "idx_type", columnList = "type"),
    @Index(name = "idx_parent_transaction_id", columnList = "parent_transaction_id"),
    @Index(name = "idx_user_due_date", columnList = "user_id,due_date"),
    @Index(name = "idx_user_type_paid", columnList = "user_id,type,is_paid")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account; // Conta associada à transação
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
    
    @Column(name = "is_paid")
    private Boolean isPaid = true; // Para transações únicas, padrão é paga
    
    @Column(name = "is_installment")
    private Boolean isInstallment = false; // Indica se é uma transação parcelada
    
    @Column(name = "parent_transaction_id")
    private Long parentTransactionId; // ID da transação pai (se for parcela)
    
    @Column(name = "installment_number")
    private Integer installmentNumber; // Número da parcela (1, 2, 3...)
    
    @Column(name = "total_installments")
    private Integer totalInstallments; // Total de parcelas
    
    @Column(name = "due_date")
    private LocalDate dueDate; // Data de vencimento (para receitas/despesas futuras)
    
    @Column(name = "paid_date")
    private LocalDate paidDate; // Data em que foi paga/recebida
    
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Installment> installments;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum TransactionType {
        INCOME,
        EXPENSE
    }
}

