package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionDto {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String type;
    private Long categoryId;
    private CategoryDto category;
    private Long accountId;
    private AccountDto account;
    private String recurrenceType;
    private Integer recurrenceDay;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextOccurrenceDate;
    private Boolean isActive;
    private Boolean autoCreate;
    private Integer createdCount;
    private LocalDate lastCreatedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}




