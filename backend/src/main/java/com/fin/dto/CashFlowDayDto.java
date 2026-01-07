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
public class CashFlowDayDto {
    private LocalDate date;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal netFlow;
    private BigDecimal balance;
    private List<TransactionForecastDto> transactions;
}




