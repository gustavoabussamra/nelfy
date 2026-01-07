package com.fin.controller;

import com.fin.dto.AiTransactionResponse;
import com.fin.dto.FinancialAnalysisDto;
import com.fin.dto.PageResponse;
import com.fin.dto.TransactionDto;
import com.fin.security.SecurityUtil;
import com.fin.service.AiLearningService;
import com.fin.service.AiTransactionService;
import com.fin.service.FinancialAnalysisService;
import com.fin.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:3000")
public class TransactionController {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private AiTransactionService aiTransactionService;
    
    @Autowired
    private FinancialAnalysisService financialAnalysisService;
    
    @Autowired
    private AiLearningService aiLearningService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<TransactionDto>> getMyTransactions() {
        Long userId = securityUtil.getCurrentUserId();
        List<TransactionDto> transactions = transactionService.getUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/paged")
    public ResponseEntity<PageResponse<TransactionDto>> getMyTransactionsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtil.getCurrentUserId();
        PageResponse<TransactionDto> transactions = transactionService.getUserTransactionsPaged(userId, page, size);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/all")
    public ResponseEntity<List<TransactionDto>> getAllMyTransactions() {
        Long userId = securityUtil.getCurrentUserId();
        List<TransactionDto> transactions = transactionService.getAllUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/installments")
    public ResponseEntity<List<TransactionDto>> getInstallmentTransactions() {
        Long userId = securityUtil.getCurrentUserId();
        List<TransactionDto> transactions = transactionService.getInstallmentTransactions(userId);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<List<TransactionDto>> getMonthlyTransactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        System.out.println("=== GET /api/transactions/monthly ===");
        System.out.println("Month parameter: " + month);
        try {
            Long userId = securityUtil.getCurrentUserId();
            System.out.println("UserId: " + userId);
            List<TransactionDto> transactions = transactionService.getMonthlyTransactions(userId, month);
            System.out.println("Transactions found: " + transactions.size());
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            System.out.println("ERROR in getMonthlyTransactions: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @GetMapping("/monthly/category-stats")
    public ResponseEntity<List<CategoryStatsDto>> getMonthlyCategoryStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        System.out.println("=== GET /api/transactions/monthly/category-stats ===");
        System.out.println("Month parameter: " + month);
        try {
            Long userId = securityUtil.getCurrentUserId();
            System.out.println("UserId: " + userId);
            List<CategoryStatsDto> stats = transactionService.getMonthlyCategoryStats(userId, month);
            System.out.println("Category stats found: " + stats.size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.out.println("ERROR in getMonthlyCategoryStats: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        TransactionDto transaction = transactionService.getTransaction(id, userId);
        return ResponseEntity.ok(transaction);
    }
    
    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(@RequestBody TransactionDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        TransactionDto transaction = transactionService.createTransaction(dto, userId);
        return ResponseEntity.ok(transaction);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(@PathVariable Long id, @RequestBody TransactionDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        TransactionDto transaction = transactionService.updateTransaction(id, dto, userId);
        return ResponseEntity.ok(transaction);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = securityUtil.getCurrentUserId();
        BigDecimal balance = transactionService.getBalance(userId, startDate, endDate);
        return ResponseEntity.ok(balance);
    }
    
    @PutMapping("/{id}/mark-paid")
    public ResponseEntity<TransactionDto> markAsPaid(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        TransactionDto transaction = transactionService.markAsPaid(id, userId);
        return ResponseEntity.ok(transaction);
    }
    
    @PutMapping("/{id}/mark-unpaid")
    public ResponseEntity<TransactionDto> markAsUnpaid(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        TransactionDto transaction = transactionService.markAsUnpaid(id, userId);
        return ResponseEntity.ok(transaction);
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<List<TransactionDto>> getUpcomingTransactions() {
        Long userId = securityUtil.getCurrentUserId();
        List<TransactionDto> transactions = transactionService.getUpcomingTransactions(userId);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/overdue")
    public ResponseEntity<List<TransactionDto>> getOverdueTransactions() {
        Long userId = securityUtil.getCurrentUserId();
        List<TransactionDto> transactions = transactionService.getOverdueTransactions(userId);
        return ResponseEntity.ok(transactions);
    }
    
    @GetMapping("/{id}/installments")
    public ResponseEntity<List<TransactionDto>> getInstallments(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        List<TransactionDto> installments = transactionService.getInstallmentsByParentId(id, userId);
        return ResponseEntity.ok(installments);
    }
    
    @PostMapping("/update-balance")
    public ResponseEntity<TransactionDto> updateBalance(@RequestBody UpdateBalanceDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        TransactionDto transaction = transactionService.createBalanceUpdateTransaction(dto.getAmount(), dto.getDescription(), userId);
        return ResponseEntity.ok(transaction);
    }
    
    @PostMapping("/ai/create")
    public ResponseEntity<AiTransactionResponse> createTransactionFromText(@RequestBody AiTransactionRequest request) {
        System.out.println("=== POST /api/transactions/ai/create ===");
        
        // Verificar autenticação antes de continuar
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            System.out.println("SecurityContext Authentication: " + (auth != null ? auth.getName() : "NULL"));
            System.out.println("SecurityContext isAuthenticated: " + (auth != null && auth.isAuthenticated()));
        } catch (Exception e) {
            System.out.println("ERROR checking SecurityContext: " + e.getMessage());
        }
        
        if (request == null) {
            System.out.println("ERROR: Request body is null");
            return ResponseEntity.badRequest().build();
        }
        System.out.println("Request body text: " + request.getText());
        try {
            Long userId = securityUtil.getCurrentUserId();
            System.out.println("UserId: " + userId);
            AiTransactionResponse response = aiTransactionService.processTextAndCreateTransaction(request.getText(), userId);
            System.out.println("Response success: " + response.isSuccess());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERROR in createTransactionFromText: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(403).body(AiTransactionResponse.needsInfo(
                "Erro de autenticação: " + e.getMessage(),
                null,
                "Por favor, faça login novamente."
            ));
        }
    }
    
    @PostMapping("/ai/confirm")
    public ResponseEntity<AiTransactionResponse> confirmTransaction(@RequestBody TransactionDto dto) {
        System.out.println("=== POST /api/transactions/ai/confirm ===");
        try {
            Long userId = securityUtil.getCurrentUserId();
            System.out.println("UserId: " + userId);
            System.out.println("Transaction DTO recebido:");
            System.out.println("  - Description: " + dto.getDescription());
            System.out.println("  - Amount: " + dto.getAmount());
            System.out.println("  - Type: " + dto.getType());
            System.out.println("  - DueDate: " + dto.getDueDate());
            System.out.println("  - TransactionDate: " + dto.getTransactionDate());
            System.out.println("  - TotalInstallments: " + dto.getTotalInstallments());
            System.out.println("  - IsPaid: " + dto.getIsPaid());
            
            // Criar a transação com os dados confirmados
            TransactionDto createdTransaction = transactionService.createTransaction(dto, userId);
            
            String successMessage = "Transação criada com sucesso!";
            if (dto.getTotalInstallments() != null && dto.getTotalInstallments() > 1 && dto.getAmount() != null) {
                BigDecimal totalAmount = dto.getAmount().multiply(new BigDecimal(dto.getTotalInstallments()));
                successMessage += String.format(" Criada compra parcelada de %s parcelas de R$ %.2f cada (total: R$ %.2f).",
                    dto.getTotalInstallments(), dto.getAmount(), totalAmount);
            } else if (dto.getAmount() != null) {
                successMessage += String.format(" %s de R$ %.2f criada com sucesso.",
                    dto.getType().equals("INCOME") ? "Receita" : "Despesa",
                    dto.getAmount());
            }
            
            return ResponseEntity.ok(AiTransactionResponse.success(createdTransaction, successMessage));
        } catch (Exception e) {
            System.out.println("ERROR in confirmTransaction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(AiTransactionResponse.needsInfo(
                "Erro ao confirmar transação: " + e.getMessage(),
                null,
                "Por favor, tente novamente."
            ));
        }
    }
    
    @GetMapping("/ai/analysis")
    public ResponseEntity<FinancialAnalysisDto> getFinancialAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long userId = securityUtil.getCurrentUserId();
        
        // Se não informar datas, usar últimos 30 dias
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        FinancialAnalysisDto analysis = financialAnalysisService.generateAnalysis(userId, startDate, endDate);
        return ResponseEntity.ok(analysis);
    }
    
    @PostMapping("/ai/train")
    public ResponseEntity<Map<String, Object>> trainLearningSystem() {
        try {
            aiLearningService.trainWithHistoricalPatterns();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sistema de aprendizado treinado com sucesso"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Erro ao treinar sistema: " + e.getMessage()
            ));
        }
    }
    
    public static class CategoryStatsDto {
        private Long categoryId;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        private BigDecimal totalAmount;
        private Long transactionCount;
        
        public CategoryStatsDto() {}
        
        public CategoryStatsDto(Long categoryId, String categoryName, String categoryIcon, String categoryColor, BigDecimal totalAmount, Long transactionCount) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.categoryIcon = categoryIcon;
            this.categoryColor = categoryColor;
            this.totalAmount = totalAmount;
            this.transactionCount = transactionCount;
        }
        
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        
        public String getCategoryIcon() { return categoryIcon; }
        public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
        
        public String getCategoryColor() { return categoryColor; }
        public void setCategoryColor(String categoryColor) { this.categoryColor = categoryColor; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public Long getTransactionCount() { return transactionCount; }
        public void setTransactionCount(Long transactionCount) { this.transactionCount = transactionCount; }
    }
    
    public static class UpdateBalanceDto {
        private java.math.BigDecimal amount;
        private String description;
        
        public java.math.BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(java.math.BigDecimal amount) {
            this.amount = amount;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
    
    public static class AiTransactionRequest {
        private String text;
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
}

