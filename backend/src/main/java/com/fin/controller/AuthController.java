package com.fin.controller;

import com.fin.dto.AuthResponse;
import com.fin.dto.LoginRequest;
import com.fin.dto.RegisterRequest;
import com.fin.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        System.out.println("========================================");
        System.out.println("=== AUTH CONTROLLER - LOGIN RECEBIDO ===");
        System.out.println("========================================");
        System.out.println("Email recebido: " + request.getEmail());
        System.out.println("Password recebido: " + (request.getPassword() != null ? "***" : "null"));
        
        try {
            AuthResponse response = authService.login(request);
            System.out.println("Login bem-sucedido para: " + request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("ERRO no login: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}



