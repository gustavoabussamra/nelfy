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
@Table(name = "withdrawal_requests", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Usuário que solicitou o saque
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // Valor solicitado
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WithdrawalStatus status = WithdrawalStatus.PENDING;
    
    @Column(name = "pix_key", length = 255)
    private String pixKey; // Chave PIX para transferência
    
    @Column(name = "pix_key_type", length = 50)
    private String pixKeyType; // Tipo da chave (CPF, EMAIL, TELEFONE, ALEATORIA)
    
    @Column(name = "receipt_file_path", length = 500)
    private String receiptFilePath; // Caminho do arquivo do comprovante PIX no MinIO
    
    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy; // Admin que processou o saque
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt; // Data de processamento
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Observações do admin
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum WithdrawalStatus {
        PENDING,      // Pendente
        PROCESSING,   // Em processamento
        COMPLETED,    // Concluído
        REJECTED      // Rejeitado
    }
}




