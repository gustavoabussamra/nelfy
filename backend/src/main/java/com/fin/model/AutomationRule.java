package com.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "automation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String name; // Nome da regra
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // Descrição da regra
    
    // Condições (critérios para aplicar a regra)
    @Column(name = "condition_type", nullable = false)
    private String conditionType; // "DESCRIPTION_CONTAINS", "AMOUNT_RANGE", "MERCHANT", "DATE_PATTERN"
    
    @Column(name = "condition_value", columnDefinition = "TEXT")
    private String conditionValue; // Valor da condição (JSON ou texto)
    
    // Ações (o que fazer quando a condição for atendida)
    @Column(name = "action_type", nullable = false)
    private String actionType; // "AUTO_CATEGORIZE", "AUTO_TAG", "AUTO_APPROVE", "SEND_ALERT"
    
    @Column(name = "action_value", columnDefinition = "TEXT")
    private String actionValue; // Valor da ação (ID da categoria, tag, etc.)
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "priority")
    private Integer priority = 0; // Prioridade (maior = executa primeiro)
    
    @Column(name = "execution_count")
    private Integer executionCount = 0; // Quantas vezes foi executada
    
    @Column(name = "last_execution")
    private LocalDateTime lastExecution;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}




