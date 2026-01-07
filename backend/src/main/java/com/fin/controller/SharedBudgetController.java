package com.fin.controller;

import com.fin.dto.SharedBudgetDto;
import com.fin.security.SecurityUtil;
import com.fin.service.SharedBudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shared-budgets")
@CrossOrigin(origins = "http://localhost:3000")
public class SharedBudgetController {
    
    @Autowired
    private SharedBudgetService sharedBudgetService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @PostMapping("/budget/{budgetId}/share")
    public ResponseEntity<SharedBudgetDto> shareBudget(
            @PathVariable Long budgetId,
            @RequestBody Map<String, Object> request) {
        Long ownerId = securityUtil.getCurrentUserId();
        Long sharedUserId = Long.parseLong(request.get("sharedUserId").toString());
        String permission = request.getOrDefault("permission", "READ_ONLY").toString();
        
        SharedBudgetDto shared = sharedBudgetService.shareBudget(budgetId, sharedUserId, permission, ownerId);
        return ResponseEntity.ok(shared);
    }
    
    @GetMapping("/with-me")
    public ResponseEntity<List<SharedBudgetDto>> getSharedBudgetsWithMe() {
        Long userId = securityUtil.getCurrentUserId();
        List<SharedBudgetDto> shared = sharedBudgetService.getSharedBudgetsWithMe(userId);
        return ResponseEntity.ok(shared);
    }
    
    @GetMapping("/i-shared")
    public ResponseEntity<List<SharedBudgetDto>> getBudgetsIShared() {
        Long userId = securityUtil.getCurrentUserId();
        List<SharedBudgetDto> shared = sharedBudgetService.getBudgetsIShared(userId);
        return ResponseEntity.ok(shared);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unshareBudget(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        sharedBudgetService.unshareBudget(id, userId);
        return ResponseEntity.noContent().build();
    }
}




