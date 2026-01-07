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
@Table(name = "recurring_transactions", indexes = {
    @Index(name = "idx_recurring_user_id", columnList = "user_id"),
    @Index(name = "idx_recurring_is_active", columnList = "is_active"),
    @Index(name = "idx_recurring_next_date", columnList = "next_occurrence_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // INCOME ou EXPENSE
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceType recurrenceType; // DAILY, WEEKLY, MONTHLY, YEARLY
    
    @Column(name = "recurrence_day")
    private Integer recurrenceDay; // Dia do mês (1-31) ou dia da semana (1-7)
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate; // Data final (opcional, null = infinito)
    
    @Column(name = "next_occurrence_date")
    private LocalDate nextOccurrenceDate; // Próxima data de ocorrência
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "auto_create", nullable = false)
    private Boolean autoCreate = false; // Se deve criar transação automaticamente
    
    @Column(name = "created_count")
    private Integer createdCount = 0; // Quantas transações já foram criadas
    
    @Column(name = "last_created_date")
    private LocalDate lastCreatedDate; // Data da última transação criada
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum RecurrenceType {
        DAILY,      // Diário
        WEEKLY,     // Semanal
        MONTHLY,    // Mensal
        YEARLY      // Anual
    }
    
    public enum TransactionType {
        INCOME,
        EXPENSE
    }
}




