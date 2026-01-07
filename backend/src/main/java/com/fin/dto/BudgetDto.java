package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BudgetDto {
    private Long id;
    private String name;
    private BigDecimal limitAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private CategoryDto category;
    private Integer alertPercentage;
    private Boolean isActive;
    private BigDecimal currentSpent;
    private BigDecimal remaining;
    private BigDecimal percentageUsed;
    private Boolean alertTriggered;
}










