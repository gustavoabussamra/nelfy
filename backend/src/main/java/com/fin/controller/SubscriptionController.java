package com.fin.controller;

import com.fin.dto.SubscriptionDto;
import com.fin.model.Subscription;
import com.fin.security.SecurityUtil;
import com.fin.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "http://localhost:3000")
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping("/me")
    public ResponseEntity<SubscriptionDto> getMySubscription() {
        Long userId = securityUtil.getCurrentUserId();
        SubscriptionDto subscription = subscriptionService.getUserSubscription(userId);
        return ResponseEntity.ok(subscription);
    }
    
    @PutMapping("/me")
    public ResponseEntity<SubscriptionDto> updateMySubscription(@RequestParam String plan) {
        Long userId = securityUtil.getCurrentUserId();
        Subscription.SubscriptionPlan subscriptionPlan = Subscription.SubscriptionPlan.valueOf(plan);
        SubscriptionDto subscription = subscriptionService.updateSubscription(userId, subscriptionPlan);
        return ResponseEntity.ok(subscription);
    }
}










