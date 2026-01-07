package com.fin.controller;

import com.fin.dto.AccountDto;
import com.fin.security.SecurityUtil;
import com.fin.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "http://localhost:3000")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<AccountDto>> getMyAccounts() {
        Long userId = securityUtil.getCurrentUserId();
        List<AccountDto> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        AccountDto account = accountService.getAccount(id, userId);
        return ResponseEntity.ok(account);
    }
    
    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        AccountDto account = accountService.createAccount(dto, userId);
        return ResponseEntity.ok(account);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id, @RequestBody AccountDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        AccountDto account = accountService.updateAccount(id, dto, userId);
        return ResponseEntity.ok(account);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        accountService.deleteAccount(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/total-balance")
    public ResponseEntity<Map<String, BigDecimal>> getTotalBalance() {
        Long userId = securityUtil.getCurrentUserId();
        BigDecimal total = accountService.getTotalBalance(userId);
        return ResponseEntity.ok(Map.of("totalBalance", total));
    }
}




