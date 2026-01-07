package com.fin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String type;
    
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    private LocalDate transactionDate;
    
    private CategoryDto category;
    private Long accountId;
    private AccountDto account;
    private Boolean isPaid;
    private Boolean isInstallment;
    private Long parentTransactionId;
    private Integer installmentNumber;
    private Integer totalInstallments;
    
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    private LocalDate dueDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    private LocalDate paidDate;
    
    private Integer daysUntilDue;
    private Boolean isOverdue;
    private Integer paidInstallmentsCount; // Número de parcelas pagas (apenas para transações parceladas)
    private Integer totalInstallmentsCount; // Total de parcelas (apenas para transações parceladas)
}

