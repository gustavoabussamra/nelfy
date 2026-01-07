package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalDto {
    private Long id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private LocalDate targetDate;
    private Long categoryId;
    private CategoryDto category;
    private String description;
    private Boolean isCompleted;
    private LocalDate completedDate;
    private Double progressPercentage;
    private Long daysRemaining;
    private Boolean isOffTrack;
    private BigDecimal expectedAmount;
    private BigDecimal alertThreshold;
}



