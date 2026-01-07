package com.fin.repository;

import com.fin.model.Budget;
import com.fin.model.Category;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUser(User user);
    List<Budget> findByUserId(Long userId);
    List<Budget> findByUserAndIsActive(User user, Boolean isActive);
    
    @Query("SELECT b FROM Budget b WHERE b.user = :user AND b.isActive = true AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsByUserAndDate(@Param("user") User user, @Param("date") LocalDate date);
    
    List<Budget> findByCategory(Category category);
}










