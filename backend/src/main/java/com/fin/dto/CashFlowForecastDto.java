package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowForecastDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<CashFlowDayDto> dailyForecast;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netFlow;
    private BigDecimal startingBalance;
    private BigDecimal endingBalance;
    private BigDecimal lowestBalance;
    private LocalDate lowestBalanceDate;
}




