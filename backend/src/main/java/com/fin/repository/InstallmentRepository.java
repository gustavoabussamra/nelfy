package com.fin.repository;

import com.fin.model.Installment;
import com.fin.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstallmentRepository extends JpaRepository<Installment, Long> {
    List<Installment> findByTransaction(Transaction transaction);
    List<Installment> findByTransactionId(Long transactionId);
    
    @Query("SELECT i FROM Installment i WHERE i.transaction.user.id = :userId AND i.isPaid = false AND i.dueDate BETWEEN :startDate AND :endDate ORDER BY i.dueDate ASC")
    List<Installment> findUnpaidInstallmentsByUserAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT i FROM Installment i WHERE i.transaction.user.id = :userId AND i.isPaid = false AND i.dueDate <= :date ORDER BY i.dueDate ASC")
    List<Installment> findOverdueInstallments(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT i FROM Installment i WHERE i.transaction.user.id = :userId AND i.isPaid = false AND i.dueDate BETWEEN :startDate AND :endDate ORDER BY i.dueDate ASC")
    List<Installment> findUpcomingInstallments(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}










