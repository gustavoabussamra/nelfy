package com.fin.controller;

import com.fin.dto.BudgetDto;
import com.fin.dto.CashFlowForecastDto;
import com.fin.dto.ScenarioDto;
import com.fin.security.SecurityUtil;
import com.fin.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "http://localhost:3000")
public class BudgetController {
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<BudgetDto>> getMyBudgets() {
        Long userId = securityUtil.getCurrentUserId();
        List<BudgetDto> budgets = budgetService.getUserBudgets(userId);
        return ResponseEntity.ok(budgets);
    }
    
    @GetMapping("/alerts")
    public ResponseEntity<List<BudgetDto>> getBudgetsWithAlerts() {
        Long userId = securityUtil.getCurrentUserId();
        List<BudgetDto> budgets = budgetService.getActiveBudgetsWithAlerts(userId);
        return ResponseEntity.ok(budgets);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto> getBudget(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        BudgetDto budget = budgetService.getBudget(id, userId);
        return ResponseEntity.ok(budget);
    }
    
    @PostMapping
    public ResponseEntity<BudgetDto> createBudget(@RequestBody BudgetDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        BudgetDto budget = budgetService.createBudget(dto, userId);
        return ResponseEntity.ok(budget);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto> updateBudget(@PathVariable Long id, @RequestBody BudgetDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        BudgetDto budget = budgetService.updateBudget(id, dto, userId);
        return ResponseEntity.ok(budget);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        budgetService.deleteBudget(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/cash-flow-forecast")
    public ResponseEntity<CashFlowForecastDto> getCashFlowForecast(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long accountId) {
        Long userId = securityUtil.getCurrentUserId();
        CashFlowForecastDto forecast = budgetService.getCashFlowForecast(userId, startDate, endDate, accountId);
        return ResponseEntity.ok(forecast);
    }
    
    @PostMapping("/scenarios")
    public ResponseEntity<ScenarioDto> simulateScenario(@RequestBody Map<String, Object> request) {
        Long userId = securityUtil.getCurrentUserId();
        String name = request.get("name").toString();
        String description = request.getOrDefault("description", "").toString();
        LocalDate startDate = LocalDate.parse(request.get("startDate").toString());
        LocalDate endDate = LocalDate.parse(request.get("endDate").toString());
        BigDecimal incomeAdjustment = request.get("incomeAdjustment") != null 
            ? new BigDecimal(request.get("incomeAdjustment").toString()) : null;
        BigDecimal expenseAdjustment = request.get("expenseAdjustment") != null 
            ? new BigDecimal(request.get("expenseAdjustment").toString()) : null;
        Long accountId = request.get("accountId") != null 
            ? Long.parseLong(request.get("accountId").toString()) : null;
        
        ScenarioDto scenario = budgetService.simulateScenario(
            userId, name, description, startDate, endDate,
            incomeAdjustment, expenseAdjustment, accountId
        );
        return ResponseEntity.ok(scenario);
    }
}







