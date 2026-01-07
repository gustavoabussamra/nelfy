package com.fin.service;

import com.fin.dto.RecurringTransactionDto;
import com.fin.model.*;
import com.fin.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecurringTransactionService {
    
    @Autowired
    private RecurringTransactionRepository recurringRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    /**
     * Lista todas as recorrências do usuário
     */
    public List<RecurringTransactionDto> getUserRecurringTransactions(Long userId) {
        List<RecurringTransaction> recurring = recurringRepository.findByUserIdAndIsActiveTrue(userId);
        return recurring.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Cria uma nova recorrência
     */
    @Transactional
    public RecurringTransactionDto createRecurringTransaction(RecurringTransactionDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        RecurringTransaction recurring = new RecurringTransaction();
        recurring.setDescription(dto.getDescription());
        recurring.setAmount(dto.getAmount());
        recurring.setType(RecurringTransaction.TransactionType.valueOf(dto.getType()));
        recurring.setRecurrenceType(RecurringTransaction.RecurrenceType.valueOf(dto.getRecurrenceType()));
        recurring.setRecurrenceDay(dto.getRecurrenceDay());
        recurring.setStartDate(dto.getStartDate());
        recurring.setEndDate(dto.getEndDate());
        recurring.setIsActive(true);
        recurring.setAutoCreate(dto.getAutoCreate() != null ? dto.getAutoCreate() : false);
        recurring.setUser(user);
        
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            recurring.setCategory(category);
        }
        
        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId()).orElse(null);
            recurring.setAccount(account);
        }
        
        // Calcular próxima ocorrência
        recurring.setNextOccurrenceDate(calculateNextOccurrence(recurring));
        
        recurring = recurringRepository.save(recurring);
        return convertToDto(recurring);
    }
    
    /**
     * Atualiza uma recorrência
     */
    @Transactional
    public RecurringTransactionDto updateRecurringTransaction(Long id, RecurringTransactionDto dto, Long userId) {
        RecurringTransaction recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recorrência não encontrada"));
        
        if (!recurring.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        recurring.setDescription(dto.getDescription());
        recurring.setAmount(dto.getAmount());
        recurring.setType(RecurringTransaction.TransactionType.valueOf(dto.getType()));
        recurring.setRecurrenceType(RecurringTransaction.RecurrenceType.valueOf(dto.getRecurrenceType()));
        recurring.setRecurrenceDay(dto.getRecurrenceDay());
        recurring.setStartDate(dto.getStartDate());
        recurring.setEndDate(dto.getEndDate());
        recurring.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : recurring.getIsActive());
        recurring.setAutoCreate(dto.getAutoCreate() != null ? dto.getAutoCreate() : recurring.getAutoCreate());
        
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            recurring.setCategory(category);
        }
        
        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId()).orElse(null);
            recurring.setAccount(account);
        }
        
        recurring.setNextOccurrenceDate(calculateNextOccurrence(recurring));
        
        recurring = recurringRepository.save(recurring);
        return convertToDto(recurring);
    }
    
    /**
     * Deleta uma recorrência
     */
    @Transactional
    public void deleteRecurringTransaction(Long id, Long userId) {
        RecurringTransaction recurring = recurringRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recorrência não encontrada"));
        
        if (!recurring.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        recurring.setIsActive(false);
        recurringRepository.save(recurring);
    }
    
    /**
     * Processa recorrências automaticamente (executado diariamente)
     */
    @Scheduled(cron = "0 0 1 * * *") // Executa todo dia à 1h da manhã
    @Transactional
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        
        // Buscar todos os usuários ativos
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            if (!subscriptionService.isSubscriptionActive(user.getId())) {
                continue; // Pular usuários sem assinatura ativa
            }
            
            List<RecurringTransaction> toProcess = recurringRepository.findRecurringTransactionsToProcess(user.getId(), today);
            
            for (RecurringTransaction recurring : toProcess) {
                if (shouldCreateTransaction(recurring, today)) {
                    createTransactionFromRecurring(recurring, today);
                }
            }
        }
    }
    
    /**
     * Verifica se deve criar transação para esta recorrência
     */
    private boolean shouldCreateTransaction(RecurringTransaction recurring, LocalDate today) {
        if (recurring.getNextOccurrenceDate() == null) {
            return false;
        }
        
        if (!recurring.getNextOccurrenceDate().isBefore(today) && !recurring.getNextOccurrenceDate().equals(today)) {
            return false;
        }
        
        if (recurring.getEndDate() != null && today.isAfter(recurring.getEndDate())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Cria uma transação a partir de uma recorrência
     */
    @Transactional
    private void createTransactionFromRecurring(RecurringTransaction recurring, LocalDate occurrenceDate) {
        Transaction transaction = new Transaction();
        transaction.setDescription(recurring.getDescription());
        transaction.setAmount(recurring.getAmount());
        transaction.setType(Transaction.TransactionType.valueOf(recurring.getType().name()));
        transaction.setTransactionDate(occurrenceDate);
        transaction.setDueDate(occurrenceDate);
        transaction.setUser(recurring.getUser());
        transaction.setCategory(recurring.getCategory());
        transaction.setAccount(recurring.getAccount());
        transaction.setIsPaid(false);
        transaction.setIsInstallment(false);
        
        transactionRepository.save(transaction);
        
        // Atualizar recorrência
        recurring.setCreatedCount(recurring.getCreatedCount() + 1);
        recurring.setLastCreatedDate(occurrenceDate);
        recurring.setNextOccurrenceDate(calculateNextOccurrence(recurring));
        recurringRepository.save(recurring);
    }
    
    /**
     * Calcula a próxima ocorrência baseada no tipo de recorrência
     */
    private LocalDate calculateNextOccurrence(RecurringTransaction recurring) {
        LocalDate baseDate = recurring.getLastCreatedDate() != null 
            ? recurring.getLastCreatedDate() 
            : recurring.getStartDate();
        
        LocalDate today = LocalDate.now();
        LocalDate nextDate = baseDate;
        
        switch (recurring.getRecurrenceType()) {
            case DAILY:
                nextDate = baseDate.plusDays(1);
                break;
            case WEEKLY:
                nextDate = baseDate.plusWeeks(1);
                if (recurring.getRecurrenceDay() != null) {
                    // Ajustar para o dia da semana específico
                    DayOfWeek targetDay = DayOfWeek.of(recurring.getRecurrenceDay());
                    while (nextDate.getDayOfWeek() != targetDay) {
                        nextDate = nextDate.plusDays(1);
                    }
                }
                break;
            case MONTHLY:
                nextDate = baseDate.plusMonths(1);
                if (recurring.getRecurrenceDay() != null) {
                    // Ajustar para o dia do mês específico
                    int day = Math.min(recurring.getRecurrenceDay(), nextDate.lengthOfMonth());
                    nextDate = nextDate.withDayOfMonth(day);
                }
                break;
            case YEARLY:
                nextDate = baseDate.plusYears(1);
                break;
        }
        
        // Garantir que a próxima data não seja no passado
        if (nextDate.isBefore(today)) {
            return calculateNextOccurrenceFromToday(recurring, today);
        }
        
        return nextDate;
    }
    
    /**
     * Calcula próxima ocorrência a partir de hoje
     */
    private LocalDate calculateNextOccurrenceFromToday(RecurringTransaction recurring, LocalDate today) {
        LocalDate nextDate = today;
        
        switch (recurring.getRecurrenceType()) {
            case DAILY:
                nextDate = today.plusDays(1);
                break;
            case WEEKLY:
                nextDate = today.plusWeeks(1);
                if (recurring.getRecurrenceDay() != null) {
                    DayOfWeek targetDay = DayOfWeek.of(recurring.getRecurrenceDay());
                    while (nextDate.getDayOfWeek() != targetDay) {
                        nextDate = nextDate.plusDays(1);
                    }
                }
                break;
            case MONTHLY:
                nextDate = today.plusMonths(1);
                if (recurring.getRecurrenceDay() != null) {
                    int day = Math.min(recurring.getRecurrenceDay(), nextDate.lengthOfMonth());
                    nextDate = nextDate.withDayOfMonth(day);
                }
                break;
            case YEARLY:
                nextDate = today.plusYears(1);
                break;
        }
        
        return nextDate;
    }
    
    private RecurringTransactionDto convertToDto(RecurringTransaction recurring) {
        RecurringTransactionDto dto = new RecurringTransactionDto();
        dto.setId(recurring.getId());
        dto.setDescription(recurring.getDescription());
        dto.setAmount(recurring.getAmount());
        dto.setType(recurring.getType().name());
        dto.setRecurrenceType(recurring.getRecurrenceType().name());
        dto.setRecurrenceDay(recurring.getRecurrenceDay());
        dto.setStartDate(recurring.getStartDate());
        dto.setEndDate(recurring.getEndDate());
        dto.setNextOccurrenceDate(recurring.getNextOccurrenceDate());
        dto.setIsActive(recurring.getIsActive());
        dto.setAutoCreate(recurring.getAutoCreate());
        dto.setCreatedCount(recurring.getCreatedCount());
        dto.setLastCreatedDate(recurring.getLastCreatedDate());
        dto.setCreatedAt(recurring.getCreatedAt());
        dto.setUpdatedAt(recurring.getUpdatedAt());
        
        if (recurring.getCategory() != null) {
            dto.setCategoryId(recurring.getCategory().getId());
            com.fin.dto.CategoryDto categoryDto = new com.fin.dto.CategoryDto();
            categoryDto.setId(recurring.getCategory().getId());
            categoryDto.setName(recurring.getCategory().getName());
            categoryDto.setIcon(recurring.getCategory().getIcon());
            categoryDto.setColor(recurring.getCategory().getColor());
            categoryDto.setType(recurring.getCategory().getType().name());
            dto.setCategory(categoryDto);
        }
        
        if (recurring.getAccount() != null) {
            dto.setAccountId(recurring.getAccount().getId());
            com.fin.dto.AccountDto accountDto = new com.fin.dto.AccountDto();
            accountDto.setId(recurring.getAccount().getId());
            accountDto.setName(recurring.getAccount().getName());
            accountDto.setType(recurring.getAccount().getType().name());
            accountDto.setBalance(recurring.getAccount().getBalance());
            dto.setAccount(accountDto);
        }
        
        return dto;
    }
}

