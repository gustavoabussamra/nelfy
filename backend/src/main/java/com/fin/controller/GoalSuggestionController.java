package com.fin.controller;

import com.fin.dto.GoalSuggestionDto;
import com.fin.security.SecurityUtil;
import com.fin.service.GoalSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals/suggestions")
@CrossOrigin(origins = "http://localhost:3000")
public class GoalSuggestionController {
    
    @Autowired
    private GoalSuggestionService goalSuggestionService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<GoalSuggestionDto>> getSuggestions() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(goalSuggestionService.suggestGoals(userId));
    }
}




