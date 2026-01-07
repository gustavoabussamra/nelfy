package com.fin.controller;

import com.fin.dto.AutomationRuleDto;
import com.fin.security.SecurityUtil;
import com.fin.service.AutomationRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/automation-rules")
@CrossOrigin(origins = "http://localhost:3000")
public class AutomationRuleController {
    
    @Autowired
    private AutomationRuleService automationRuleService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<AutomationRuleDto>> getMyRules() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(automationRuleService.getUserRules(userId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AutomationRuleDto> getRule(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(automationRuleService.getRule(id, userId));
    }
    
    @PostMapping
    public ResponseEntity<AutomationRuleDto> createRule(@RequestBody AutomationRuleDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(automationRuleService.createRule(userId, dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AutomationRuleDto> updateRule(@PathVariable Long id, @RequestBody AutomationRuleDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(automationRuleService.updateRule(id, userId, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        automationRuleService.deleteRule(id, userId);
        return ResponseEntity.noContent().build();
    }
}




