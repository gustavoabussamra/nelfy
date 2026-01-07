package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mensagem enviada para o Kafka contendo a transação e o userId
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionKafkaMessage {
    private TransactionDto transaction;
    private Long userId;
    private String operation; // CREATE, UPDATE, DELETE
}








