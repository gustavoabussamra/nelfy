package com.fin.repository;

import com.fin.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
    Long countByUserIdAndIsReadFalse(Long userId);
    
    // Queries paginadas
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    // Query otimizada para verificar se já existe notificação para uma transação em uma data específica
    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.relatedTransactionId = :transactionId AND CAST(n.createdAt AS date) = :date AND n.type = :type")
    boolean existsByTransactionIdAndDateAndType(@Param("transactionId") Long transactionId, @Param("date") LocalDate date, @Param("type") Notification.NotificationType type);
}

