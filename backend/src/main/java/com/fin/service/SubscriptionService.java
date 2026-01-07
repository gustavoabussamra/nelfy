package com.fin.service;

import com.fin.dto.SubscriptionDto;
import com.fin.model.Subscription;
import com.fin.model.User;
import com.fin.repository.SubscriptionRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionService {
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Subscription getUserSubscriptionEntity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        return subscriptionRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Assinatura não encontrada"));
    }
    
    public SubscriptionDto getUserSubscription(Long userId) {
        Subscription subscription = getUserSubscriptionEntity(userId);
        return convertToDto(subscription);
    }
    
    @Transactional
    public SubscriptionDto updateSubscription(Long userId, Subscription.SubscriptionPlan plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
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
        return convertToDto(subscription);
    }
    
    @Scheduled(cron = "0 0 0 * * ?") // Executa diariamente à meia-noite
    @Transactional
    public void checkExpiredSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        for (Subscription subscription : subscriptions) {
            if (subscription.getEndDate() != null && 
                subscription.getEndDate().isBefore(now) && 
                subscription.getIsActive()) {
                subscription.setIsActive(false);
                subscriptionRepository.save(subscription);
            }
        }
    }
    
    public boolean isSubscriptionActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Subscription subscription = subscriptionRepository.findByUser(user).orElse(null);
        
        if (subscription == null || !subscription.getIsActive()) {
            return false;
        }
        
        if (subscription.getEndDate() != null && subscription.getEndDate().isBefore(LocalDateTime.now())) {
            subscription.setIsActive(false);
            subscriptionRepository.save(subscription);
            return false;
        }
        
        return true;
    }
    
    private SubscriptionDto convertToDto(Subscription subscription) {
        SubscriptionDto dto = new SubscriptionDto();
        dto.setId(subscription.getId());
        dto.setPlan(subscription.getPlan().name());
        dto.setIsActive(subscription.getIsActive());
        dto.setStartDate(subscription.getStartDate());
        dto.setEndDate(subscription.getEndDate());
        return dto;
    }
}

