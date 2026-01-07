package com.fin.consumer.service;

import com.fin.consumer.model.AutomationRule;
import com.fin.consumer.model.Category;
import com.fin.consumer.model.Transaction;
import com.fin.consumer.repository.AutomationRuleRepository;
import com.fin.consumer.repository.CategoryRepository;
import com.fin.consumer.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutomationRuleService {
    
    private static final Logger logger = LoggerFactory.getLogger(AutomationRuleService.class);
    
    @Autowired
    private AutomationRuleRepository automationRuleRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    /**
     * Aplica regras de automação a uma transação
     */
    @Transactional
    public void applyRulesToTransaction(Long userId, Transaction transaction) {
        try {
            List<AutomationRule> rules = automationRuleRepository.findByUserIdAndIsActiveTrueOrderByPriorityDesc(userId);
            
            for (AutomationRule rule : rules) {
                if (matchesCondition(rule, transaction)) {
                    executeAction(rule, transaction);
                    rule.setExecutionCount(rule.getExecutionCount() + 1);
                    rule.setLastExecution(LocalDateTime.now());
                    automationRuleRepository.save(rule);
                    logger.info("Regra de automação '{}' aplicada à transação {}", rule.getName(), transaction.getId());
                    break; // Apenas a primeira regra que corresponder é executada
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao aplicar regras de automação: {}", e.getMessage());
            // Não lançar exceção para não quebrar o processamento da transação
        }
    }
    
    private boolean matchesCondition(AutomationRule rule, Transaction transaction) {
        switch (rule.getConditionType()) {
            case "DESCRIPTION_CONTAINS":
                String keyword = rule.getConditionValue().toLowerCase();
                return transaction.getDescription() != null &&
                       transaction.getDescription().toLowerCase().contains(keyword);
            
            case "AMOUNT_RANGE":
                String[] range = rule.getConditionValue().split(":");
                BigDecimal amount = transaction.getAmount();
                if (range.length == 2) {
                    BigDecimal min = range[0].isEmpty() ? null : new BigDecimal(range[0]);
                    BigDecimal max = range[1].isEmpty() ? null : new BigDecimal(range[1]);
                    if (min != null && amount.compareTo(min) < 0) return false;
                    if (max != null && amount.compareTo(max) > 0) return false;
                    return true;
                }
                return false;
            
            case "MERCHANT":
                String merchant = rule.getConditionValue().toLowerCase();
                return transaction.getDescription() != null &&
                       transaction.getDescription().toLowerCase().contains(merchant);
            
            default:
                return false;
        }
    }
    
    private void executeAction(AutomationRule rule, Transaction transaction) {
        switch (rule.getActionType()) {
            case "AUTO_CATEGORIZE":
                try {
                    Long categoryId = Long.parseLong(rule.getActionValue());
                    Category category = categoryRepository.findById(categoryId).orElse(null);
                    if (category != null && category.getUser().getId().equals(transaction.getUser().getId())) {
                        transaction.setCategory(category);
                        transactionRepository.save(transaction);
                        logger.info("Categoria '{}' aplicada automaticamente à transação {}", category.getName(), transaction.getId());
                    }
                } catch (NumberFormatException e) {
                    logger.warn("ID de categoria inválido na regra: {}", rule.getActionValue());
                }
                break;
            
            case "AUTO_APPROVE":
                transaction.setIsPaid(true);
                transactionRepository.save(transaction);
                logger.info("Transação {} marcada como paga automaticamente", transaction.getId());
                break;
        }
    }
}




