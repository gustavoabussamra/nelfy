package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoalSuggestionDto {
    private String name;
    private String description;
    private BigDecimal suggestedAmount;
    private LocalDate suggestedTargetDate;
    private Long suggestedCategoryId;
    private String reason; // Por que esta meta foi sugerida
    private Double confidence; // Nível de confiança (0.0 a 1.0)
}




