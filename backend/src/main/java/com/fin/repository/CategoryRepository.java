package com.fin.repository;

import com.fin.model.Category;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUser(User user);
    List<Category> findByUserId(Long userId);
    List<Category> findByUserIdAndType(Long userId, com.fin.model.TransactionType type);
    List<Category> findByUserAndName(User user, String name);
}



