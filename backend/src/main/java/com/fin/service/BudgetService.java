package com.fin.service;

import com.fin.dto.BudgetDto;
import com.fin.dto.CategoryDto;
import com.fin.model.Budget;
import com.fin.model.Category;
import com.fin.model.Transaction;
import com.fin.model.User;
import com.fin.repository.BudgetRepository;
import com.fin.repository.CategoryRepository;
import com.fin.repository.TransactionRepository;
import com.fin.repository.UserRepository;
import com.fin.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fin.dto.CashFlowForecastDto;
import com.fin.dto.CashFlowDayDto;
import com.fin.dto.TransactionForecastDto;
import com.fin.dto.ScenarioDto;
import com.fin.model.RecurringTransaction;
import com.fin.repository.RecurringTransactionRepository;
import com.fin.repository.AccountRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BudgetService {
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private RecurringTransactionRepository recurringTransactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    public List<BudgetDto> getUserBudgets(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        return budgetRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public BudgetDto getBudget(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));
        
        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        return convertToDto(budget);
    }
    
    @Transactional
    public BudgetDto createBudget(BudgetDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        Budget budget = new Budget();
        budget.setName(dto.getName());
        budget.setLimitAmount(dto.getLimitAmount());
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
        budget.setUser(user);
        budget.setAlertPercentage(dto.getAlertPercentage() != null ? dto.getAlertPercentage() : 80);
        budget.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        if (dto.getCategory() != null && dto.getCategory().getId() != null) {
            Category category = categoryRepository.findById(dto.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            budget.setCategory(category);
        }
        
        budget = budgetRepository.save(budget);
        return convertToDto(budget);
    }
    
    @Transactional
    public BudgetDto updateBudget(Long id, BudgetDto dto, Long userId) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));
        
        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        budget.setName(dto.getName());
        budget.setLimitAmount(dto.getLimitAmount());
        budget.setStartDate(dto.getStartDate());
        budget.setEndDate(dto.getEndDate());
        budget.setAlertPercentage(dto.getAlertPercentage());
        budget.setIsActive(dto.getIsActive());
        
        if (dto.getCategory() != null && dto.getCategory().getId() != null) {
            Category category = categoryRepository.findById(dto.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            budget.setCategory(category);
        } else {
            budget.setCategory(null);
        }
        
        budget = budgetRepository.save(budget);
        return convertToDto(budget);
    }
    
    @Transactional
    public void deleteBudget(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));
        
        if (!budget.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        budgetRepository.delete(budget);
    }
    
    public List<BudgetDto> getActiveBudgetsWithAlerts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        LocalDate today = LocalDate.now();
        List<Budget> activeBudgets = budgetRepository.findActiveBudgetsByUserAndDate(user, today);
        
        return activeBudgets.stream()
                .map(this::convertToDto)
                .filter(budget -> budget.getAlertTriggered() != null && budget.getAlertTriggered())
                .collect(Collectors.toList());
    }
    
    private BudgetDto convertToDto(Budget budget) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setName(budget.getName());
        dto.setLimitAmount(budget.getLimitAmount());
        dto.setStartDate(budget.getStartDate());
        dto.setEndDate(budget.getEndDate());
        dto.setAlertPercentage(budget.getAlertPercentage());
        dto.setIsActive(budget.getIsActive());
        
        if (budget.getCategory() != null) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(budget.getCategory().getId());
            categoryDto.setName(budget.getCategory().getName());
            categoryDto.setIcon(budget.getCategory().getIcon());
            categoryDto.setColor(budget.getCategory().getColor());
            categoryDto.setType(budget.getCategory().getType().name());
            dto.setCategory(categoryDto);
        }
        
        // Calcular gasto atual
        BigDecimal currentSpent = BigDecimal.ZERO;
        if (budget.getCategory() != null) {
            List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                    budget.getUser().getId(),
                    budget.getStartDate(),
                    budget.getEndDate()
            );
            
            currentSpent = transactions.stream()
                    .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                    .filter(t -> budget.getCategory().getId().equals(t.getCategory() != null ? t.getCategory().getId() : null))
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            // Se não tem categoria, soma todas as despesas no período
            List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                    budget.getUser().getId(),
                    budget.getStartDate(),
                    budget.getEndDate()
            );
            
            currentSpent = transactions.stream()
                    .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        
        dto.setCurrentSpent(currentSpent);
        
        // Calcular valores restantes
        BigDecimal remaining = budget.getLimitAmount().subtract(currentSpent);
        dto.setRemaining(remaining);
        
        // Calcular porcentagem usada
        BigDecimal percentageUsed = BigDecimal.ZERO;
        if (budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
            percentageUsed = currentSpent
                    .divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        dto.setPercentageUsed(percentageUsed);
        
        // Verificar se alerta deve ser disparado
        boolean alertTriggered = percentageUsed.compareTo(BigDecimal.valueOf(budget.getAlertPercentage())) >= 0;
        dto.setAlertTriggered(alertTriggered);
        
        return dto;
    }
    
    /**
     * Previsão de fluxo de caixa para um período
     */
    public CashFlowForecastDto getCashFlowForecast(Long userId, LocalDate startDate, LocalDate endDate, Long accountId) {
        // Buscar transações existentes no período
        List<Transaction> existingTransactions = transactionRepository.findByUserIdAndTransactionDateBetween(
            userId, startDate, endDate
        );
        
        // Buscar recorrências que serão processadas no período
        List<RecurringTransaction> recurringTransactions = recurringTransactionRepository.findByUserIdAndIsActiveTrue(userId);
        
        // Calcular saldo inicial
        BigDecimal[] startingBalance = {BigDecimal.ZERO};
        if (accountId != null) {
            accountRepository.findById(accountId).ifPresent(account -> {
                startingBalance[0] = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
            });
        } else {
            // Soma de todas as contas
            startingBalance[0] = accountRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(account -> account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal currentBalance = startingBalance[0];
        
        // Gerar previsão diária
        List<CashFlowDayDto> dailyForecast = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal lowestBalance = currentBalance;
        LocalDate lowestBalanceDate = startDate;
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            BigDecimal dayIncome = BigDecimal.ZERO;
            BigDecimal dayExpenses = BigDecimal.ZERO;
            List<TransactionForecastDto> dayTransactions = new ArrayList<>();
            
            // Transações existentes neste dia
            for (Transaction t : existingTransactions) {
                if (t.getTransactionDate().equals(currentDate)) {
                    if (t.getType() == Transaction.TransactionType.INCOME) {
                        dayIncome = dayIncome.add(t.getAmount());
                    } else {
                        dayExpenses = dayExpenses.add(t.getAmount());
                    }
                    
                    TransactionForecastDto forecast = new TransactionForecastDto();
                    forecast.setDescription(t.getDescription());
                    forecast.setAmount(t.getAmount());
                    forecast.setType(t.getType().name());
                    forecast.setDate(currentDate);
                    forecast.setCategoryName(t.getCategory() != null ? t.getCategory().getName() : null);
                    forecast.setIsRecurring(false);
                    dayTransactions.add(forecast);
                }
            }
            
            // Recorrências que devem ocorrer neste dia
            for (RecurringTransaction rt : recurringTransactions) {
                if (rt.getNextOccurrenceDate() != null && rt.getNextOccurrenceDate().equals(currentDate)) {
                    BigDecimal amount = rt.getAmount();
                    if (rt.getType() == RecurringTransaction.TransactionType.INCOME) {
                        dayIncome = dayIncome.add(amount);
                    } else {
                        dayExpenses = dayExpenses.add(amount);
                    }
                    
                    TransactionForecastDto forecast = new TransactionForecastDto();
                    forecast.setDescription(rt.getDescription() + " (Recorrente)");
                    forecast.setAmount(amount);
                    forecast.setType(rt.getType().name());
                    forecast.setDate(currentDate);
                    forecast.setIsRecurring(true);
                    dayTransactions.add(forecast);
                }
            }
            
            BigDecimal netFlow = dayIncome.subtract(dayExpenses);
            currentBalance = currentBalance.add(netFlow);
            totalIncome = totalIncome.add(dayIncome);
            totalExpenses = totalExpenses.add(dayExpenses);
            
            if (currentBalance.compareTo(lowestBalance) < 0) {
                lowestBalance = currentBalance;
                lowestBalanceDate = currentDate;
            }
            
            CashFlowDayDto dayDto = new CashFlowDayDto();
            dayDto.setDate(currentDate);
            dayDto.setIncome(dayIncome);
            dayDto.setExpenses(dayExpenses);
            dayDto.setNetFlow(netFlow);
            dayDto.setBalance(currentBalance);
            dayDto.setTransactions(dayTransactions);
            dailyForecast.add(dayDto);
            
            currentDate = currentDate.plusDays(1);
        }
        
        CashFlowForecastDto forecast = new CashFlowForecastDto();
        forecast.setStartDate(startDate);
        forecast.setEndDate(endDate);
        forecast.setDailyForecast(dailyForecast);
        forecast.setTotalIncome(totalIncome);
        forecast.setTotalExpenses(totalExpenses);
        forecast.setNetFlow(totalIncome.subtract(totalExpenses));
        forecast.setStartingBalance(startingBalance[0]);
        forecast.setEndingBalance(currentBalance);
        forecast.setLowestBalance(lowestBalance);
        forecast.setLowestBalanceDate(lowestBalanceDate);
        
        return forecast;
    }
    
    /**
     * Simula cenário "e se" com ajustes percentuais
     */
    public ScenarioDto simulateScenario(Long userId, String scenarioName, String description,
                                       LocalDate startDate, LocalDate endDate,
                                       BigDecimal incomeAdjustment, BigDecimal expenseAdjustment,
                                       Long accountId) {
        // Obter previsão base
        CashFlowForecastDto baseForecast = getCashFlowForecast(userId, startDate, endDate, accountId);
        
        // Aplicar ajustes
        List<CashFlowDayDto> adjustedForecast = baseForecast.getDailyForecast().stream()
            .map(day -> {
                CashFlowDayDto adjusted = new CashFlowDayDto();
                adjusted.setDate(day.getDate());
                
                BigDecimal adjustedIncome = day.getIncome();
                if (incomeAdjustment != null) {
                    adjustedIncome = adjustedIncome.multiply(BigDecimal.ONE.add(incomeAdjustment));
                }
                
                BigDecimal adjustedExpenses = day.getExpenses();
                if (expenseAdjustment != null) {
                    adjustedExpenses = adjustedExpenses.multiply(BigDecimal.ONE.add(expenseAdjustment));
                }
                
                adjusted.setIncome(adjustedIncome);
                adjusted.setExpenses(adjustedExpenses);
                adjusted.setNetFlow(adjustedIncome.subtract(adjustedExpenses));
                
                adjusted.setTransactions(day.getTransactions());
                
                return adjusted;
            })
            .collect(Collectors.toList());
        
        // Recalcular saldos sequencialmente
        BigDecimal runningBalance = baseForecast.getStartingBalance();
        for (CashFlowDayDto day : adjustedForecast) {
            runningBalance = runningBalance.add(day.getNetFlow());
            day.setBalance(runningBalance);
        }
        
        // Recalcular totais
        BigDecimal totalIncome = adjustedForecast.stream()
            .map(CashFlowDayDto::getIncome)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalExpenses = adjustedForecast.stream()
            .map(CashFlowDayDto::getExpenses)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal endingBalance = adjustedForecast.isEmpty() 
            ? baseForecast.getStartingBalance()
            : adjustedForecast.get(adjustedForecast.size() - 1).getBalance();
        
        BigDecimal lowestBalance = adjustedForecast.stream()
            .map(CashFlowDayDto::getBalance)
            .min(Comparator.naturalOrder())
            .orElse(baseForecast.getStartingBalance());
        
        LocalDate lowestBalanceDate = adjustedForecast.stream()
            .filter(d -> d.getBalance().equals(lowestBalance))
            .findFirst()
            .map(CashFlowDayDto::getDate)
            .orElse(startDate);
        
        CashFlowForecastDto scenarioForecast = new CashFlowForecastDto();
        scenarioForecast.setStartDate(startDate);
        scenarioForecast.setEndDate(endDate);
        scenarioForecast.setDailyForecast(adjustedForecast);
        scenarioForecast.setTotalIncome(totalIncome);
        scenarioForecast.setTotalExpenses(totalExpenses);
        scenarioForecast.setNetFlow(totalIncome.subtract(totalExpenses));
        scenarioForecast.setStartingBalance(baseForecast.getStartingBalance());
        scenarioForecast.setEndingBalance(endingBalance);
        scenarioForecast.setLowestBalance(lowestBalance);
        scenarioForecast.setLowestBalanceDate(lowestBalanceDate);
        
        ScenarioDto scenario = new ScenarioDto();
        scenario.setName(scenarioName);
        scenario.setDescription(description);
        scenario.setStartDate(startDate);
        scenario.setEndDate(endDate);
        scenario.setIncomeAdjustment(incomeAdjustment);
        scenario.setExpenseAdjustment(expenseAdjustment);
        scenario.setForecast(scenarioForecast);
        
        return scenario;
    }
}

