package com.fin.controller;

import com.fin.dto.RecurringTransactionDto;
import com.fin.security.SecurityUtil;
import com.fin.service.RecurringTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
@CrossOrigin(origins = "http://localhost:3000")
public class RecurringTransactionController {
    
    @Autowired
    private RecurringTransactionService recurringService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<RecurringTransactionDto>> getMyRecurringTransactions() {
        Long userId = securityUtil.getCurrentUserId();
        List<RecurringTransactionDto> recurring = recurringService.getUserRecurringTransactions(userId);
        return ResponseEntity.ok(recurring);
    }
    
    @PostMapping
    public ResponseEntity<RecurringTransactionDto> createRecurringTransaction(@RequestBody RecurringTransactionDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        RecurringTransactionDto recurring = recurringService.createRecurringTransaction(dto, userId);
        return ResponseEntity.ok(recurring);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionDto> updateRecurringTransaction(@PathVariable Long id, @RequestBody RecurringTransactionDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        RecurringTransactionDto recurring = recurringService.updateRecurringTransaction(id, dto, userId);
        return ResponseEntity.ok(recurring);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        recurringService.deleteRecurringTransaction(id, userId);
        return ResponseEntity.noContent().build();
    }
}




