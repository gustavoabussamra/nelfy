package com.fin.controller;

import com.fin.dto.ExecutiveDashboardDto;
import com.fin.security.SecurityUtil;
import com.fin.service.ExecutiveDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/executive")
@CrossOrigin(origins = "http://localhost:3000")
public class ExecutiveDashboardController {
    
    @Autowired
    private ExecutiveDashboardService executiveDashboardService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<ExecutiveDashboardDto> getExecutiveDashboard() {
        Long userId = securityUtil.getCurrentUserId();
        ExecutiveDashboardDto dashboard = executiveDashboardService.getExecutiveDashboard(userId);
        return ResponseEntity.ok(dashboard);
    }
}




