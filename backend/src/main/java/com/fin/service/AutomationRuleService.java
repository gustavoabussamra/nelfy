package com.fin.service;

import com.fin.dto.AutomationRuleDto;
import com.fin.model.AutomationRule;
import com.fin.model.Category;
import com.fin.model.Transaction;
import com.fin.model.User;
import com.fin.repository.AutomationRuleRepository;
import com.fin.repository.CategoryRepository;
import com.fin.repository.TransactionRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AutomationRuleService {
    
    @Autowired
    private AutomationRuleRepository automationRuleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    public List<AutomationRuleDto> getUserRules(Long userId) {
        return automationRuleRepository.findByUserIdOrderByPriorityDesc(userId).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    public AutomationRuleDto getRule(Long ruleId, Long userId) {
        AutomationRule rule = automationRuleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Regra não encontrada"));
        
        if (!rule.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        return convertToDto(rule);
    }
    
    @Transactional
    public AutomationRuleDto createRule(Long userId, AutomationRuleDto dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        AutomationRule rule = new AutomationRule();
        rule.setUser(user);
        rule.setName(dto.getName());
        rule.setDescription(dto.getDescription());
        rule.setConditionType(dto.getConditionType());
        rule.setConditionValue(dto.getConditionValue());
        rule.setActionType(dto.getActionType());
        rule.setActionValue(dto.getActionValue());
        rule.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        
        rule = automationRuleRepository.save(rule);
        return convertToDto(rule);
    }
    
    @Transactional
    public AutomationRuleDto updateRule(Long ruleId, Long userId, AutomationRuleDto dto) {
        AutomationRule rule = automationRuleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Regra não encontrada"));
        
        if (!rule.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        rule.setName(dto.getName());
        rule.setDescription(dto.getDescription());
        rule.setConditionType(dto.getConditionType());
        rule.setConditionValue(dto.getConditionValue());
        rule.setActionType(dto.getActionType());
        rule.setActionValue(dto.getActionValue());
        rule.setIsActive(dto.getIsActive());
        rule.setPriority(dto.getPriority());
        
        rule = automationRuleRepository.save(rule);
        return convertToDto(rule);
    }
    
    @Transactional
    public void deleteRule(Long ruleId, Long userId) {
        AutomationRule rule = automationRuleRepository.findById(ruleId)
            .orElseThrow(() -> new RuntimeException("Regra não encontrada"));
        
        if (!rule.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        automationRuleRepository.delete(rule);
    }
    
    /**
     * Aplica regras de automação a uma transação
     */
    @Transactional
    public void applyRulesToTransaction(Long userId, Transaction transaction) {
        List<AutomationRule> rules = automationRuleRepository.findByUserIdAndIsActiveTrueOrderByPriorityDesc(userId);
        
        for (AutomationRule rule : rules) {
            if (matchesCondition(rule, transaction)) {
                executeAction(rule, transaction);
                rule.setExecutionCount(rule.getExecutionCount() + 1);
                rule.setLastExecution(LocalDateTime.now());
                automationRuleRepository.save(rule);
                break; // Apenas a primeira regra que corresponder é executada
            }
        }
    }
    
    private boolean matchesCondition(AutomationRule rule, Transaction transaction) {
        switch (rule.getConditionType()) {
            case "DESCRIPTION_CONTAINS":
                String keyword = rule.getConditionValue().toLowerCase();
                return transaction.getDescription() != null &&
                       transaction.getDescription().toLowerCase().contains(keyword);
            
            case "AMOUNT_RANGE":
                // Formato: "min:max" ou "min:" ou ":max"
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
                // Verifica se a descrição contém o nome do estabelecimento
                String merchant = rule.getConditionValue().toLowerCase();
                return transaction.getDescription() != null &&
                       transaction.getDescription().toLowerCase().contains(merchant);
            
            case "DATE_PATTERN":
                // Por enquanto, apenas verifica se é do mesmo dia da semana
                // Pode ser expandido para padrões mais complexos
                return true; // Simplificado
            
            default:
                return false;
        }
    }
    
    private void executeAction(AutomationRule rule, Transaction transaction) {
        switch (rule.getActionType()) {
            case "AUTO_CATEGORIZE":
                try {
                    Long categoryId = Long.parseLong(rule.getActionValue());
                    Category category = categoryRepository.findById(categoryId)
                        .orElse(null);
                    if (category != null && category.getUser().getId().equals(transaction.getUser().getId())) {
                        transaction.setCategory(category);
                        transactionRepository.save(transaction);
                    }
                } catch (NumberFormatException e) {
                    // Ignorar se não for um ID válido
                }
                break;
            
            case "AUTO_APPROVE":
                transaction.setIsPaid(true);
                transactionRepository.save(transaction);
                break;
            
            case "SEND_ALERT":
                // Será implementado com NotificationService
                break;
        }
    }
    
    private AutomationRuleDto convertToDto(AutomationRule rule) {
        AutomationRuleDto dto = new AutomationRuleDto();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setDescription(rule.getDescription());
        dto.setConditionType(rule.getConditionType());
        dto.setConditionValue(rule.getConditionValue());
        dto.setActionType(rule.getActionType());
        dto.setActionValue(rule.getActionValue());
        dto.setIsActive(rule.getIsActive());
        dto.setPriority(rule.getPriority());
        dto.setExecutionCount(rule.getExecutionCount());
        dto.setLastExecution(rule.getLastExecution());
        dto.setCreatedAt(rule.getCreatedAt());
        return dto;
    }
}




