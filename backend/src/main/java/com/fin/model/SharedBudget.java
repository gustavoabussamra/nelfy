package com.fin.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_budgets", indexes = {
    @Index(name = "idx_budget_id", columnList = "budget_id"),
    @Index(name = "idx_shared_user_id", columnList = "shared_user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;
    
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Dono do orçamento
    
    @ManyToOne
    @JoinColumn(name = "shared_user_id", nullable = false)
    private User sharedUser; // Usuário com quem foi compartilhado
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionType permission; // READ_ONLY ou READ_WRITE
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum PermissionType {
        READ_ONLY,  // Apenas visualização
        READ_WRITE  // Pode editar
    }
}




