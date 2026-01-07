package com.fin.service;

import com.fin.dto.ExecutiveDashboardDto;
import com.fin.model.Account;
import com.fin.model.Budget;
import com.fin.model.Category;
import com.fin.model.Goal;
import com.fin.model.Transaction;
import com.fin.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExecutiveDashboardService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private BudgetRepository budgetRepository;
    
    public ExecutiveDashboardDto getExecutiveDashboard(Long userId) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        YearMonth sameMonthLastYear = currentMonth.minusYears(1);
        
        // KPIs Principais
        BigDecimal totalBalance = getTotalBalance(userId);
        ExecutiveDashboardDto.MonthlyTrendDto currentMonthData = getMonthlyData(userId, currentMonth);
        ExecutiveDashboardDto.MonthlyTrendDto previousMonthData = getMonthlyData(userId, previousMonth);
        ExecutiveDashboardDto.MonthlyTrendDto lastYearData = getMonthlyData(userId, sameMonthLastYear);
        
        BigDecimal monthlyIncome = currentMonthData.getIncome();
        BigDecimal monthlyExpense = currentMonthData.getExpense();
        BigDecimal netFlow = monthlyIncome.subtract(monthlyExpense);
        BigDecimal savingsRate = monthlyIncome.compareTo(BigDecimal.ZERO) > 0
            ? netFlow.divide(monthlyIncome, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
            : BigDecimal.ZERO;
        
        // Comparações
        ExecutiveDashboardDto.ComparisonDto monthOverMonth = new ExecutiveDashboardDto.ComparisonDto(
            calculatePercentChange(previousMonthData.getIncome(), monthlyIncome),
            calculatePercentChange(previousMonthData.getExpense(), monthlyExpense),
            calculatePercentChange(previousMonthData.getNetFlow(), netFlow)
        );
        
        ExecutiveDashboardDto.ComparisonDto yearOverYear = new ExecutiveDashboardDto.ComparisonDto(
            calculatePercentChange(lastYearData.getIncome(), monthlyIncome),
            calculatePercentChange(lastYearData.getExpense(), monthlyExpense),
            calculatePercentChange(lastYearData.getNetFlow(), netFlow)
        );
        
        // Tendências (últimos 12 meses)
        List<ExecutiveDashboardDto.MonthlyTrendDto> monthlyTrends = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            monthlyTrends.add(getMonthlyData(userId, month));
        }
        
        // Top Categorias com tendência
        List<ExecutiveDashboardDto.CategoryTrendDto> topCategories = getCategoryTrends(userId, currentMonth, previousMonth);
        
        // Anomalias
        List<ExecutiveDashboardDto.AnomalyDto> anomalies = detectAnomalies(userId, currentMonth);
        
        // Metas e Orçamentos
        List<Goal> goals = goalRepository.findByUserId(userId);
        int activeGoals = (int) goals.stream().filter(g -> !g.getIsCompleted()).count();
        int completedGoals = (int) goals.stream().filter(Goal::getIsCompleted).count();
        
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        int budgetsAtRisk = (int) budgets.stream()
            .filter(b -> {
                if (!b.getIsActive()) return false;
                BigDecimal spent = getBudgetSpent(userId, b);
                BigDecimal percentage = b.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(b.getLimitAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
                return percentage.compareTo(BigDecimal.valueOf(b.getAlertPercentage())) >= 0;
            })
            .count();
        
        ExecutiveDashboardDto dashboard = new ExecutiveDashboardDto();
        dashboard.setTotalBalance(totalBalance);
        dashboard.setMonthlyIncome(monthlyIncome);
        dashboard.setMonthlyExpense(monthlyExpense);
        dashboard.setNetFlow(netFlow);
        dashboard.setSavingsRate(savingsRate);
        dashboard.setMonthOverMonth(monthOverMonth);
        dashboard.setYearOverYear(yearOverYear);
        dashboard.setMonthlyTrends(monthlyTrends);
        dashboard.setTopCategories(topCategories);
        dashboard.setAnomalies(anomalies);
        dashboard.setActiveGoals(activeGoals);
        dashboard.setCompletedGoals(completedGoals);
        dashboard.setBudgetsAtRisk(budgetsAtRisk);
        
        return dashboard;
    }
    
    private BigDecimal getTotalBalance(Long userId) {
        return accountRepository.findByUserIdAndIsActiveTrue(userId).stream()
            .map(Account::getBalance)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private ExecutiveDashboardDto.MonthlyTrendDto getMonthlyData(Long userId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
            .filter(t -> {
                LocalDate date = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
            })
            .filter(t -> {
                // Excluir transações pai parceladas
                if (Boolean.TRUE.equals(t.getIsInstallment()) && t.getParentTransactionId() == null) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        BigDecimal income = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expense = transactions.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal netFlow = income.subtract(expense);
        
        return new ExecutiveDashboardDto.MonthlyTrendDto(
            month.format(DateTimeFormatter.ofPattern("yyyy-MM")),
            income,
            expense,
            netFlow
        );
    }
    
    private BigDecimal calculatePercentChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue == null || oldValue.compareTo(BigDecimal.ZERO) == 0) {
            return newValue.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return newValue.subtract(oldValue)
            .divide(oldValue.abs(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    private List<ExecutiveDashboardDto.CategoryTrendDto> getCategoryTrends(Long userId, YearMonth current, YearMonth previous) {
        Map<Category, BigDecimal> currentMonthMap = getCategoryTotals(userId, current);
        Map<Category, BigDecimal> previousMonthMap = getCategoryTotals(userId, previous);
        
        return currentMonthMap.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .map(entry -> {
                Category cat = entry.getKey();
                BigDecimal currentAmount = entry.getValue();
                BigDecimal previousAmount = previousMonthMap.getOrDefault(cat, BigDecimal.ZERO);
                BigDecimal changePercent = calculatePercentChange(previousAmount, currentAmount);
                
                return new ExecutiveDashboardDto.CategoryTrendDto(
                    cat.getId(),
                    cat.getName(),
                    cat.getIcon(),
                    currentAmount,
                    previousAmount,
                    changePercent
                );
            })
            .collect(Collectors.toList());
    }
    
    private Map<Category, BigDecimal> getCategoryTotals(Long userId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        
        return transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
            .filter(t -> {
                LocalDate date = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
            })
            .filter(t -> t.getCategory() != null)
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Transaction::getAmount,
                    BigDecimal::add
                )
            ));
    }
    
    private List<ExecutiveDashboardDto.AnomalyDto> detectAnomalies(Long userId, YearMonth month) {
        List<ExecutiveDashboardDto.AnomalyDto> anomalies = new ArrayList<>();
        
        // Calcular média de gastos por categoria nos últimos 3 meses
        Map<Category, BigDecimal> avgSpending = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            YearMonth prevMonth = month.minusMonths(i);
            Map<Category, BigDecimal> monthTotals = getCategoryTotals(userId, prevMonth);
            monthTotals.forEach((cat, amount) -> {
                avgSpending.merge(cat, amount, BigDecimal::add);
            });
        }
        avgSpending.replaceAll((k, v) -> v.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP));
        
        // Detectar gastos anômalos (50% acima da média)
        Map<Category, BigDecimal> currentTotals = getCategoryTotals(userId, month);
        currentTotals.forEach((cat, currentAmount) -> {
            BigDecimal avg = avgSpending.getOrDefault(cat, BigDecimal.ZERO);
            if (avg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal increase = currentAmount.subtract(avg)
                    .divide(avg, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
                
                if (increase.compareTo(BigDecimal.valueOf(50)) > 0) {
                    String severity = increase.compareTo(BigDecimal.valueOf(100)) > 0 ? "HIGH" : "MEDIUM";
                    anomalies.add(new ExecutiveDashboardDto.AnomalyDto(
                        "HIGH_EXPENSE",
                        String.format("Gasto em %s está %.0f%% acima da média", cat.getName(), increase),
                        currentAmount,
                        severity
                    ));
                }
            }
        });
        
        // Detectar orçamentos excedidos
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        budgets.stream()
            .filter(Budget::getIsActive)
            .forEach(budget -> {
                BigDecimal spent = getBudgetSpent(userId, budget);
                if (spent.compareTo(budget.getLimitAmount()) > 0) {
                    anomalies.add(new ExecutiveDashboardDto.AnomalyDto(
                        "BUDGET_EXCEEDED",
                        String.format("Orçamento '%s' foi excedido em R$ %.2f", budget.getName(), 
                            spent.subtract(budget.getLimitAmount())),
                        spent.subtract(budget.getLimitAmount()),
                        "HIGH"
                    ));
                }
            });
        
        return anomalies;
    }
    
    private BigDecimal getBudgetSpent(Long userId, Budget budget) {
        LocalDate startDate = budget.getStartDate();
        LocalDate endDate = budget.getEndDate();
        
        return transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
            .filter(t -> {
                if (budget.getCategory() != null) {
                    return budget.getCategory().equals(t.getCategory());
                }
                return true;
            })
            .filter(t -> {
                LocalDate date = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
            })
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

