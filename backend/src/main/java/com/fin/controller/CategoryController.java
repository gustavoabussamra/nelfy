package com.fin.controller;

import com.fin.dto.CategoryDto;
import com.fin.security.SecurityUtil;
import com.fin.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getMyCategories(@RequestParam(required = false) String type) {
        Long userId = securityUtil.getCurrentUserId();
        List<CategoryDto> categories;
        
        if (type != null && !type.isEmpty()) {
            try {
                com.fin.model.TransactionType transactionType = com.fin.model.TransactionType.valueOf(type.toUpperCase());
                categories = categoryService.getUserCategoriesByType(userId, transactionType);
            } catch (IllegalArgumentException e) {
                categories = categoryService.getUserCategories(userId);
            }
        } else {
            categories = categoryService.getUserCategories(userId);
        }
        
        return ResponseEntity.ok(categories);
    }
    
    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@RequestBody CategoryDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        CategoryDto category = categoryService.createCategory(dto, userId);
        return ResponseEntity.ok(category);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long id, @RequestBody CategoryDto dto) {
        Long userId = securityUtil.getCurrentUserId();
        CategoryDto category = categoryService.updateCategory(id, dto, userId);
        return ResponseEntity.ok(category);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.noContent().build();
    }
}



