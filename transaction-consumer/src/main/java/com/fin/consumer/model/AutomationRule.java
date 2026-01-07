package com.fin.consumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "automation_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "condition_type", nullable = false)
    private String conditionType;
    
    @Column(name = "condition_value", columnDefinition = "TEXT")
    private String conditionValue;
    
    @Column(name = "action_type", nullable = false)
    private String actionType;
    
    @Column(name = "action_value", columnDefinition = "TEXT")
    private String actionValue;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "priority")
    private Integer priority = 0;
    
    @Column(name = "execution_count")
    private Integer executionCount = 0;
    
    @Column(name = "last_execution")
    private LocalDateTime lastExecution;
}




