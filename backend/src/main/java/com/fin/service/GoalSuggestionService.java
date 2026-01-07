package com.fin.service;

import com.fin.dto.GoalSuggestionDto;
import com.fin.model.Category;
import com.fin.model.Transaction;
import com.fin.repository.CategoryRepository;
import com.fin.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoalSuggestionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    /**
     * Gera sugestões inteligentes de metas baseadas no histórico do usuário
     */
    public List<GoalSuggestionDto> suggestGoals(Long userId) {
        List<GoalSuggestionDto> suggestions = new ArrayList<>();
        
        // Analisar gastos por categoria nos últimos 6 meses
        Map<Category, BigDecimal> categorySpending = analyzeCategorySpending(userId, 6);
        
        // Sugerir metas de economia baseadas em categorias com maior gasto
        categorySpending.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(3)
            .forEach(entry -> {
                Category category = entry.getKey();
                BigDecimal avgSpending = entry.getValue();
                
                // Sugerir economizar 20% do gasto médio mensal
                BigDecimal suggestedAmount = avgSpending.multiply(BigDecimal.valueOf(0.20))
                    .multiply(BigDecimal.valueOf(6)); // Meta de 6 meses
                
                GoalSuggestionDto suggestion = new GoalSuggestionDto();
                suggestion.setName("Economizar em " + category.getName());
                suggestion.setDescription(
                    String.format("Baseado no seu histórico, você gasta em média R$ %.2f/mês com %s. " +
                        "Sugerimos uma meta de economizar R$ %.2f em 6 meses.",
                        avgSpending, category.getName(), suggestedAmount)
                );
                suggestion.setSuggestedAmount(suggestedAmount);
                suggestion.setSuggestedTargetDate(LocalDate.now().plusMonths(6));
                suggestion.setSuggestedCategoryId(category.getId());
                suggestion.setReason("Gasto alto nesta categoria nos últimos meses");
                suggestion.setConfidence(0.75);
                
                suggestions.add(suggestion);
            });
        
        // Sugerir meta de emergência (3 meses de despesas)
        BigDecimal monthlyExpenses = calculateMonthlyExpenses(userId);
        if (monthlyExpenses.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal emergencyFund = monthlyExpenses.multiply(BigDecimal.valueOf(3));
            
            GoalSuggestionDto emergencyGoal = new GoalSuggestionDto();
            emergencyGoal.setName("Fundo de Emergência");
            emergencyGoal.setDescription(
                String.format("Recomendamos ter um fundo de emergência equivalente a 3 meses de despesas (R$ %.2f). " +
                    "Isso garante segurança financeira em caso de imprevistos.",
                    emergencyFund)
            );
            emergencyGoal.setSuggestedAmount(emergencyFund);
            emergencyGoal.setSuggestedTargetDate(LocalDate.now().plusMonths(12));
            emergencyGoal.setReason("Padrão recomendado de segurança financeira");
            emergencyGoal.setConfidence(0.90);
            
            suggestions.add(emergencyGoal);
        }
        
        // Sugerir meta baseada em padrão de receitas
        BigDecimal monthlyIncome = calculateMonthlyIncome(userId);
        if (monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            // Sugerir poupar 10% da renda mensal
            BigDecimal savingsGoal = monthlyIncome.multiply(BigDecimal.valueOf(0.10))
                .multiply(BigDecimal.valueOf(12)); // Meta anual
            
            GoalSuggestionDto savingsSuggestion = new GoalSuggestionDto();
            savingsSuggestion.setName("Poupança Anual");
            savingsSuggestion.setDescription(
                String.format("Com base na sua renda média de R$ %.2f/mês, sugerimos uma meta de poupar " +
                    "R$ %.2f ao longo do ano (10%% da renda mensal).",
                    monthlyIncome, savingsGoal)
            );
            savingsSuggestion.setSuggestedAmount(savingsGoal);
            savingsSuggestion.setSuggestedTargetDate(LocalDate.now().plusYears(1));
            savingsSuggestion.setReason("Regra dos 10% - padrão de educação financeira");
            savingsSuggestion.setConfidence(0.85);
            
            suggestions.add(savingsSuggestion);
        }
        
        return suggestions;
    }
    
    private Map<Category, BigDecimal> analyzeCategorySpending(Long userId, int months) {
        LocalDate startDate = LocalDate.now().minusMonths(months);
        
        List<Transaction> transactions = transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
            .filter(t -> {
                LocalDate date = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                return date != null && !date.isBefore(startDate);
            })
            .filter(t -> t.getCategory() != null)
            .collect(Collectors.toList());
        
        Map<Category, BigDecimal> totalByCategory = transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    Transaction::getAmount,
                    BigDecimal::add
                )
            ));
        
        // Calcular média mensal
        Map<Category, BigDecimal> avgByCategory = new HashMap<>();
        totalByCategory.forEach((category, total) -> {
            BigDecimal avg = total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            avgByCategory.put(category, avg);
        });
        
        return avgByCategory;
    }
    
    private BigDecimal calculateMonthlyExpenses(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        return transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
            .filter(t -> {
                LocalDate date = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
            })
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateMonthlyIncome(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        return transactionRepository.findByUserId(userId).stream()
            .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
            .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
            .filter(t -> {
                LocalDate date = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
            })
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}




