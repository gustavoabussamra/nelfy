package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioDto {
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal incomeAdjustment; // Ajuste percentual na receita (ex: 0.1 = +10%)
    private BigDecimal expenseAdjustment; // Ajuste percentual nas despesas (ex: -0.2 = -20%)
    private CashFlowForecastDto forecast;
}




