package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private String plan;
    private String paymentMethod; // CREDIT_CARD, PIX, BOLETO
    private String returnUrl; // URL para retorno após pagamento
}




