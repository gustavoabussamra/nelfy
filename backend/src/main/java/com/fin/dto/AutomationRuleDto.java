package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationRuleDto {
    private Long id;
    private String name;
    private String description;
    private String conditionType;
    private String conditionValue;
    private String actionType;
    private String actionValue;
    private Boolean isActive;
    private Integer priority;
    private Integer executionCount;
    private LocalDateTime lastExecution;
    private LocalDateTime createdAt;
}




