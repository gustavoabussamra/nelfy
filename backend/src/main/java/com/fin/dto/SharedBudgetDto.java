package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedBudgetDto {
    private Long id;
    private Long budgetId;
    private BudgetDto budget;
    private Long ownerId;
    private String ownerName;
    private Long sharedUserId;
    private String sharedUserName;
    private String permission;
    private LocalDateTime createdAt;
}




