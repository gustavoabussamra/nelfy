package com.fin.controller;

import com.fin.dto.PaymentRequestDto;
import com.fin.dto.PaymentResponseDto;
import com.fin.security.SecurityUtil;
import com.fin.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDto> createPayment(@RequestBody PaymentRequestDto request) {
        Long userId = securityUtil.getCurrentUserId();
        PaymentResponseDto response = paymentService.createPaymentRequest(userId, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestParam String paymentId, @RequestParam String status) {
        paymentService.processPaymentWebhook(paymentId, status);
        return ResponseEntity.ok().build();
    }
}




