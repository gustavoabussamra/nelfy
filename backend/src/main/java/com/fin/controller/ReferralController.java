package com.fin.controller;

import com.fin.dto.ReferralDto;
import com.fin.security.SecurityUtil;
import com.fin.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/referrals")
@CrossOrigin(origins = "http://localhost:3000")
public class ReferralController {
    
    @Autowired
    private ReferralService referralService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping("/my-code")
    public ResponseEntity<Map<String, String>> getMyReferralCode() {
        Long userId = securityUtil.getCurrentUserId();
        String code = referralService.generateReferralCode(userId);
        return ResponseEntity.ok(Map.of("referralCode", code));
    }
    
    @GetMapping("/my-referrals")
    public ResponseEntity<List<ReferralDto>> getMyReferrals() {
        Long userId = securityUtil.getCurrentUserId();
        List<ReferralDto> referrals = referralService.getUserReferrals(userId);
        return ResponseEntity.ok(referrals);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getReferralStats() {
        Long userId = securityUtil.getCurrentUserId();
        Long totalReferrals = referralService.countUserReferrals(userId);
        BigDecimal totalCommissions = referralService.getTotalCommissions(userId);
        BigDecimal expectedFutureCommissions = referralService.getExpectedFutureCommissions(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReferrals", totalReferrals);
        stats.put("totalCommissions", totalCommissions);
        stats.put("expectedFutureCommissions", expectedFutureCommissions);
        
        return ResponseEntity.ok(stats);
    }
}

