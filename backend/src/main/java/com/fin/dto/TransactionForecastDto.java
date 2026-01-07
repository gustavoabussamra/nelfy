package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionForecastDto {
    private String description;
    private BigDecimal amount;
    private String type;
    private LocalDate date;
    private String categoryName;
    private Boolean isRecurring;
}




