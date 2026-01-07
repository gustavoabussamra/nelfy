package com.fin.security;

import com.fin.model.User;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    
    @Autowired
    private UserRepository userRepository;
    
    public Long getCurrentUserId() {
        try {
            org.springframework.security.core.Authentication auth = 
                SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                System.out.println("ERROR: SecurityContext authentication is null or not authenticated");
                throw new RuntimeException("Usuário não autenticado");
            }
            String email = auth.getName();
            System.out.println("SecurityUtil.getCurrentUserId - Email: " + email);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));
            System.out.println("SecurityUtil.getCurrentUserId - UserId: " + user.getId());
            return user.getId();
        } catch (Exception e) {
            System.out.println("ERROR in SecurityUtil.getCurrentUserId: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }
    
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> auth.equals("ROLE_ADMIN"));
    }
}

