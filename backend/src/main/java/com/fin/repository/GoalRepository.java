package com.fin.repository;

import com.fin.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserId(Long userId);
    List<Goal> findByUserIdAndIsCompleted(Long userId, Boolean isCompleted);
    Optional<Goal> findByIdAndUserId(Long id, Long userId);
}






