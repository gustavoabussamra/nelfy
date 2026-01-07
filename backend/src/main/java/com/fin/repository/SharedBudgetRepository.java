package com.fin.repository;

import com.fin.model.Budget;
import com.fin.model.SharedBudget;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedBudgetRepository extends JpaRepository<SharedBudget, Long> {
    List<SharedBudget> findByBudget(Budget budget);
    List<SharedBudget> findByBudgetId(Long budgetId);
    List<SharedBudget> findBySharedUser(User user);
    List<SharedBudget> findBySharedUserId(Long userId);
    List<SharedBudget> findByOwner(User owner);
    List<SharedBudget> findByOwnerId(Long ownerId);
    Optional<SharedBudget> findByBudgetIdAndSharedUserId(Long budgetId, Long sharedUserId);
    boolean existsByBudgetIdAndSharedUserId(Long budgetId, Long sharedUserId);
}




