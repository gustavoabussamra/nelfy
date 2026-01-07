package com.fin.service;

import com.fin.dto.CategoryDto;
import com.fin.dto.GoalDto;
import com.fin.model.Category;
import com.fin.model.Goal;
import com.fin.model.Notification;
import com.fin.model.User;
import com.fin.repository.CategoryRepository;
import com.fin.repository.GoalRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoalService {
    
    @Autowired
    private GoalRepository goalRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private NotificationService notificationService;
    
    public List<GoalDto> getUserGoals(Long userId) {
        return goalRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public GoalDto getGoalById(Long id, Long userId) {
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));
        return convertToDto(goal);
    }
    
    @Transactional
    public GoalDto createGoal(GoalDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        Goal goal = new Goal();
        goal.setName(dto.getName());
        goal.setTargetAmount(dto.getTargetAmount());
        goal.setCurrentAmount(dto.getCurrentAmount() != null ? dto.getCurrentAmount() : java.math.BigDecimal.ZERO);
        goal.setTargetDate(dto.getTargetDate());
        goal.setDescription(dto.getDescription());
        goal.setUser(user);
        
        if (dto.getAlertThreshold() != null) {
            goal.setAlertThreshold(dto.getAlertThreshold());
        }
        
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElse(null);
            goal.setCategory(category);
        }
        
        // Verificar se está desviando
        goal.checkIfOffTrack();
        
        goal = goalRepository.save(goal);
        return convertToDto(goal);
    }
    
    @Transactional
    public GoalDto updateGoal(Long id, GoalDto dto, Long userId) {
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));
        
        goal.setName(dto.getName());
        goal.setTargetAmount(dto.getTargetAmount());
        goal.setCurrentAmount(dto.getCurrentAmount() != null ? dto.getCurrentAmount() : goal.getCurrentAmount());
        goal.setTargetDate(dto.getTargetDate());
        goal.setDescription(dto.getDescription());
        
        if (dto.getAlertThreshold() != null) {
            goal.setAlertThreshold(dto.getAlertThreshold());
        }
        
        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElse(null);
            goal.setCategory(category);
        } else {
            goal.setCategory(null);
        }
        
        // Verificar se a meta foi alcançada
        goal.checkIfCompleted();
        
        // Verificar se está desviando
        goal.checkIfOffTrack();
        
        goal = goalRepository.save(goal);
        return convertToDto(goal);
    }
    
    @Transactional
    public GoalDto updateGoalProgress(Long id, BigDecimal amount, Long userId) {
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));
        
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        goal.checkIfCompleted();
        goal.checkIfOffTrack();
        
        goal = goalRepository.save(goal);
        return convertToDto(goal);
    }
    
    @Transactional
    public void deleteGoal(Long id, Long userId) {
        Goal goal = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Meta não encontrada"));
        
        goalRepository.delete(goal);
    }
    
    /**
     * Verifica metas desviando e envia notificações (executa diariamente)
     */
    @Scheduled(cron = "0 0 9 * * *") // Executa todo dia às 9h
    @Transactional
    public void checkGoalsOffTrack() {
        List<Goal> allGoals = goalRepository.findAll();
        LocalDate today = LocalDate.now();
        
        for (Goal goal : allGoals) {
            if (goal.getIsCompleted() || goal.getTargetDate() == null) {
                continue;
            }
            
            // Verificar se está desviando
            boolean wasOffTrack = goal.getIsOffTrack();
            goal.checkIfOffTrack();
            
            // Se acabou de ficar desviando e não foi alertado hoje
            if (goal.getIsOffTrack() && (!wasOffTrack || goal.getLastAlertDate() == null || !goal.getLastAlertDate().equals(today))) {
                BigDecimal expected = goal.getExpectedAmount();
                BigDecimal current = goal.getCurrentAmount();
                BigDecimal difference = expected.subtract(current);
                
                String message = String.format(
                    "Meta '%s' está desviando do objetivo! Esperado: R$ %.2f, Atual: R$ %.2f (faltam R$ %.2f)",
                    goal.getName(),
                    expected,
                    current,
                    difference
                );
                
                notificationService.createNotification(
                    goal.getUser().getId(),
                    "Meta Desviando",
                    message,
                    Notification.NotificationType.GOAL_ALERT,
                    null
                );
                
                goal.setLastAlertDate(today);
                goalRepository.save(goal);
            }
        }
    }
    
    private GoalDto convertToDto(Goal goal) {
        GoalDto dto = new GoalDto();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setTargetDate(goal.getTargetDate());
        dto.setDescription(goal.getDescription());
        dto.setIsCompleted(goal.getIsCompleted());
        dto.setCompletedDate(goal.getCompletedDate());
        dto.setProgressPercentage(goal.getProgressPercentage());
        dto.setDaysRemaining(goal.getDaysRemaining());
        dto.setIsOffTrack(goal.getIsOffTrack());
        dto.setExpectedAmount(goal.getExpectedAmount());
        dto.setAlertThreshold(goal.getAlertThreshold());
        
        if (goal.getCategory() != null) {
            Category category = goal.getCategory();
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(category.getId());
            categoryDto.setName(category.getName());
            categoryDto.setIcon(category.getIcon());
            categoryDto.setColor(category.getColor());
            categoryDto.setType(category.getType().name());
            dto.setCategory(categoryDto);
            dto.setCategoryId(category.getId());
        }
        
        return dto;
    }
}

