package com.fin.service;

import com.fin.dto.AuthResponse;
import com.fin.dto.LoginRequest;
import com.fin.dto.RegisterRequest;
import com.fin.dto.UserDto;
import com.fin.model.Subscription;
import com.fin.model.User;
import com.fin.repository.SubscriptionRepository;
import com.fin.repository.UserRepository;
import com.fin.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private ReferralService referralService;
    
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email já está em uso");
        }
        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        user = userRepository.save(user);
        
        // Criar assinatura grátis de 30 dias
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(Subscription.SubscriptionPlan.FREE);
        subscription.setIsActive(true);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(30));
        subscriptionRepository.save(subscription);
        
        // Processar código de referência se fornecido
        if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
            try {
                String code = request.getReferralCode().trim().toUpperCase();
                System.out.println("=== PROCESSANDO CÓDIGO DE REFERÊNCIA ===");
                System.out.println("Código recebido: " + code);
                System.out.println("ID do novo usuário: " + user.getId());
                referralService.processReferral(code, user.getId());
                System.out.println("Código de referência processado com sucesso!");
            } catch (Exception e) {
                // Log do erro mas não impede o registro
                System.err.println("=== ERRO AO PROCESSAR CÓDIGO DE REFERÊNCIA ===");
                System.err.println("Código: " + request.getReferralCode());
                System.err.println("Erro: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Nenhum código de referência fornecido no registro");
        }
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        
        return new AuthResponse(token, "Bearer", convertToDto(user));
    }
    
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtUtil.generateToken(userDetails);
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Log para debug
        System.out.println("=========================================");
        System.out.println("AuthService.login - Usuário encontrado:");
        System.out.println("Email: " + user.getEmail());
        System.out.println("Role do banco (raw): '" + user.getRole() + "'");
        System.out.println("Role length: " + (user.getRole() != null ? user.getRole().length() : 0));
        System.out.println("Role (uppercase): '" + (user.getRole() != null ? user.getRole().toUpperCase().trim() : "NULL") + "'");
        System.out.println("Role equals 'ADMIN': " + "ADMIN".equals(user.getRole()));
        System.out.println("Role equalsIgnoreCase 'ADMIN': " + "ADMIN".equalsIgnoreCase(user.getRole() != null ? user.getRole().trim() : ""));
        System.out.println("=========================================");
        
        UserDto userDto = convertToDto(user);
        System.out.println("UserDto criado - Role final: '" + userDto.getRole() + "'");
        System.out.println("UserDto Role equals 'ADMIN': " + "ADMIN".equals(userDto.getRole()));
        
        return new AuthResponse(token, "Bearer", userDto);
    }
    
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        
        // GARANTIR que a role está sempre em UPPERCASE e sem espaços
        String role = user.getRole();
        if (role != null) {
            role = role.toUpperCase().trim();
        } else {
            role = "USER"; // Default se null
        }
        dto.setRole(role);
        
        System.out.println("convertToDto - Role original: " + user.getRole());
        System.out.println("convertToDto - Role normalizada: " + role);
        
        // ADMIN não deve ter subscription - não incluir no DTO
        if (!"ADMIN".equals(role)) {
            Subscription subscription = subscriptionRepository.findByUser(user).orElse(null);
            if (subscription != null) {
                dto.setSubscription(convertSubscriptionToDto(subscription));
            }
        }
        // Se for ADMIN, subscription será null (não incluído)
        
        return dto;
    }
    
    private com.fin.dto.SubscriptionDto convertSubscriptionToDto(Subscription subscription) {
        com.fin.dto.SubscriptionDto dto = new com.fin.dto.SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setPlan(subscription.getPlan().name());
        dto.setIsActive(subscription.getIsActive());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        return dto;
    }
}

