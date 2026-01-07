package com.fin.service;

import com.fin.model.Subscription;
import com.fin.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanLimitsService {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private TransactionAttachmentRepository attachmentRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    /**
     * Verifica limites do plano FREE
     */
    public PlanLimits getPlanLimits(Long userId) {
        if (!subscriptionService.isSubscriptionActive(userId)) {
            return new PlanLimits(false, 0, 0, 0, 0, 0, 0, false, false, false);
        }
        
        Subscription subscription = subscriptionService.getUserSubscriptionEntity(userId);
        Subscription.SubscriptionPlan plan = subscription.getPlan();
        
        switch (plan) {
            case FREE:
                return new PlanLimits(
                    true,
                    Integer.MAX_VALUE, // maxTransactions (ilimitado)
                    3,       // maxCategories
                    1,       // maxAccounts
                    0,       // maxAttachments (0 = sem anexos)
                    3,       // maxGoals
                    1,       // maxBudgets
                    false,   // canExportExcel
                    false,   // canUseAI
                    false    // canCollaborate
                );
            case BASIC:
                return new PlanLimits(
                    true,
                    Integer.MAX_VALUE, // transações ilimitadas
                    Integer.MAX_VALUE, // categorias ilimitadas
                    3,                 // 3 contas
                    10,                // 10 anexos
                    Integer.MAX_VALUE, // metas ilimitadas
                    Integer.MAX_VALUE, // orçamentos ilimitados
                    true,              // pode exportar Excel
                    false,              // sem IA
                    false               // sem colaboração
                );
            case PREMIUM:
                return new PlanLimits(
                    true,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE, // contas ilimitadas
                    Integer.MAX_VALUE, // anexos ilimitados
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    true,
                    true,              // IA habilitada
                    false
                );
            case ENTERPRISE:
                return new PlanLimits(
                    true,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE,
                    true,
                    true,
                    true               // colaboração habilitada
                );
            default:
                return new PlanLimits(false, 0, 0, 0, 0, 0, 0, false, false, false);
        }
    }
    
    /**
     * Verifica se pode criar transação
     */
    public boolean canCreateTransaction(Long userId) {
        PlanLimits limits = getPlanLimits(userId);
        if (!limits.isActive()) return false;
        
        if (limits.getMaxTransactions() == Integer.MAX_VALUE) return true;
        
        long transactionCount = transactionRepository.findByUserId(userId).size();
        return transactionCount < limits.getMaxTransactions();
    }
    
    /**
     * Verifica se pode criar categoria
     */
    public boolean canCreateCategory(Long userId) {
        PlanLimits limits = getPlanLimits(userId);
        if (!limits.isActive()) return false;
        
        if (limits.getMaxCategories() == Integer.MAX_VALUE) return true;
        
        long categoryCount = categoryRepository.findByUserId(userId).size();
        return categoryCount < limits.getMaxCategories();
    }
    
    /**
     * Verifica se pode criar conta
     */
    public boolean canCreateAccount(Long userId) {
        PlanLimits limits = getPlanLimits(userId);
        if (!limits.isActive()) return false;
        
        if (limits.getMaxAccounts() == Integer.MAX_VALUE) return true;
        
        long accountCount = accountRepository.findByUserIdAndIsActiveTrue(userId).size();
        return accountCount < limits.getMaxAccounts();
    }
    
    /**
     * Verifica se pode fazer upload de anexo
     */
    public boolean canUploadAttachment(Long userId) {
        PlanLimits limits = getPlanLimits(userId);
        if (!limits.isActive()) return false;
        
        if (limits.getMaxAttachments() == Integer.MAX_VALUE) return true;
        
        long attachmentCount = attachmentRepository.findByUserId(userId).size();
        return attachmentCount < limits.getMaxAttachments();
    }
    
    /**
     * Verifica se pode criar meta
     */
    public boolean canCreateGoal(Long userId) {
        PlanLimits limits = getPlanLimits(userId);
        if (!limits.isActive()) return false;
        
        if (limits.getMaxGoals() == Integer.MAX_VALUE) return true;
        
        long goalCount = goalRepository.findByUserId(userId).size();
        return goalCount < limits.getMaxGoals();
    }
    
    /**
     * Verifica se pode criar orçamento
     */
    public boolean canCreateBudget(Long userId) {
        PlanLimits limits = getPlanLimits(userId);
        if (!limits.isActive()) return false;
        
        if (limits.getMaxBudgets() == Integer.MAX_VALUE) return true;
        
        long budgetCount = budgetRepository.findByUserId(userId).size();
        return budgetCount < limits.getMaxBudgets();
    }
    
    /**
     * Classe interna para representar limites
     */
    public static class PlanLimits {
        private boolean isActive;
        private int maxTransactions;
        private int maxCategories;
        private int maxAccounts;
        private int maxAttachments;
        private int maxGoals;
        private int maxBudgets;
        private boolean canExportExcel;
        private boolean canUseAI;
        private boolean canCollaborate;
        
        public PlanLimits(boolean isActive, int maxTransactions, int maxCategories, 
                         int maxAccounts, int maxAttachments, int maxGoals, int maxBudgets,
                         boolean canExportExcel, boolean canUseAI, boolean canCollaborate) {
            this.isActive = isActive;
            this.maxTransactions = maxTransactions;
            this.maxCategories = maxCategories;
            this.maxAccounts = maxAccounts;
            this.maxAttachments = maxAttachments;
            this.maxGoals = maxGoals;
            this.maxBudgets = maxBudgets;
            this.canExportExcel = canExportExcel;
            this.canUseAI = canUseAI;
            this.canCollaborate = canCollaborate;
        }
        
        // Getters
        public boolean isActive() { return isActive; }
        public int getMaxTransactions() { return maxTransactions; }
        public int getMaxCategories() { return maxCategories; }
        public int getMaxAccounts() { return maxAccounts; }
        public int getMaxAttachments() { return maxAttachments; }
        public int getMaxGoals() { return maxGoals; }
        public int getMaxBudgets() { return maxBudgets; }
        public boolean canExportExcel() { return canExportExcel; }
        public boolean canUseAI() { return canUseAI; }
        public boolean canCollaborate() { return canCollaborate; }
    }
}

