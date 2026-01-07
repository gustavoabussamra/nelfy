package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutiveDashboardDto {
    // KPIs Principais
    private BigDecimal totalBalance;
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal netFlow;
    private BigDecimal savingsRate; // Taxa de poupança (%)
    
    // Comparações
    private ComparisonDto monthOverMonth;
    private ComparisonDto yearOverYear;
    
    // Tendências
    private List<MonthlyTrendDto> monthlyTrends; // Últimos 12 meses
    private List<CategoryTrendDto> topCategories;
    
    // Anomalias detectadas
    private List<AnomalyDto> anomalies;
    
    // Metas e Orçamentos
    private Integer activeGoals;
    private Integer completedGoals;
    private Integer budgetsAtRisk; // Orçamentos próximos do limite
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonDto {
        private BigDecimal incomeChange; // % de mudança
        private BigDecimal expenseChange;
        private BigDecimal netFlowChange;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrendDto {
        private String month; // "2024-01"
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal netFlow;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTrendDto {
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private BigDecimal currentMonth;
        private BigDecimal previousMonth;
        private BigDecimal changePercent;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyDto {
        private String type; // "HIGH_EXPENSE", "UNUSUAL_PATTERN", "BUDGET_EXCEEDED"
        private String description;
        private BigDecimal amount;
        private String severity; // "LOW", "MEDIUM", "HIGH"
    }
}




