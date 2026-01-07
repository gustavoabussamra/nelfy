package com.fin.security;

import com.fin.config.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean shouldSkip = path != null && path.contains("/api/auth/");
        if (shouldSkip) {
            System.out.println("=== JWT Filter: PULANDO FILTRO PARA ROTA DE AUTH ===");
            System.out.println("Path: " + path);
        }
        // Não aplicar filtro JWT para rotas de autenticação
        return shouldSkip;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");
        String requestPath = request.getRequestURI();
        
        System.out.println("=== JWT Filter - Processando request ===");
        System.out.println("Path: " + requestPath);
        System.out.println("Method: " + request.getMethod());
        
        String username = null;
        String jwt = null;
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                System.out.println("ERROR extracting username from JWT: " + e.getMessage());
            }
        }
        
        if (username != null) {
            org.springframework.security.core.Authentication existingAuth = 
                SecurityContextHolder.getContext().getAuthentication();
            
            boolean shouldUpdateAuth = existingAuth == null || 
                !existingAuth.isAuthenticated() || 
                !existingAuth.getName().equals(username);
            
            if (shouldUpdateAuth) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    System.out.println("JWT Filter: Autenticação definida para: " + username);
                } else {
                    SecurityContextHolder.clearContext();
                    System.out.println("JWT Filter: Token inválido para: " + username);
                }
            }
        }
        
        chain.doFilter(request, response);
    }
}



