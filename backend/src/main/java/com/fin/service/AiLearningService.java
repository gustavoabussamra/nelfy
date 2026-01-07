package com.fin.service;

import com.fin.model.AiLearningPattern;
import com.fin.model.Category;
import com.fin.model.User;
import com.fin.repository.AiLearningPatternRepository;
import com.fin.repository.CategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiLearningService {
    
    @Autowired
    private AiLearningPatternRepository patternRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Value("${openai.api.key:}")
    private String openAiApiKey;
    
    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String openAiApiUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public AiLearningService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Busca padrão similar já aprendido no banco (evita chamar OpenAI)
     * @return Padrão encontrado ou null se não encontrar
     */
    public AiLearningPattern findSimilarLearnedPattern(String originalText, String normalizedText, Long userId) {
        // Buscar padrões similares já processados
        List<AiLearningPattern> similarPatterns = findSimilarPatterns(originalText, userId);
        
        // Filtrar apenas padrões processados com alta confiança
        similarPatterns = similarPatterns.stream()
            .filter(p -> p.getIsProcessed() != null && p.getIsProcessed())
            .filter(p -> p.getConfidenceScore() != null && p.getConfidenceScore() >= 0.7)
            .collect(Collectors.toList());
        
        if (similarPatterns.isEmpty()) {
            return null;
        }
        
        // Calcular similaridade mais precisa
        String normalizedInput = normalizedText.toLowerCase().trim();
        
        for (AiLearningPattern pattern : similarPatterns) {
            String normalizedPattern = pattern.getNormalizedText() != null ? 
                pattern.getNormalizedText().toLowerCase().trim() : "";
            
            // Verificar se é muito similar (mesma estrutura)
            double similarity = calculateSimilarity(normalizedInput, normalizedPattern);
            
            if (similarity >= 0.8) { // 80% de similaridade
                System.out.println("Padrão similar encontrado no banco (similaridade: " + similarity + 
                    "). Evitando chamada à OpenAI.");
                return pattern;
            }
        }
        
        return null;
    }
    
    /**
     * Calcula similaridade entre dois textos (método simples baseado em palavras-chave)
     */
    private double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }
        
        // Se textos são idênticos
        if (text1.equals(text2)) {
            return 1.0;
        }
        
        // Contar palavras em comum
        String[] words1 = text1.split("\\s+");
        String[] words2 = text2.split("\\s+");
        
        int commonWords = 0;
        int totalWords = Math.max(words1.length, words2.length);
        
        for (String word1 : words1) {
            if (word1.length() > 3) { // Ignorar palavras muito curtas
                for (String word2 : words2) {
                    if (word1.equals(word2) || word1.contains(word2) || word2.contains(word1)) {
                        commonWords++;
                        break;
                    }
                }
            }
        }
        
        return totalWords > 0 ? (double) commonWords / totalWords : 0.0;
    }
    
    /**
     * Busca melhor padrão aprendido como fallback (mesmo com baixa confiança)
     */
    public AiLearningPattern findBestFallbackPattern(String originalText, String normalizedText, Long userId) {
        List<AiLearningPattern> similarPatterns = findSimilarPatterns(originalText, userId);
        
        // Filtrar apenas padrões processados (mesmo com baixa confiança)
        similarPatterns = similarPatterns.stream()
            .filter(p -> p.getIsProcessed() != null && p.getIsProcessed())
            .sorted((a, b) -> Double.compare(
                b.getConfidenceScore() != null ? b.getConfidenceScore() : 0.0,
                a.getConfidenceScore() != null ? a.getConfidenceScore() : 0.0
            ))
            .limit(5)
            .collect(Collectors.toList());
        
        if (similarPatterns.isEmpty()) {
            return null;
        }
        
        // Retornar o mais similar e com maior confiança
        String normalizedInput = normalizedText.toLowerCase().trim();
        AiLearningPattern bestMatch = null;
        double bestSimilarity = 0.0;
        
        for (AiLearningPattern pattern : similarPatterns) {
            String normalizedPattern = pattern.getNormalizedText() != null ? 
                pattern.getNormalizedText().toLowerCase().trim() : "";
            
            double similarity = calculateSimilarity(normalizedInput, normalizedPattern);
            
            if (similarity > bestSimilarity && similarity >= 0.5) { // Pelo menos 50% similar
                bestSimilarity = similarity;
                bestMatch = pattern;
            }
        }
        
        if (bestMatch != null) {
            System.out.println("Usando padrão aprendido como fallback (similaridade: " + bestSimilarity + 
                ", confiança: " + bestMatch.getConfidenceScore() + ")");
        }
        
        return bestMatch;
    }
    
    /**
     * Processa um texto do usuário usando OpenAI para aprender padrões
     */
    public AiLearningPattern processTextWithAI(String originalText, Long userId) {
        try {
            // Normalizar texto
            String normalizedText = normalizeText(originalText);
            
            // Buscar padrões históricos do usuário para contexto
            List<AiLearningPattern> historicalPatterns = patternRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(20) // Últimos 20 padrões
                .collect(Collectors.toList());
            
            // Buscar categorias do usuário
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            String categoriesContext = userCategories.stream()
                .map(c -> String.format("- %s (%s)", c.getName(), c.getType().name()))
                .collect(Collectors.joining("\n"));
            
            // Construir prompt para OpenAI
            String prompt = buildLearningPrompt(originalText, normalizedText, historicalPatterns, categoriesContext);
            
            // Chamar OpenAI
            String aiResponse = callOpenAI(prompt);
            
            // Parsear resposta
            Map<String, Object> extractedData = parseAIResponse(aiResponse);
            
            // Criar e salvar padrão
            AiLearningPattern pattern = new AiLearningPattern();
            pattern.setOriginalText(originalText);
            pattern.setNormalizedText(normalizedText);
            pattern.setUser(new User());
            pattern.getUser().setId(userId);
            
            // Preencher dados extraídos
            if (extractedData.containsKey("type")) {
                pattern.setTransactionType(extractedData.get("type").toString());
            }
            if (extractedData.containsKey("amount")) {
                pattern.setAmount(new BigDecimal(extractedData.get("amount").toString()));
            }
            if (extractedData.containsKey("amountPerInstallment")) {
                pattern.setAmountPerInstallment(new BigDecimal(extractedData.get("amountPerInstallment").toString()));
            }
            if (extractedData.containsKey("installments")) {
                pattern.setInstallments(Integer.parseInt(extractedData.get("installments").toString()));
            }
            if (extractedData.containsKey("description")) {
                pattern.setDescription(extractedData.get("description").toString());
            }
            if (extractedData.containsKey("date")) {
                pattern.setTransactionDate(LocalDate.parse(extractedData.get("date").toString()));
            }
            if (extractedData.containsKey("categoryId")) {
                pattern.setCategoryId(Long.parseLong(extractedData.get("categoryId").toString()));
            }
            if (extractedData.containsKey("categoryName")) {
                pattern.setCategoryName(extractedData.get("categoryName").toString());
            }
            if (extractedData.containsKey("confidence")) {
                pattern.setConfidenceScore(Double.parseDouble(extractedData.get("confidence").toString()));
            }
            if (extractedData.containsKey("patterns")) {
                pattern.setLearnedPatterns(objectMapper.writeValueAsString(extractedData.get("patterns")));
            }
            if (extractedData.containsKey("notes")) {
                pattern.setProcessingNotes(extractedData.get("notes").toString());
            }
            
            pattern.setIsProcessed(true);
            
            return patternRepository.save(pattern);
            
        } catch (Exception e) {
            System.err.println("Erro ao processar texto com IA: " + e.getMessage());
            e.printStackTrace();
            
            // Em caso de erro, salvar padrão sem processamento
            AiLearningPattern pattern = new AiLearningPattern();
            pattern.setOriginalText(originalText);
            pattern.setNormalizedText(normalizeText(originalText));
            pattern.setUser(new User());
            pattern.getUser().setId(userId);
            pattern.setIsProcessed(false);
            pattern.setProcessingNotes("Erro ao processar: " + e.getMessage());
            return patternRepository.save(pattern);
        }
    }
    
    /**
     * Treina o modelo com padrões históricos não processados
     */
    public void trainWithHistoricalPatterns() {
        List<AiLearningPattern> unprocessed = patternRepository.findByIsProcessedFalse();
        System.out.println("Processando " + unprocessed.size() + " padrões não processados...");
        
        for (AiLearningPattern pattern : unprocessed) {
            try {
                processTextWithAI(pattern.getOriginalText(), pattern.getUser().getId());
            } catch (Exception e) {
                System.err.println("Erro ao processar padrão " + pattern.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Busca padrões similares para um texto
     */
    public List<AiLearningPattern> findSimilarPatterns(String text, Long userId) {
        String normalizedText = normalizeText(text);
        String[] keywords = normalizedText.split("\\s+");
        
        List<AiLearningPattern> similar = new ArrayList<>();
        for (String keyword : keywords) {
            if (keyword.length() > 3) { // Ignorar palavras muito curtas
                List<AiLearningPattern> found = patternRepository.findSimilarPatterns(
                    new User() {{ setId(userId); }}, 
                    keyword
                );
                similar.addAll(found);
            }
        }
        
        // Remover duplicatas e ordenar por confiança
        return similar.stream()
            .distinct()
            .sorted((a, b) -> Double.compare(
                b.getConfidenceScore() != null ? b.getConfidenceScore() : 0.0,
                a.getConfidenceScore() != null ? a.getConfidenceScore() : 0.0
            ))
            .limit(10)
            .collect(Collectors.toList());
    }
    
    private String buildLearningPrompt(String originalText, String normalizedText, 
                                       List<AiLearningPattern> historicalPatterns, 
                                       String categoriesContext) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Você é um assistente especializado em extrair informações financeiras de textos em português brasileiro.\n\n");
        prompt.append("TAREFA: Analise o texto do usuário e extraia todas as informações relevantes para criar uma transação financeira.\n\n");
        
        prompt.append("TEXTO ORIGINAL: ").append(originalText).append("\n\n");
        prompt.append("TEXTO NORMALIZADO: ").append(normalizedText).append("\n\n");
        
        // Adicionar contexto histórico
        if (!historicalPatterns.isEmpty()) {
            prompt.append("PADRÕES HISTÓRICOS DO USUÁRIO (aprenda com estes exemplos):\n");
            for (AiLearningPattern p : historicalPatterns) {
                prompt.append(String.format("- Texto: \"%s\" → Tipo: %s, Valor: %s, Descrição: %s\n",
                    p.getOriginalText(),
                    p.getTransactionType(),
                    p.getAmount(),
                    p.getDescription()
                ));
            }
            prompt.append("\n");
        }
        
        // Adicionar categorias disponíveis
        prompt.append("CATEGORIAS DISPONÍVEIS:\n").append(categoriesContext).append("\n\n");
        
        prompt.append("INSTRUÇÕES:\n");
        prompt.append("1. Identifique o TIPO: EXPENSE (despesa) ou INCOME (receita)\n");
        prompt.append("2. Extraia o VALOR (número decimal)\n");
        prompt.append("3. Se mencionar parcelas, identifique: número de parcelas e se o valor é total ou por parcela\n");
        prompt.append("4. Extraia a DATA (hoje, ontem, segunda, dia 15, 15/11, etc.)\n");
        prompt.append("5. Identifique a CATEGORIA mais apropriada (use o ID da categoria)\n");
        prompt.append("6. Extraia uma DESCRIÇÃO concisa (apenas o nome do item/produto, sem verbos ou valores)\n");
        prompt.append("7. Calcule um SCORE de confiança (0.0 a 1.0)\n");
        prompt.append("8. Identifique PADRÕES únicos no texto que podem ser reutilizados\n\n");
        
        prompt.append("RESPONDA APENAS EM JSON com este formato:\n");
        prompt.append("{\n");
        prompt.append("  \"type\": \"EXPENSE\" ou \"INCOME\",\n");
        prompt.append("  \"amount\": valor_total (se parcelado, use valor total),\n");
        prompt.append("  \"amountPerInstallment\": valor_por_parcela (se parcelado),\n");
        prompt.append("  \"installments\": número_parcelas (null se não parcelado),\n");
        prompt.append("  \"description\": \"descrição_concisa\",\n");
        prompt.append("  \"date\": \"YYYY-MM-DD\",\n");
        prompt.append("  \"categoryId\": id_categoria (null se não encontrada),\n");
        prompt.append("  \"categoryName\": \"nome_categoria\",\n");
        prompt.append("  \"confidence\": 0.0-1.0,\n");
        prompt.append("  \"patterns\": [\"padrão1\", \"padrão2\"],\n");
        prompt.append("  \"notes\": \"observações sobre o processamento\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private String callOpenAI(String prompt) {
        if (openAiApiKey == null || openAiApiKey.isEmpty()) {
            throw new RuntimeException("OpenAI API key não configurada");
        }
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1000);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Você é um assistente especializado em análise de textos financeiros. Sempre responda APENAS em JSON válido, sem texto adicional.");
        messages.add(systemMessage);
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        
        try {
            Mono<String> responseMono = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class);
            
            String response = responseMono.block();
            
            // Extrair conteúdo da resposta
            JsonNode jsonNode = objectMapper.readTree(response);
            String content = jsonNode.get("choices").get(0).get("message").get("content").asText();
            
            return content;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao chamar OpenAI: " + e.getMessage(), e);
        }
    }
    
    private Map<String, Object> parseAIResponse(String aiResponse) {
        try {
            // Limpar resposta (remover markdown code blocks se houver)
            String cleanedResponse = aiResponse.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();
            
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            Map<String, Object> result = new HashMap<>();
            
            if (jsonNode.has("type")) {
                result.put("type", jsonNode.get("type").asText());
            }
            if (jsonNode.has("amount")) {
                result.put("amount", jsonNode.get("amount").asDouble());
            }
            if (jsonNode.has("amountPerInstallment")) {
                result.put("amountPerInstallment", jsonNode.get("amountPerInstallment").asDouble());
            }
            if (jsonNode.has("installments")) {
                result.put("installments", jsonNode.get("installments").asInt());
            }
            if (jsonNode.has("description")) {
                result.put("description", jsonNode.get("description").asText());
            }
            if (jsonNode.has("date")) {
                result.put("date", jsonNode.get("date").asText());
            }
            if (jsonNode.has("categoryId")) {
                result.put("categoryId", jsonNode.get("categoryId").asLong());
            }
            if (jsonNode.has("categoryName")) {
                result.put("categoryName", jsonNode.get("categoryName").asText());
            }
            if (jsonNode.has("confidence")) {
                result.put("confidence", jsonNode.get("confidence").asDouble());
            }
            if (jsonNode.has("patterns")) {
                List<String> patterns = new ArrayList<>();
                jsonNode.get("patterns").forEach(p -> patterns.add(p.asText()));
                result.put("patterns", patterns);
            }
            if (jsonNode.has("notes")) {
                result.put("notes", jsonNode.get("notes").asText());
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Erro ao parsear resposta da IA: " + e.getMessage());
            System.err.println("Resposta recebida: " + aiResponse);
            return new HashMap<>();
        }
    }
    
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
            .replaceAll("[^a-záàâãéêíóôõúç0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
}

