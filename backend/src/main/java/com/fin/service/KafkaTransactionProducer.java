package com.fin.service;

import com.fin.dto.TransactionDto;
import com.fin.dto.TransactionKafkaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaTransactionProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaTransactionProducer.class);
    
    @Autowired
    private KafkaTemplate<String, TransactionKafkaMessage> kafkaTemplate;
    
    @Value("${kafka.topic.transactions:transactions}")
    private String transactionsTopic;
    
    /**
     * Envia uma transação para o tópico do Kafka
     * @param transactionDto - DTO da transação a ser enviada
     * @param userId - ID do usuário (usado como chave para particionamento)
     */
    public void sendTransaction(TransactionDto transactionDto, Long userId) {
        try {
            String key = userId.toString();
            
            TransactionKafkaMessage message = new TransactionKafkaMessage();
            message.setTransaction(transactionDto);
            message.setUserId(userId);
            message.setOperation("CREATE");
            
            logger.info("Enviando transação para Kafka - Tópico: {}, UserId: {}, Operation: {}", 
                transactionsTopic, userId, message.getOperation());
            
            CompletableFuture<SendResult<String, TransactionKafkaMessage>> future = 
                kafkaTemplate.send(transactionsTopic, key, message);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Transação enviada com sucesso para Kafka - Offset: {}, Partition: {}", 
                        result.getRecordMetadata().offset(), 
                        result.getRecordMetadata().partition());
                } else {
                    logger.error("Erro ao enviar transação para Kafka", ex);
                    throw new RuntimeException("Erro ao enviar transação para Kafka: " + ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Erro ao enviar transação para Kafka", e);
            throw new RuntimeException("Erro ao enviar transação para Kafka: " + e.getMessage(), e);
        }
    }
}

