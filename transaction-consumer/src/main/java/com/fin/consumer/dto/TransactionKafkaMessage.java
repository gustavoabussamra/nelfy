package com.fin.consumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionKafkaMessage {
    private TransactionDto transaction;
    private Long userId;
    private String operation; // CREATE, UPDATE, DELETE
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDto {
        private Long id;
        private String description;
        private BigDecimal amount;
        private String type;
        private LocalDate transactionDate;
        private CategoryDto category;
        private Boolean isPaid;
        private Boolean isInstallment;
        private Long parentTransactionId;
        private Integer installmentNumber;
        private Integer totalInstallments;
        private LocalDate dueDate;
        private LocalDate paidDate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private Long id;
        private String name;
        private String icon;
        private String color;
        private String type;
    }
}








