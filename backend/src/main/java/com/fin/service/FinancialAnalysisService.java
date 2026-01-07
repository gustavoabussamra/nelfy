package com.fin.service;

import com.fin.dto.FinancialAnalysisDto;
import com.fin.model.Transaction;
import com.fin.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialAnalysisService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Value("${openai.api.key:}")
    private String openAiApiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public FinancialAnalysisService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Gera análise financeira usando IA baseada nas transações do usuário
     */
    public FinancialAnalysisDto generateAnalysis(Long userId, LocalDate startDate, LocalDate endDate) {
        // Buscar transações do período
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
            userId, startDate, endDate
        );
        
        // Filtrar apenas transações de despesa pagas (não parcelas futuras)
        List<Transaction> expenseTransactions = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
                .filter(t -> t.getParentTransactionId() == null) // Não contar transações pai parceladas
                .filter(t -> !(Boolean.TRUE.equals(t.getIsInstallment()) && t.getParentTransactionId() == null))
                .collect(Collectors.toList());
        
        if (expenseTransactions.isEmpty()) {
            return createDefaultAnalysis("Você não possui despesas registradas no período para análise.");
        }
        
        // Preparar dados para análise
        Map<String, Object> financialData = prepareFinancialData(expenseTransactions, startDate, endDate);
        
        // Gerar análise com IA
        try {
            if (openAiApiKey == null || openAiApiKey.isEmpty()) {
                // Se não tiver API key, retornar análise básica
                return generateBasicAnalysis(financialData);
            }
            
            return generateAiAnalysis(financialData);
        } catch (Exception e) {
            System.err.println("Erro ao gerar análise com IA: " + e.getMessage());
            e.printStackTrace();
            // Fallback para análise básica
            return generateBasicAnalysis(financialData);
        }
    }
    
    private Map<String, Object> prepareFinancialData(List<Transaction> transactions, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();
        
        // Total de despesas
        BigDecimal totalExpenses = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Despesas por categoria
        Map<String, BigDecimal> expensesByCategory = transactions.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                    t -> t.getCategory().getName(),
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
        
        // Top 5 categorias
        List<Map<String, Object>> topCategories = expensesByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> cat = new HashMap<>();
                    cat.put("name", entry.getKey());
                    cat.put("amount", entry.getValue().doubleValue());
                    cat.put("percentage", entry.getValue().divide(totalExpenses, 2, java.math.RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")).doubleValue());
                    return cat;
                })
                .collect(Collectors.toList());
        
        // Média diária
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double dailyAverage = totalExpenses.divide(new BigDecimal(days), 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
        
        data.put("totalExpenses", totalExpenses.doubleValue());
        data.put("period", startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                  " a " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        data.put("days", days);
        data.put("dailyAverage", dailyAverage);
        data.put("totalTransactions", transactions.size());
        data.put("topCategories", topCategories);
        data.put("expensesByCategory", expensesByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().doubleValue()
                )));
        
        return data;
    }
    
    private FinancialAnalysisDto generateAiAnalysis(Map<String, Object> financialData) {
        try {
            String prompt = buildAnalysisPrompt(financialData);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1000); // Reduzido para economizar tokens
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Você é um consultor financeiro. Forneça análises objetivas e recomendações práticas de economia. " +
                    "Seja direto e acionável. Respostas em português brasileiro.");
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            
            messages.add(systemMessage);
            messages.add(userMessage);
            requestBody.put("messages", messages);
            
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseAiResponse(response, financialData);
            
        } catch (Exception e) {
            System.err.println("Erro ao chamar OpenAI API: " + e.getMessage());
            return generateBasicAnalysis(financialData);
        }
    }
    
    private String buildAnalysisPrompt(Map<String, Object> financialData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analise os seguintes dados financeiros e forneça uma análise completa com recomendações:\n\n");
        prompt.append("PERÍODO: ").append(financialData.get("period")).append("\n");
        prompt.append("TOTAL DE DESPESAS: R$ ").append(String.format("%.2f", financialData.get("totalExpenses"))).append("\n");
        prompt.append("MÉDIA DIÁRIA: R$ ").append(String.format("%.2f", financialData.get("dailyAverage"))).append("\n");
        prompt.append("TOTAL DE TRANSAÇÕES: ").append(financialData.get("totalTransactions")).append("\n\n");
        
        prompt.append("TOP 5 CATEGORIAS DE GASTOS:\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topCategories = (List<Map<String, Object>>) financialData.get("topCategories");
        for (Map<String, Object> cat : topCategories) {
            prompt.append("- ").append(cat.get("name"))
                  .append(": R$ ").append(String.format("%.2f", cat.get("amount")))
                  .append(" (").append(String.format("%.1f", cat.get("percentage"))).append("%)\n");
        }
        
        prompt.append("\nPor favor, forneça:\n");
        prompt.append("1. Uma análise geral dos gastos (identificando padrões, excessos, oportunidades)\n");
        prompt.append("2. Pelo menos 3 recomendações específicas de economia com:\n");
        prompt.append("   - Categoria relacionada\n");
        prompt.append("   - Sugestão concreta\n");
        prompt.append("   - Impacto esperado\n");
        prompt.append("   - Economia estimada em R$\n");
        prompt.append("3. Um resumo executivo com os principais pontos de atenção\n\n");
        prompt.append("Resposta em formato JSON com a seguinte estrutura:\n");
        prompt.append("{\n");
        prompt.append("  \"analysis\": \"análise completa em texto\",\n");
        prompt.append("  \"summary\": \"resumo executivo\",\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"category\": \"nome da categoria\",\n");
        prompt.append("      \"suggestion\": \"sugestão específica\",\n");
        prompt.append("      \"impact\": \"impacto esperado\",\n");
        prompt.append("      \"estimatedSavings\": 0.0\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"potentialSavings\": 0.0\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private FinancialAnalysisDto parseAiResponse(String response, Map<String, Object> financialData) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode choices = jsonResponse.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    String content = message.get("content").asText();
                    
                    // Tentar extrair JSON do conteúdo
                    String jsonContent = extractJsonFromResponse(content);
                    if (jsonContent != null) {
                        JsonNode analysisJson = objectMapper.readTree(jsonContent);
                        return parseAnalysisJson(analysisJson);
                    } else {
                        // Se não conseguir extrair JSON, usar análise básica com a resposta da IA
                        return createAnalysisFromText(content, financialData);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao parsear resposta da IA: " + e.getMessage());
        }
        return generateBasicAnalysis(financialData);
    }
    
    private String extractJsonFromResponse(String content) {
        // Tentar extrair JSON entre {}
        int startIndex = content.indexOf('{');
        int endIndex = content.lastIndexOf('}');
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return content.substring(startIndex, endIndex + 1);
        }
        return null;
    }
    
    private FinancialAnalysisDto parseAnalysisJson(JsonNode json) {
        FinancialAnalysisDto dto = new FinancialAnalysisDto();
        dto.setAnalysis(json.has("analysis") ? json.get("analysis").asText() : "");
        dto.setSummary(json.has("summary") ? json.get("summary").asText() : "");
        dto.setPotentialSavings(json.has("potentialSavings") ? json.get("potentialSavings").asDouble() : 0.0);
        
        List<FinancialAnalysisDto.Recommendation> recommendations = new ArrayList<>();
        if (json.has("recommendations") && json.get("recommendations").isArray()) {
            for (JsonNode rec : json.get("recommendations")) {
                FinancialAnalysisDto.Recommendation recommendation = new FinancialAnalysisDto.Recommendation();
                recommendation.setCategory(rec.has("category") ? rec.get("category").asText() : "");
                recommendation.setSuggestion(rec.has("suggestion") ? rec.get("suggestion").asText() : "");
                recommendation.setImpact(rec.has("impact") ? rec.get("impact").asText() : "");
                recommendation.setEstimatedSavings(rec.has("estimatedSavings") ? rec.get("estimatedSavings").asDouble() : 0.0);
                recommendations.add(recommendation);
            }
        }
        dto.setRecommendations(recommendations);
        
        return dto;
    }
    
    private FinancialAnalysisDto createAnalysisFromText(String content, Map<String, Object> financialData) {
        FinancialAnalysisDto dto = new FinancialAnalysisDto();
        dto.setAnalysis(content);
        dto.setSummary("Análise gerada pela IA. Verifique os detalhes acima.");
        dto.setRecommendations(generateBasicRecommendations(financialData));
        dto.setPotentialSavings(calculatePotentialSavings(financialData));
        return dto;
    }
    
    private FinancialAnalysisDto generateBasicAnalysis(Map<String, Object> financialData) {
        FinancialAnalysisDto dto = new FinancialAnalysisDto();
        
        double totalExpenses = (Double) financialData.get("totalExpenses");
        double dailyAverage = (Double) financialData.get("dailyAverage");
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("📊 ANÁLISE FINANCEIRA DO PERÍODO\n\n");
        analysis.append("No período de ").append(financialData.get("period")).append(", você gastou um total de R$ ")
                .append(String.format("%.2f", totalExpenses)).append(".\n");
        analysis.append("Isso representa uma média diária de R$ ").append(String.format("%.2f", dailyAverage)).append(".\n\n");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topCategories = (List<Map<String, Object>>) financialData.get("topCategories");
        if (!topCategories.isEmpty()) {
            analysis.append("🔝 PRINCIPAIS CATEGORIAS DE GASTO:\n");
            for (Map<String, Object> cat : topCategories) {
                analysis.append("• ").append(cat.get("name"))
                        .append(": R$ ").append(String.format("%.2f", cat.get("amount")))
                        .append(" (").append(String.format("%.1f", cat.get("percentage"))).append("%)\n");
            }
        }
        
        dto.setAnalysis(analysis.toString());
        dto.setSummary("Identifique suas principais categorias de gasto e considere oportunidades de economia.");
        dto.setRecommendations(generateBasicRecommendations(financialData));
        dto.setPotentialSavings(calculatePotentialSavings(financialData));
        
        return dto;
    }
    
    private List<FinancialAnalysisDto.Recommendation> generateBasicRecommendations(Map<String, Object> financialData) {
        List<FinancialAnalysisDto.Recommendation> recommendations = new ArrayList<>();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topCategories = (List<Map<String, Object>>) financialData.get("topCategories");
        
        if (!topCategories.isEmpty()) {
            // Recomendação 1: Maior categoria
            Map<String, Object> topCategory = topCategories.get(0);
            FinancialAnalysisDto.Recommendation rec1 = new FinancialAnalysisDto.Recommendation();
            rec1.setCategory(topCategory.get("name").toString());
            rec1.setSuggestion("Revisar gastos nesta categoria e identificar possíveis economias.");
            rec1.setImpact("Redução de 10-20% nos gastos desta categoria");
            rec1.setEstimatedSavings((Double) topCategory.get("amount") * 0.15);
            recommendations.add(rec1);
        }
        
        // Recomendação 2: Geral
        FinancialAnalysisDto.Recommendation rec2 = new FinancialAnalysisDto.Recommendation();
        rec2.setCategory("Geral");
        rec2.setSuggestion("Criar um orçamento mensal e acompanhar os gastos regularmente.");
        rec2.setImpact("Maior controle financeiro e identificação de oportunidades");
        rec2.setEstimatedSavings(0.0);
        recommendations.add(rec2);
        
        // Recomendação 3: Compras planejadas
        FinancialAnalysisDto.Recommendation rec3 = new FinancialAnalysisDto.Recommendation();
        rec3.setCategory("Compras");
        rec3.setSuggestion("Evitar compras por impulso e pesquisar preços antes de comprar.");
        rec3.setImpact("Economia de 5-15% em compras não planejadas");
        rec3.setEstimatedSavings(0.0);
        recommendations.add(rec3);
        
        return recommendations;
    }
    
    private double calculatePotentialSavings(Map<String, Object> financialData) {
        double totalExpenses = (Double) financialData.get("totalExpenses");
        // Estimar economia potencial de 10-15% do total
        return totalExpenses * 0.12;
    }
    
    private FinancialAnalysisDto createDefaultAnalysis(String message) {
        FinancialAnalysisDto dto = new FinancialAnalysisDto();
        dto.setAnalysis(message);
        dto.setSummary("Não há dados suficientes para análise.");
        dto.setRecommendations(new ArrayList<>());
        dto.setPotentialSavings(0.0);
        return dto;
    }
}

