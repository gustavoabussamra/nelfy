package com.fin.repository;

import com.fin.model.AiLearningPattern;
import com.fin.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiLearningPatternRepository extends JpaRepository<AiLearningPattern, Long> {
    List<AiLearningPattern> findByUser(User user);
    List<AiLearningPattern> findByUserId(Long userId);
    
    // Buscar padrões não processados
    List<AiLearningPattern> findByIsProcessedFalse();
    
    // Buscar padrões similares (para encontrar padrões relacionados)
    @Query("SELECT p FROM AiLearningPattern p WHERE p.user = :user AND p.normalizedText LIKE %:keyword%")
    List<AiLearningPattern> findSimilarPatterns(@Param("user") User user, @Param("keyword") String keyword);
    
    // Buscar padrões mais recentes
    List<AiLearningPattern> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Buscar padrões com alta confiança
    @Query("SELECT p FROM AiLearningPattern p WHERE p.user = :user AND p.confidenceScore >= :minScore ORDER BY p.confidenceScore DESC")
    List<AiLearningPattern> findHighConfidencePatterns(@Param("user") User user, @Param("minScore") Double minScore);
    
    // Buscar por tipo de transação
    List<AiLearningPattern> findByUserIdAndTransactionType(Long userId, String transactionType);
}

