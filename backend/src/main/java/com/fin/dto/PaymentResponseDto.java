package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private String paymentId;
    private String paymentUrl; // URL para redirecionar o usuário
    private String qrCode; // Para PIX
    private String status; // PENDING, APPROVED, REJECTED
    private String message;
}




