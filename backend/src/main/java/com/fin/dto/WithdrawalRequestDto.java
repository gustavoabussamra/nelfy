package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private BigDecimal amount;
    private String status;
    private String pixKey;
    private String pixKeyType;
    private String receiptFileUrl; // URL para acessar o comprovante
    private Long processedById;
    private String processedByName;
    private LocalDateTime processedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}




