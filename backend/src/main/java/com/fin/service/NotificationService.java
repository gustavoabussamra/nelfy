package com.fin.service;

import com.fin.dto.NotificationDto;
import com.fin.dto.PageResponse;
import com.fin.model.Notification;
import com.fin.model.Transaction;
import com.fin.model.User;
import com.fin.repository.NotificationRepository;
import com.fin.repository.TransactionRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // Método paginado para notificações
    public PageResponse<NotificationDto> getUserNotificationsPaged(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        List<NotificationDto> content = notificationPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, page, size, notificationPage.getTotalElements());
    }
    
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // Método paginado para notificações não lidas
    public PageResponse<NotificationDto> getUnreadNotificationsPaged(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        
        List<NotificationDto> content = notificationPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, page, size, notificationPage.getTotalElements());
    }
    
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }
    
    @Transactional
    public NotificationDto markAsRead(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        notification.setIsRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());
        notification = notificationRepository.save(notification);
        
        return convertToDto(notification);
    }
    
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        notifications.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(now);
        });
        notificationRepository.saveAll(notifications);
    }
    
    @Transactional
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada"));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        notificationRepository.delete(notification);
    }
    
    // Verificar contas a pagar e criar notificações
    @Scheduled(cron = "0 0 9 * * *") // Executa todo dia às 9h
    @Transactional
    public void checkBillsToPay() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate threeDaysFromNow = today.plusDays(3);
            
            // OTIMIZAÇÃO CRÍTICA: Usar query específica ao invés de findAll()
            // Buscar apenas transações de despesa não pagas com vencimento nos próximos 3 dias
            List<Transaction> upcomingBills = transactionRepository.findUnpaidTransactionsByTypeAndDueDateRange(
                Transaction.TransactionType.EXPENSE,
                today,
                threeDaysFromNow
            );
            
            for (Transaction bill : upcomingBills) {
                // OTIMIZAÇÃO: Usar query otimizada ao invés de buscar todas as notificações
                boolean alreadyNotified = notificationRepository.existsByTransactionIdAndDateAndType(
                    bill.getId(),
                    today,
                    Notification.NotificationType.BILL_REMINDER
                );
                
                if (!alreadyNotified) {
                    long daysUntilDue = ChronoUnit.DAYS.between(today, bill.getDueDate());
                    String message;
                    if (daysUntilDue == 0) {
                        message = String.format("A conta '%s' vence HOJE! Valor: R$ %.2f", 
                                bill.getDescription(), bill.getAmount());
                    } else if (daysUntilDue == 1) {
                        message = String.format("A conta '%s' vence AMANHÃ! Valor: R$ %.2f", 
                                bill.getDescription(), bill.getAmount());
                    } else {
                        message = String.format("A conta '%s' vence em %d dias. Valor: R$ %.2f", 
                                bill.getDescription(), daysUntilDue, bill.getAmount());
                    }
                    
                    Notification notification = new Notification();
                    notification.setUser(bill.getUser());
                    notification.setTitle("Lembrete de Conta a Pagar");
                    notification.setMessage(message);
                    notification.setType(Notification.NotificationType.BILL_REMINDER);
                    notification.setRelatedTransactionId(bill.getId());
                    
                    notificationRepository.save(notification);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao verificar contas a pagar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Método para criar notificação manualmente (pode ser chamado por outros serviços)
    @Transactional
    public NotificationDto createNotification(Long userId, String title, String message, 
                                             Notification.NotificationType type, Long relatedTransactionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedTransactionId(relatedTransactionId);
        
        notification = notificationRepository.save(notification);
        return convertToDto(notification);
    }
    
    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType().name());
        dto.setIsRead(notification.getIsRead());
        dto.setReadAt(notification.getReadAt());
        dto.setRelatedTransactionId(notification.getRelatedTransactionId());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}

