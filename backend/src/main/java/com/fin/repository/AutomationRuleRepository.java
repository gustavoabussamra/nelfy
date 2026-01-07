package com.fin.repository;

import com.fin.model.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationRuleRepository extends JpaRepository<AutomationRule, Long> {
    List<AutomationRule> findByUserIdAndIsActiveTrueOrderByPriorityDesc(Long userId);
    List<AutomationRule> findByUserIdOrderByPriorityDesc(Long userId);
}




