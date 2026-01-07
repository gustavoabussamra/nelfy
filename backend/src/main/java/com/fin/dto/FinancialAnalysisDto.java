package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialAnalysisDto {
    private String analysis; // Análise completa gerada pela IA
    private List<Recommendation> recommendations; // Lista de recomendações
    private String summary; // Resumo executivo
    private Double potentialSavings; // Potencial de economia em R$
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recommendation {
        private String category; // Categoria relacionada
        private String suggestion; // Sugestão específica
        private String impact; // Impacto esperado
        private Double estimatedSavings; // Economia estimada
    }
}









