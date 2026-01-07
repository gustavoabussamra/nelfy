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
@Table(name = "ai_learning_patterns")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiLearningPattern {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalText; // Texto original do usuário
    
    @Column(nullable = false)
    private String normalizedText; // Texto normalizado
    
    // Resultado extraído
    @Column(nullable = false)
    private String transactionType; // EXPENSE ou INCOME
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount; // Valor extraído
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amountPerInstallment; // Valor por parcela (se parcelado)
    
    private Integer installments; // Número de parcelas
    
    private String description; // Descrição extraída
    
    @Column(name = "transaction_date")
    private LocalDate transactionDate; // Data extraída
    
    private Long categoryId; // ID da categoria detectada
    
    private String categoryName; // Nome da categoria detectada
    
    // Padrões aprendidos pelo OpenAI (JSON)
    @Column(columnDefinition = "TEXT")
    private String learnedPatterns; // JSON com padrões extraídos pelo OpenAI
    
    // Metadados
    @Column(name = "is_processed")
    private Boolean isProcessed = false; // Se já foi processado pelo OpenAI
    
    @Column(name = "confidence_score")
    private Double confidenceScore; // Score de confiança (0.0 a 1.0)
    
    @Column(name = "processing_notes", columnDefinition = "TEXT")
    private String processingNotes; // Notas do processamento OpenAI
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}








