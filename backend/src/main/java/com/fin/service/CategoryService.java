package com.fin.service;

import com.fin.dto.CategoryDto;
import com.fin.model.Category;
import com.fin.model.User;
import com.fin.repository.CategoryRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private PlanLimitsService planLimitsService;
    
    @Cacheable(value = "categories", key = "#userId")
    public List<CategoryDto> getUserCategories(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        return categoryRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Cacheable(value = "categories", key = "#userId + '_' + #type.name()")
    public List<CategoryDto> getUserCategoriesByType(Long userId, com.fin.model.TransactionType type) {
        return categoryRepository.findByUserIdAndType(userId, type).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = "categories", key = "#userId", allEntries = true)
    public CategoryDto createCategory(CategoryDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        if (!planLimitsService.canCreateCategory(userId)) {
            throw new RuntimeException("Limite de categorias atingido para seu plano. Faça upgrade para criar mais categorias.");
        }
        
        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setColor(dto.getColor());
        category.setType(com.fin.model.TransactionType.valueOf(dto.getType()));
        category.setUser(user);
        
        category = categoryRepository.save(category);
        return convertToDto(category);
    }
    
    @Transactional
    @CacheEvict(value = "categories", key = "#userId", allEntries = true)
    public CategoryDto updateCategory(Long id, CategoryDto dto, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
        
        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setColor(dto.getColor());
        category.setType(com.fin.model.TransactionType.valueOf(dto.getType()));
        
        category = categoryRepository.save(category);
        return convertToDto(category);
    }
    
    @Transactional
    @CacheEvict(value = "categories", key = "#userId", allEntries = true)
    public void deleteCategory(Long id, Long userId) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
        
        if (!category.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        categoryRepository.delete(category);
    }
    
    private CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setColor(category.getColor());
        dto.setType(category.getType().name());
        return dto;
    }
}

