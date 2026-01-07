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
@Table(name = "goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Goal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal targetAmount; // Valor alvo
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO; // Valor atual acumulado
    
    @Column(nullable = false)
    private LocalDate targetDate; // Data limite para alcançar a meta
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category; // Categoria opcional (ex: "Viagem", "Casa")
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;
    
    @Column(name = "completed_date")
    private LocalDate completedDate; // Data em que a meta foi alcançada
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Descrição opcional da meta
    
    @Column(name = "alert_threshold", precision = 5, scale = 2)
    private BigDecimal alertThreshold = BigDecimal.valueOf(80); // Alerta quando progresso < 80% do esperado
    
    @Column(name = "last_alert_date")
    private LocalDate lastAlertDate; // Data do último alerta enviado
    
    @Column(name = "is_off_track", nullable = false)
    private Boolean isOffTrack = false; // Se está desviando do objetivo
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Método para calcular o progresso em porcentagem
    public Double getProgressPercentage() {
        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }
        return Math.min(100.0, currentAmount.divide(targetAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue());
    }
    
    // Método para verificar se a meta foi alcançada
    public Boolean checkIfCompleted() {
        if (!isCompleted && currentAmount.compareTo(targetAmount) >= 0) {
            isCompleted = true;
            completedDate = LocalDate.now();
            isOffTrack = false; // Meta alcançada, não está desviando
            return true;
        }
        return false;
    }
    
    // Calcula dias restantes até a data alvo
    public Long getDaysRemaining() {
        if (targetDate == null || isCompleted) {
            return 0L;
        }
        LocalDate today = LocalDate.now();
        if (targetDate.isBefore(today)) {
            return 0L; // Data já passou
        }
        return java.time.temporal.ChronoUnit.DAYS.between(today, targetDate);
    }
    
    // Calcula o valor esperado até hoje baseado no progresso linear
    public BigDecimal getExpectedAmount() {
        if (targetDate == null || createdAt == null) {
            return BigDecimal.ZERO;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate startDate = createdAt.toLocalDate();
        
        if (today.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }
        
        if (today.isAfter(targetDate) || today.equals(targetDate)) {
            return targetAmount; // Já passou a data, esperado é o valor total
        }
        
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, targetDate);
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
        
        if (totalDays == 0) {
            return targetAmount;
        }
        
        // Progresso linear: (dias passados / total de dias) * valor alvo
        BigDecimal progressRatio = BigDecimal.valueOf(daysPassed)
                .divide(BigDecimal.valueOf(totalDays), 4, java.math.RoundingMode.HALF_UP);
        
        return targetAmount.multiply(progressRatio);
    }
    
    // Verifica se está desviando do objetivo
    public Boolean checkIfOffTrack() {
        if (isCompleted || targetDate == null) {
            isOffTrack = false;
            return false;
        }
        
        BigDecimal expectedAmount = getExpectedAmount();
        if (expectedAmount.compareTo(BigDecimal.ZERO) == 0) {
            isOffTrack = false;
            return false;
        }
        
        // Calcular porcentagem do esperado que foi alcançado
        BigDecimal progressRatio = currentAmount
                .divide(expectedAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        
        // Se o progresso atual é menor que o threshold do esperado, está desviando
        isOffTrack = progressRatio.compareTo(alertThreshold) < 0;
        
        return isOffTrack;
    }
}



