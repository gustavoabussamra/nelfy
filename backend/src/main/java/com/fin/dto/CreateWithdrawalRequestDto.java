package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWithdrawalRequestDto {
    private BigDecimal amount;
    private String pixKey;
    private String pixKeyType; // CPF, EMAIL, TELEFONE, ALEATORIA
}




