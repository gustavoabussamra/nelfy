package com.fin.controller;

import com.fin.dto.GoalDto;
import com.fin.security.SecurityUtil;
import com.fin.service.GoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "http://localhost:3000")
public class GoalController {
    
    @Autowired
    private GoalService goalService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<GoalDto>> getMyGoals(@RequestParam(required = false) Boolean completed) {
        Long userId = securityUtil.getCurrentUserId();
        List<GoalDto> goals;
        
        if (completed != null) {
            // Filtrar por status de conclusão se necessário
            goals = goalService.getUserGoals(userId);
            if (completed) {
                goals = goals.stream().filter(GoalDto::getIsCompleted).toList();
            } else {
                goals = goals.stream().filter(g -> !g.getIsCompleted()).toList();
            }
        } else {
            goals = goalService.getUserGoals(userId);
        }
        
        return ResponseEntity.ok(goals);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<GoalDto> getGoal(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        GoalDto goal = goalService.getGoalById(id, userId);
        return ResponseEntity.ok(goal);
    }
    
    @PostMapping
    public ResponseEntity<GoalDto> createGoal(@RequestBody GoalDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        GoalDto goal = goalService.createGoal(dto, userId);
        return ResponseEntity.ok(goal);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<GoalDto> updateGoal(@PathVariable Long id, @RequestBody GoalDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        GoalDto goal = goalService.updateGoal(id, dto, userId);
        return ResponseEntity.ok(goal);
    }
    
    @PutMapping("/{id}/progress")
    public ResponseEntity<GoalDto> updateProgress(
            @PathVariable Long id, 
            @RequestParam BigDecimal amount) {
        Long userId = securityUtil.getCurrentUserId();
        GoalDto goal = goalService.updateGoalProgress(id, amount, userId);
        return ResponseEntity.ok(goal);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        goalService.deleteGoal(id, userId);
        return ResponseEntity.noContent().build();
    }
}






