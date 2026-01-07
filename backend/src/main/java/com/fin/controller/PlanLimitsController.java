package com.fin.controller;

import com.fin.security.SecurityUtil;
import com.fin.service.PlanLimitsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plan-limits")
@CrossOrigin(origins = "http://localhost:3000")
public class PlanLimitsController {
    
    @Autowired
    private PlanLimitsService planLimitsService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyPlanLimits() {
        Long userId = securityUtil.getCurrentUserId();
        PlanLimitsService.PlanLimits limits = planLimitsService.getPlanLimits(userId);
        
        return ResponseEntity.ok(Map.of(
            "isActive", limits.isActive(),
            "maxTransactions", limits.getMaxTransactions(),
            "maxCategories", limits.getMaxCategories(),
            "maxAccounts", limits.getMaxAccounts(),
            "maxAttachments", limits.getMaxAttachments(),
            "maxGoals", limits.getMaxGoals(),
            "maxBudgets", limits.getMaxBudgets(),
            "canExportExcel", limits.canExportExcel(),
            "canUseAI", limits.canUseAI(),
            "canCollaborate", limits.canCollaborate()
        ));
    }
}




