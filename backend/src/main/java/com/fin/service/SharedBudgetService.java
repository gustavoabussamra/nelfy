package com.fin.service;

import com.fin.dto.SharedBudgetDto;
import com.fin.model.Budget;
import com.fin.model.SharedBudget;
import com.fin.model.User;
import com.fin.repository.BudgetRepository;
import com.fin.repository.SharedBudgetRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SharedBudgetService {
    
    @Autowired
    private SharedBudgetRepository sharedBudgetRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PlanLimitsService planLimitsService;
    
    /**
     * Compartilha um orçamento com outro usuário
     */
    @Transactional
    public SharedBudgetDto shareBudget(Long budgetId, Long sharedUserId, String permission, Long ownerId) {
        // Verificar se o dono tem plano que permite colaboração
        if (!planLimitsService.getPlanLimits(ownerId).canCollaborate()) {
            throw new RuntimeException("Seu plano não permite compartilhamento. Faça upgrade para ENTERPRISE.");
        }
        
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));
        
        if (!budget.getUser().getId().equals(ownerId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        User sharedUser = userRepository.findById(sharedUserId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Verificar se já foi compartilhado
        if (sharedBudgetRepository.existsByBudgetIdAndSharedUserId(budgetId, sharedUserId)) {
            throw new RuntimeException("Orçamento já foi compartilhado com este usuário");
        }
        
        SharedBudget sharedBudget = new SharedBudget();
        sharedBudget.setBudget(budget);
        sharedBudget.setOwner(budget.getUser());
        sharedBudget.setSharedUser(sharedUser);
        sharedBudget.setPermission(SharedBudget.PermissionType.valueOf(permission));
        
        sharedBudget = sharedBudgetRepository.save(sharedBudget);
        return convertToDto(sharedBudget);
    }
    
    /**
     * Lista orçamentos compartilhados com o usuário
     */
    public List<SharedBudgetDto> getSharedBudgetsWithMe(Long userId) {
        List<SharedBudget> shared = sharedBudgetRepository.findBySharedUserId(userId);
        return shared.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Lista orçamentos que o usuário compartilhou
     */
    public List<SharedBudgetDto> getBudgetsIShared(Long userId) {
        List<SharedBudget> shared = sharedBudgetRepository.findByOwnerId(userId);
        return shared.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Remove compartilhamento
     */
    @Transactional
    public void unshareBudget(Long sharedBudgetId, Long userId) {
        SharedBudget sharedBudget = sharedBudgetRepository.findById(sharedBudgetId)
                .orElseThrow(() -> new RuntimeException("Compartilhamento não encontrado"));
        
        if (!sharedBudget.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        sharedBudgetRepository.delete(sharedBudget);
    }
    
    /**
     * Verifica se usuário tem permissão em um orçamento
     */
    public boolean hasPermission(Long budgetId, Long userId) {
        // Verificar se é o dono
        Budget budget = budgetRepository.findById(budgetId).orElse(null);
        if (budget != null && budget.getUser().getId().equals(userId)) {
            return true;
        }
        
        // Verificar se tem compartilhamento
        return sharedBudgetRepository.existsByBudgetIdAndSharedUserId(budgetId, userId);
    }
    
    private SharedBudgetDto convertToDto(SharedBudget sharedBudget) {
        SharedBudgetDto dto = new SharedBudgetDto();
        dto.setId(sharedBudget.getId());
        dto.setBudgetId(sharedBudget.getBudget().getId());
        dto.setOwnerId(sharedBudget.getOwner().getId());
        dto.setOwnerName(sharedBudget.getOwner().getName());
        dto.setSharedUserId(sharedBudget.getSharedUser().getId());
        dto.setSharedUserName(sharedBudget.getSharedUser().getName());
        dto.setPermission(sharedBudget.getPermission().name());
        dto.setCreatedAt(sharedBudget.getCreatedAt());
        
        // Incluir dados básicos do orçamento
        com.fin.dto.BudgetDto budgetDto = new com.fin.dto.BudgetDto();
        budgetDto.setId(sharedBudget.getBudget().getId());
        budgetDto.setName(sharedBudget.getBudget().getName());
        budgetDto.setLimitAmount(sharedBudget.getBudget().getLimitAmount());
        budgetDto.setStartDate(sharedBudget.getBudget().getStartDate());
        budgetDto.setEndDate(sharedBudget.getBudget().getEndDate());
        dto.setBudget(budgetDto);
        
        return dto;
    }
}

