package com.fin.controller;

import com.fin.dto.SubscriptionDto;
import com.fin.dto.UserDto;
import com.fin.model.Subscription;
import com.fin.security.SecurityUtil;
import com.fin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    private void checkAdminAccess() {
        if (!securityUtil.isAdmin()) {
            throw new RuntimeException("Acesso negado. Apenas administradores podem acessar esta funcionalidade.");
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy) {
        checkAdminAccess();
        List<UserDto> users = adminService.getAllUsers(page, size, sortBy);
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        checkAdminAccess();
        UserDto user = adminService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/users/{id}/subscription")
    public ResponseEntity<SubscriptionDto> updateUserSubscription(
            @PathVariable Long id,
            @RequestParam String plan) {
        checkAdminAccess();
        Subscription.SubscriptionPlan subscriptionPlan = Subscription.SubscriptionPlan.valueOf(plan);
        SubscriptionDto subscription = adminService.updateUserSubscription(id, subscriptionPlan);
        return ResponseEntity.ok(subscription);
    }
    
    @PutMapping("/users/{id}/subscription/deactivate")
    public ResponseEntity<Void> deactivateUserSubscription(@PathVariable Long id) {
        checkAdminAccess();
        adminService.deactivateUserSubscription(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/users/{id}/subscription/extend")
    public ResponseEntity<SubscriptionDto> extendUserSubscription(
            @PathVariable Long id,
            @RequestParam Integer days) {
        checkAdminAccess();
        adminService.extendUserSubscription(id, days);
        UserDto user = adminService.getUserById(id);
        return ResponseEntity.ok(user.getSubscription());
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        checkAdminAccess();
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        checkAdminAccess();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", adminService.getTotalUsers());
        stats.put("activeSubscriptions", adminService.getActiveSubscriptions());
        return ResponseEntity.ok(stats);
    }
    
    @PostMapping("/create-admin")
    public ResponseEntity<UserDto> createAdmin(@RequestBody UserDto userDto) {
        checkAdminAccess();
        UserDto newAdmin = adminService.createAdminUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newAdmin);
    }
}

