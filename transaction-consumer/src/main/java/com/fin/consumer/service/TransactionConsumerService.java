package com.fin.consumer.service;

import com.fin.consumer.dto.TransactionKafkaMessage;
import com.fin.consumer.model.Category;
import com.fin.consumer.model.Transaction;
import com.fin.consumer.model.User;
import com.fin.consumer.model.Installment;
import com.fin.consumer.repository.CategoryRepository;
import com.fin.consumer.repository.TransactionRepository;
import com.fin.consumer.repository.UserRepository;
import com.fin.consumer.repository.InstallmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumerService.class);
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private AutomationRuleService automationRuleService;
    
    @KafkaListener(topics = "${kafka.topic.transactions:transactions}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeTransaction(
            @Payload TransactionKafkaMessage message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("=== PROCESSANDO MENSAGEM DO KAFKA ===");
            logger.info("Partition: {}, Offset: {}, Key: {}", partition, offset, key);
            logger.info("Operation: {}, UserId: {}", message.getOperation(), message.getUserId());
            
            TransactionKafkaMessage.TransactionDto dto = message.getTransaction();
            Long userId = message.getUserId();
            
            if (!"CREATE".equals(message.getOperation())) {
                logger.warn("Operação não suportada: {}", message.getOperation());
                acknowledgment.acknowledge();
                return;
            }
            
            // Buscar usuário
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + userId));
            
            // Criar transação
            Transaction transaction = new Transaction();
            transaction.setDescription(dto.getDescription());
            transaction.setAmount(dto.getAmount());
            transaction.setType(Transaction.TransactionType.valueOf(dto.getType()));
            transaction.setUser(user);
            transaction.setDueDate(dto.getDueDate());
            transaction.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : dto.getDueDate());
            transaction.setIsPaid(dto.getIsPaid() != null ? dto.getIsPaid() : false);
            
            // Definir categoria
            if (dto.getCategory() != null && dto.getCategory().getId() != null) {
                Category category = categoryRepository.findById(dto.getCategory().getId())
                        .orElse(null);
                if (category != null) {
                    transaction.setCategory(category);
                }
            }
            
            // Se for parcelada, criar parcelas
            if (dto.getTotalInstallments() != null && dto.getTotalInstallments() > 1) {
                transaction.setIsInstallment(true);
                transaction.setTotalInstallments(dto.getTotalInstallments());
                transaction.setInstallmentNumber(0); // Transação pai
                transaction.setIsPaid(false);
                transaction.setAmount(BigDecimal.ZERO); // Valor da transação pai é zero
                
                // Salvar transação pai
                Transaction savedParent = transactionRepository.save(transaction);
                transactionRepository.flush();
                
                // Criar parcelas
                createInstallments(savedParent, dto.getAmount(), dto.getTotalInstallments(), dto.getDueDate());
                
                // Aplicar regras de automação nas parcelas criadas
                try {
                    List<Transaction> createdInstallments = transactionRepository.findByParentTransactionId(savedParent.getId());
                    for (Transaction installment : createdInstallments) {
                        automationRuleService.applyRulesToTransaction(userId, installment);
                    }
                } catch (Exception e) {
                    logger.warn("Erro ao aplicar regras de automação nas parcelas: {}", e.getMessage());
                }
                
                logger.info("Transação parcelada criada: ID={}, Total de parcelas: {}", 
                    savedParent.getId(), dto.getTotalInstallments());
            } else {
                transaction.setIsInstallment(false);
                transaction.setTotalInstallments(1);
                transaction = transactionRepository.save(transaction);
                
                // Aplicar regras de automação
                try {
                    automationRuleService.applyRulesToTransaction(userId, transaction);
                    // Recarregar a transação caso tenha sido modificada pelas regras
                    transaction = transactionRepository.findById(transaction.getId()).orElse(transaction);
                } catch (Exception e) {
                    logger.warn("Erro ao aplicar regras de automação: {}", e.getMessage());
                }
                
                logger.info("Transação criada: ID={}, Description: {}", 
                    transaction.getId(), transaction.getDescription());
            }
            
            // Confirmar processamento da mensagem
            acknowledgment.acknowledge();
            logger.info("Mensagem processada com sucesso - Offset: {}", offset);
            
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem do Kafka - Offset: {}", offset, e);
            // Não fazer acknowledge para reprocessar a mensagem
            // Em produção, implementar dead letter queue
            throw new RuntimeException("Erro ao processar transação: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    private void createInstallments(Transaction parentTransaction, BigDecimal installmentAmount, 
                                   Integer totalInstallments, LocalDate startDate) {
        logger.info("Criando {} parcelas de R$ {} cada, começando em {}", 
            totalInstallments, installmentAmount, startDate);
        
        List<Installment> installments = new ArrayList<>();
        List<Transaction> installmentTransactions = new ArrayList<>();
        
        for (int i = 1; i <= totalInstallments; i++) {
            // Calcular data da parcela
            LocalDate installmentDate = startDate.plusMonths(i - 1);
            
            // Criar transação para a parcela
            Transaction installmentTransaction = new Transaction();
            installmentTransaction.setDescription(parentTransaction.getDescription());
            installmentTransaction.setAmount(installmentAmount);
            installmentTransaction.setType(parentTransaction.getType());
            installmentTransaction.setUser(parentTransaction.getUser());
            installmentTransaction.setCategory(parentTransaction.getCategory());
            installmentTransaction.setDueDate(installmentDate);
            installmentTransaction.setTransactionDate(installmentDate);
            installmentTransaction.setIsInstallment(false); // Parcelas individuais não são installment
            installmentTransaction.setParentTransactionId(parentTransaction.getId());
            installmentTransaction.setInstallmentNumber(i);
            installmentTransaction.setTotalInstallments(totalInstallments);
            installmentTransaction.setIsPaid(false); // Parcelas começam como não pagas
            
            installmentTransactions.add(installmentTransaction);
        }
        
        // Salvar todas as parcelas
        for (Transaction installmentTransaction : installmentTransactions) {
            Transaction saved = transactionRepository.save(installmentTransaction);
            transactionRepository.flush();
            
            // Criar registro na tabela installments
            Installment installment = new Installment();
            installment.setTransaction(saved);
            installment.setInstallmentNumber(saved.getInstallmentNumber());
            installment.setTotalInstallments(saved.getTotalInstallments());
            installment.setAmount(saved.getAmount());
            installment.setDueDate(saved.getDueDate());
            installment.setIsPaid(false);
            
            installments.add(installment);
        }
        
        // Salvar todos os registros de installments
        installmentRepository.saveAll(installments);
        installmentRepository.flush();
        
        logger.info("Criadas {} parcelas com sucesso", totalInstallments);
    }
}





