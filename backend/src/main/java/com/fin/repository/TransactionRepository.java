package com.fin.repository;

import com.fin.model.Transaction;
import com.fin.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
    List<Transaction> findByUserId(Long userId);
    
    List<Transaction> findByParentTransactionId(Long parentTransactionId);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndTypeAndDateRange(
        @Param("user") User user,
        @Param("type") Transaction.TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    // Query otimizada para buscar transações não pagas com vencimento em um intervalo de datas
    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.isPaid = false AND t.dueDate BETWEEN :startDate AND :endDate")
    List<Transaction> findUnpaidTransactionsByTypeAndDueDateRange(
        @Param("type") Transaction.TransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Query otimizada para buscar transações do usuário com JOIN FETCH para evitar N+1
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.user.id = :userId")
    List<Transaction> findByUserIdWithCategory(@Param("userId") Long userId);
    
    // Query paginada para buscar transações do usuário
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.user.id = :userId ORDER BY t.dueDate DESC, t.createdAt DESC")
    Page<Transaction> findByUserIdWithCategoryPaged(@Param("userId") Long userId, Pageable pageable);
    
    // Query otimizada para buscar transações principais (não parcelas)
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.user.id = :userId AND t.parentTransactionId IS NULL")
    List<Transaction> findMainTransactionsByUserId(@Param("userId") Long userId);
    
    // Query paginada para buscar transações principais (não parcelas)
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.user.id = :userId AND t.parentTransactionId IS NULL ORDER BY t.dueDate DESC, t.createdAt DESC")
    Page<Transaction> findMainTransactionsByUserIdPaged(@Param("userId") Long userId, Pageable pageable);
    
    // Query otimizada para buscar transações parceladas
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.user.id = :userId AND t.parentTransactionId IS NULL AND t.isInstallment = true AND t.totalInstallments > 1")
    List<Transaction> findInstallmentTransactionsByUserId(@Param("userId") Long userId);
    
    // Query otimizada para buscar transações vencendo em breve
    // Exclui transações pai parceladas (isInstallment = true E parentTransactionId = null)
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.user.id = :userId AND (t.isPaid IS NULL OR t.isPaid = false) AND t.dueDate BETWEEN :startDate AND :endDate AND NOT (t.isInstallment = true AND t.parentTransactionId IS NULL)")
    List<Transaction> findUpcomingTransactionsByUserId(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Query otimizada para buscar transações vencidas
    // Exclui transações pai parceladas (isInstallment = true E parentTransactionId = null)
    @Query("SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.user.id = :userId AND (t.isPaid IS NULL OR t.isPaid = false) AND t.dueDate < :date AND NOT (t.isInstallment = true AND t.parentTransactionId IS NULL)")
    List<Transaction> findOverdueTransactionsByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);
}

