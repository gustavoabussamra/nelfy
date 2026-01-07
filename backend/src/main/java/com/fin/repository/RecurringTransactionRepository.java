package com.fin.repository;

import com.fin.model.RecurringTransaction;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {
    List<RecurringTransaction> findByUser(User user);
    List<RecurringTransaction> findByUserId(Long userId);
    List<RecurringTransaction> findByUserIdAndIsActiveTrue(Long userId);
    
    // Buscar recorrências que precisam ser processadas
    @Query("SELECT r FROM RecurringTransaction r WHERE r.user.id = :userId AND r.isActive = true AND r.autoCreate = true AND (r.nextOccurrenceDate <= :date OR r.nextOccurrenceDate IS NULL) AND (r.endDate IS NULL OR r.endDate >= :date)")
    List<RecurringTransaction> findRecurringTransactionsToProcess(@Param("userId") Long userId, @Param("date") LocalDate date);
}




