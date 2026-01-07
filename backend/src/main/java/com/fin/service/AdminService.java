package com.fin.service;

import com.fin.dto.SubscriptionDto;
import com.fin.dto.UserDto;
import com.fin.model.Subscription;
import com.fin.model.User;
import com.fin.repository.SubscriptionRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<UserDto> getAllUsers(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy != null ? sortBy : "createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);
        
        return users.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return convertToDto(user);
    }
    
    @Transactional
    public SubscriptionDto updateUserSubscription(Long userId, Subscription.SubscriptionPlan plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Verificar se o usuário é admin - admins não podem ter subscription
        String role = user.getRole();
        if (role != null) {
            role = role.toUpperCase().trim();
        }
        if ("ADMIN".equals(role)) {
            throw new RuntimeException("Administradores não podem ter assinatura");
        }
        
        Subscription subscription = subscriptionRepository.findByUser(user)
                .orElseGet(() -> {
                    Subscription newSubscription = new Subscription();
                    newSubscription.setUser(user);
                    return newSubscription;
                });
        
        subscription.setPlan(plan);
        subscription.setIsActive(true);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(LocalDateTime.now().plusDays(plan.getDays()));
        
        subscription = subscriptionRepository.save(subscription);
        return convertSubscriptionToDto(subscription);
    }
    
    @Transactional
    public void deactivateUserSubscription(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Verificar se o usuário é admin - admins não podem ter subscription
        String role = user.getRole();
        if (role != null) {
            role = role.toUpperCase().trim();
        }
        if ("ADMIN".equals(role)) {
            throw new RuntimeException("Administradores não têm assinatura para desativar");
        }
        
        Subscription subscription = subscriptionRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Assinatura não encontrada"));
        
        subscription.setIsActive(false);
        subscriptionRepository.save(subscription);
    }
    
    @Transactional
    public void extendUserSubscription(Long userId, Integer days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Verificar se o usuário é admin - admins não podem ter subscription
        String role = user.getRole();
        if (role != null) {
            role = role.toUpperCase().trim();
        }
        if ("ADMIN".equals(role)) {
            throw new RuntimeException("Administradores não têm assinatura para estender");
        }
        
        Subscription subscription = subscriptionRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Assinatura não encontrada"));
        
        if (subscription.getEndDate() != null) {
            subscription.setEndDate(subscription.getEndDate().plusDays(days));
        } else {
            subscription.setEndDate(LocalDateTime.now().plusDays(days));
        }
        
        subscription.setIsActive(true);
        subscriptionRepository.save(subscription);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        userRepository.delete(user);
    }
    
    public Long getTotalUsers() {
        // Contar apenas usuários não-admin (usuários com plano)
        List<User> allUsers = userRepository.findAll();
        long totalNonAdmin = allUsers.stream()
                .filter(user -> {
                    String role = user.getRole();
                    if (role != null) {
                        role = role.toUpperCase().trim();
                    }
                    boolean isAdmin = "ADMIN".equals(role);
                    if (isAdmin) {
                        System.out.println("getTotalUsers - Excluindo admin: " + user.getEmail() + " (role: " + user.getRole() + ")");
                    }
                    return !isAdmin;
                })
                .count();
        
        System.out.println("=========================================");
        System.out.println("getTotalUsers - Total de usuários no banco: " + allUsers.size());
        System.out.println("getTotalUsers - Total de usuários não-admin: " + totalNonAdmin);
        System.out.println("=========================================");
        
        return totalNonAdmin;
    }
    
    public Long getActiveSubscriptions() {
        // Contar apenas assinaturas de usuários não-admin
        List<Subscription> allSubscriptions = subscriptionRepository.findAll();
        long activeNonAdmin = allSubscriptions.stream()
                .filter(s -> {
                    // Verificar se o usuário não é admin
                    User user = s.getUser();
                    String role = user.getRole();
                    if (role != null) {
                        role = role.toUpperCase().trim();
                    }
                    boolean isAdmin = "ADMIN".equals(role);
                    boolean isNotAdmin = !isAdmin;
                    
                    if (isAdmin) {
                        System.out.println("getActiveSubscriptions - Excluindo subscription de admin: " + user.getEmail() + " (plan: " + s.getPlan() + ")");
                    }
                    
                    // Verificar se a assinatura está ativa
                    boolean isActive = s.getIsActive() && 
                            (s.getEndDate() == null || s.getEndDate().isAfter(LocalDateTime.now()));
                    
                    return isNotAdmin && isActive;
                })
                .count();
        
        System.out.println("=========================================");
        System.out.println("getActiveSubscriptions - Total de subscriptions no banco: " + allSubscriptions.size());
        System.out.println("getActiveSubscriptions - Total de subscriptions ativas (não-admin): " + activeNonAdmin);
        System.out.println("=========================================");
        
        return activeNonAdmin;
    }
    
    @Transactional
    public UserDto createAdminUser(UserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email já está em uso");
        }
        
        User adminUser = new User();
        adminUser.setName(dto.getName());
        adminUser.setEmail(dto.getEmail());
        adminUser.setPassword(passwordEncoder.encode(dto.getPassword() != null ? dto.getPassword() : "admin123"));
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        
        return convertToDto(adminUser);
    }
    
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        
        // Normalizar role
        String role = user.getRole();
        if (role != null) {
            role = role.toUpperCase().trim();
        } else {
            role = "USER";
        }
        dto.setRole(role);
        
        // ADMIN não deve ter subscription - não incluir no DTO
        if (!"ADMIN".equals(role)) {
            Subscription subscription = subscriptionRepository.findByUser(user).orElse(null);
            if (subscription != null) {
                dto.setSubscription(convertSubscriptionToDto(subscription));
            }
        }
        
        return dto;
    }
    
    private SubscriptionDto convertSubscriptionToDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setPlan(subscription.getPlan().name());
        dto.setIsActive(subscription.getIsActive());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        return dto;
    }
}

