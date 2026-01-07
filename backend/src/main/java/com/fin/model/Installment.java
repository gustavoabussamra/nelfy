package com.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Installment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;
    
    @Column(nullable = false)
    private Integer installmentNumber; // Número da parcela (1, 2, 3...)
    
    @Column(nullable = false)
    private Integer totalInstallments; // Total de parcelas
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // Valor da parcela
    
    @Column(nullable = false)
    private LocalDate dueDate; // Data de vencimento
    
    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;
    
    @Column(name = "paid_date")
    private LocalDate paidDate; // Data em que foi paga
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}










