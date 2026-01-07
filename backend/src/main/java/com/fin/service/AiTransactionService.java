package com.fin.service;

import com.fin.dto.AiTransactionResponse;
import com.fin.dto.TransactionDto;
import com.fin.model.Category;
import com.fin.model.TransactionType;
import com.fin.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiTransactionService {
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private AiLearningService aiLearningService;
    
    @Value("${openai.api.key:}")
    private String openAiApiKey;
    
    @Value("${ai.use-learning:true}")
    private Boolean useLearning;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public AiTransactionService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Processa texto em linguagem natural e cria uma transação ou retorna informações faltantes
     * Exemplos:
     * - "gastei com mercado o valor de 50 reais"
     * - "fiz uma compra de uma televisao no valor de 1500 em 10x"
     * - "recebi um valor de 500 reais"
     * - "adquiri um livro de 80 reais ontem"
     */
    public AiTransactionResponse processTextAndCreateTransaction(String text, Long userId) {
        String originalText = text.trim();
        String normalizedText = normalizeText(text);
        
        System.out.println("DEBUG processTextAndCreateTransaction: Texto original: " + originalText);
        System.out.println("DEBUG processTextAndCreateTransaction: Texto normalizado: " + normalizedText);
        
        // ESTRATÉGIA 1: Verificar se já existe padrão aprendido no banco (evita chamar OpenAI)
        if (useLearning) {
            com.fin.model.AiLearningPattern learnedPattern = aiLearningService.findSimilarLearnedPattern(
                originalText, normalizedText, userId);
            
            if (learnedPattern != null) {
                System.out.println("Usando padrão aprendido do banco (sem chamar OpenAI)");
                System.out.println("DEBUG: Padrão encontrado - Amount: " + learnedPattern.getAmount() + 
                    ", AmountPerInstallment: " + learnedPattern.getAmountPerInstallment());
                return createTransactionFromPattern(learnedPattern, originalText, normalizedText, userId);
            }
        }
        
        // ESTRATÉGIA 2: Tentar usar OpenAI para aprender novo padrão (se habilitado e chave configurada)
        if (useLearning && openAiApiKey != null && !openAiApiKey.isEmpty()) {
            try {
                System.out.println("Padrão não encontrado no banco. Chamando OpenAI para aprender...");
                return processWithAIAndLearning(originalText, normalizedText, userId);
            } catch (Exception e) {
                System.err.println("Erro ao processar com OpenAI: " + e.getMessage());
                
                // ESTRATÉGIA 3: Tentar usar padrões aprendidos existentes como fallback
                com.fin.model.AiLearningPattern fallbackPattern = aiLearningService.findBestFallbackPattern(
                    originalText, normalizedText, userId);
                
                if (fallbackPattern != null) {
                    System.out.println("Usando padrão aprendido como fallback após erro na OpenAI");
                    return createTransactionFromPattern(fallbackPattern, originalText, normalizedText, userId);
                }
                
                // Continuar com método tradicional como último recurso
                System.err.println("Usando padrões hardcoded como último recurso");
            }
        }
        
        // ESTRATÉGIA 4: Usar padrões hardcoded (método tradicional)
        return processWithHardcodedPatterns(originalText, normalizedText, userId);
    }
    
    /**
     * Cria transação a partir de um padrão aprendido
     */
    private AiTransactionResponse createTransactionFromPattern(com.fin.model.AiLearningPattern pattern, String currentOriginalText, String currentNormalizedText, Long userId) {
        // Extrair dados do padrão aprendido
        TransactionType type = pattern.getTransactionType() != null && 
            pattern.getTransactionType().equals("INCOME") ? TransactionType.INCOME : TransactionType.EXPENSE;
        
        // IMPORTANTE: Extrair parcelas do texto atual primeiro
        Integer installments = extractInstallments(currentNormalizedText);
        if (installments == null || installments <= 1) {
            // Se não encontrou no texto atual, usar do padrão
            installments = pattern.getInstallments();
        } else {
            System.out.println("DEBUG: Usando número de parcelas do texto atual: " + installments);
        }
        
        BigDecimal amount = pattern.getAmountPerInstallment() != null ? 
            pattern.getAmountPerInstallment() : pattern.getAmount();
        
        // Se amount for null ou zero, tentar extrair do texto atual (não do padrão antigo)
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("AVISO: Padrão aprendido não tem valor (ou é zero). Tentando extrair do texto atual...");
            BigDecimal extractedAmount = extractAmount(currentNormalizedText);
            if (extractedAmount != null && extractedAmount.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println("Valor extraído do texto atual: R$ " + extractedAmount);
                amount = extractedAmount;
                
                // Se há parcelas, verificar se é valor total ou valor por parcela
                if (installments != null && installments > 1) {
                    boolean isTotalAmount = isLikelyTotalAmount(extractedAmount, installments, currentNormalizedText);
                    if (isTotalAmount) {
                        // Dividir o valor total pelo número de parcelas
                        BigDecimal amountPerInstallment = extractedAmount.divide(
                            new BigDecimal(installments), 
                            2, 
                            java.math.RoundingMode.HALF_UP
                        );
                        System.out.println("DEBUG: Valor total detectado: R$ " + extractedAmount + 
                            " dividido em " + installments + "x = R$ " + amountPerInstallment + " por parcela");
                        amount = amountPerInstallment;
                    }
                }
            }
        } else if (installments != null && installments > 1) {
            // Se temos valor e parcelas, verificar se o valor precisa ser dividido
            // Se o padrão tem Amount (total) e não tem AmountPerInstallment, dividir
            if (pattern.getAmount() != null && pattern.getAmountPerInstallment() == null) {
                // Provavelmente é valor total, dividir
                BigDecimal amountPerInstallment = pattern.getAmount().divide(
                    new BigDecimal(installments), 
                    2, 
                    java.math.RoundingMode.HALF_UP
                );
                System.out.println("DEBUG: Dividindo valor total do padrão: R$ " + pattern.getAmount() + 
                    " em " + installments + "x = R$ " + amountPerInstallment + " por parcela");
                amount = amountPerInstallment;
            }
        }
        
        String description = pattern.getDescription();
        
        // Aplicar formatação Title Case na descrição (se vier do padrão aprendido)
        if (description != null && !description.trim().isEmpty()) {
            description = capitalizeWords(description.trim());
        }
        
        Long categoryId = pattern.getCategoryId();
        
        // IMPORTANTE: Verificar se o texto original atual menciona data
        // Se não mencionar, ignorar a data do padrão aprendido e pedir para o usuário informar
        // Usar o texto ATUAL do usuário, não o texto do padrão aprendido
        LocalDate extractedDateFromText = extractDate(currentNormalizedText, currentOriginalText);
        
        // Verificar informações faltantes
        List<AiTransactionResponse.MissingInfo> missingInfo = new ArrayList<>();
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            missingInfo.add(new AiTransactionResponse.MissingInfo(
                "amount", "Valor da transação",
                "Por favor, informe o valor. Ex: '50 reais', 'R$ 1500', 'valor de 300'"
            ));
        }
        
        // Verificar se precisa de data (sempre pedir se não mencionou no texto atual)
        // Ignorar data do padrão aprendido se o texto atual não menciona data
        if (extractedDateFromText == null) {
            // Não mencionou data no texto atual, pedir ao usuário
            if (installments != null && installments > 1) {
                missingInfo.add(new AiTransactionResponse.MissingInfo(
                    "startDate", "Data de início das parcelas",
                    "Por favor, informe quando começa o pagamento. Ex: 'dia 15', 'no dia 10/12', 'hoje', 'ontem'"
                ));
            } else {
                missingInfo.add(new AiTransactionResponse.MissingInfo(
                    "date", "Data da transação",
                    "Por favor, informe a data. Ex: 'hoje', 'ontem', 'dia 15', '15/11', 'amanhã'"
                ));
            }
        }
        
        if (!missingInfo.isEmpty()) {
            StringBuilder question = new StringBuilder();
            question.append("Para criar sua transação, preciso de mais algumas informações:\n\n");
            for (AiTransactionResponse.MissingInfo info : missingInfo) {
                question.append("• ").append(info.getDescription()).append(": ").append(info.getSuggestion()).append("\n");
            }
            return AiTransactionResponse.needsInfo("Informações faltantes", missingInfo, question.toString());
        }
        
        // Criar transação (apenas se todas as informações estiverem presentes)
        // Neste ponto, sabemos que extractedDateFromText não é null (verificado acima)
        if (extractedDateFromText == null) {
            throw new RuntimeException("Data não pode ser null neste ponto");
        }
        
        TransactionDto dto = new TransactionDto();
        dto.setDescription(description != null ? description : "Compra");
        dto.setAmount(amount);
        dto.setType(type.name());
        
        // Usar data extraída do texto (já verificamos que existe acima)
        LocalDate effectiveDate = extractedDateFromText;
        LocalDate today = LocalDate.now();
        
        dto.setDueDate(effectiveDate);
        dto.setTransactionDate(effectiveDate);
        dto.setIsPaid(effectiveDate.isBefore(today) || effectiveDate.equals(today));
        dto.setTotalInstallments(installments != null ? installments : 1);
        
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                com.fin.dto.CategoryDto categoryDto = new com.fin.dto.CategoryDto();
                categoryDto.setId(category.getId());
                categoryDto.setName(category.getName());
                categoryDto.setIcon(category.getIcon());
                categoryDto.setColor(category.getColor());
                categoryDto.setType(category.getType().name());
                dto.setCategory(categoryDto);
            }
        }
        
        // ANTES DA CONFIRMAÇÃO: Verificar se precisa de categoria (apenas para despesas)
        // Se for despesa e não tiver categoria, pedir categoria primeiro
        if (type == TransactionType.EXPENSE && dto.getCategory() == null) {
            // Buscar categorias de despesa disponíveis
            List<com.fin.dto.CategoryDto> availableCategories = categoryService.getUserCategoriesByType(userId, TransactionType.EXPENSE);
            String message = "Qual categoria para esta despesa?";
            return AiTransactionResponse.needsCategory(dto, availableCategories, message);
        }
        
        // Retornar confirmação ao invés de criar diretamente
        // Calcular valor total correto para parcelas
        BigDecimal totalAmount = null;
        if (installments != null && installments > 1 && amount != null) {
            // Se temos o valor total no padrão, usar ele
            if (pattern.getAmount() != null && pattern.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalAmount = pattern.getAmount();
            } else {
                // Se não temos, calcular: valor por parcela * número de parcelas
                totalAmount = amount.multiply(new BigDecimal(installments));
            }
        }
        
        String confirmationMessage = "Por favor, confirme os dados da transação:\n\n";
        confirmationMessage += String.format("• Descrição: %s\n", dto.getDescription());
        if (installments != null && installments > 1 && totalAmount != null) {
            // Para parcelas, mostrar valor por parcela e total
            confirmationMessage += String.format("• Valor por parcela: R$ %.2f\n", amount);
            confirmationMessage += String.format("• Parcelas: %dx de R$ %.2f (Total: R$ %.2f)\n", 
                installments, amount, totalAmount);
        } else {
            confirmationMessage += String.format("• Valor: R$ %.2f\n", amount);
        }
        confirmationMessage += String.format("• Data: %s\n", formatDate(effectiveDate));
        confirmationMessage += String.format("• Tipo: %s\n", type == TransactionType.INCOME ? "Receita" : "Despesa");
        if (dto.getCategory() != null) {
            confirmationMessage += String.format("• Categoria: %s\n", dto.getCategory().getName());
        }
        confirmationMessage += "\nOs dados estão corretos?";
        
        return AiTransactionResponse.needsConfirmation(dto, confirmationMessage);
    }
    
    /**
     * Processa usando OpenAI com aprendizado do banco de dados
     */
    private AiTransactionResponse processWithAIAndLearning(String originalText, String normalizedText, Long userId) {
        System.out.println("DEBUG: processWithAIAndLearning - Texto original: " + originalText);
        
        // Processar com OpenAI
        com.fin.model.AiLearningPattern pattern = aiLearningService.processTextWithAI(originalText, userId);
        
        if (pattern == null || !pattern.getIsProcessed()) {
            System.out.println("DEBUG: OpenAI não processou ou falhou. Tentando fallback...");
            // Se falhou, tentar usar padrões aprendidos como fallback
            com.fin.model.AiLearningPattern fallbackPattern = aiLearningService.findBestFallbackPattern(
                originalText, normalizedText, userId);
            
            if (fallbackPattern != null) {
                System.out.println("Usando padrão aprendido como fallback após falha no OpenAI");
                return createTransactionFromPattern(fallbackPattern, originalText, normalizedText, userId);
            }
            
            // Se não há fallback, usar padrões hardcoded
            System.out.println("Usando padrões hardcoded como último recurso");
            return processWithHardcodedPatterns(originalText, normalizedText, userId);
        }
        
        // Verificar se o padrão aprendido tem valor válido
        BigDecimal patternAmount = pattern.getAmountPerInstallment() != null ? 
            pattern.getAmountPerInstallment() : pattern.getAmount();
        
        if (patternAmount == null || patternAmount.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("DEBUG: Padrão aprendido não tem valor válido. Tentando extrair com método hardcoded...");
            // Se o padrão aprendido não tem valor, tentar extrair com método hardcoded
            BigDecimal extractedAmount = extractAmount(normalizedText);
            if (extractedAmount != null && extractedAmount.compareTo(BigDecimal.ZERO) > 0) {
                System.out.println("DEBUG: Valor extraído com método hardcoded: R$ " + extractedAmount);
                // Atualizar o padrão com o valor extraído
                if (pattern.getAmountPerInstallment() == null) {
                    pattern.setAmount(extractedAmount);
                } else {
                    pattern.setAmountPerInstallment(extractedAmount);
                }
            }
        }
        
        // Usar padrão aprendido com sucesso
        System.out.println("DEBUG: Usando padrão aprendido da OpenAI");
        return createTransactionFromPattern(pattern, originalText, normalizedText, userId);
    }
    
    /**
     * Processa usando padrões hardcoded (método tradicional)
     */
    private AiTransactionResponse processWithHardcodedPatterns(String originalText, String normalizedText, Long userId) {
        
        // Detectar tipo (receita ou despesa)
        TransactionType type = detectTransactionType(normalizedText);
        
        // Extrair número de parcelas (se houver) - ANTES do valor para detectar se é total ou parcela
        Integer installments = extractInstallments(normalizedText);
        
        // Extrair valor
        BigDecimal amount = extractAmount(normalizedText);
        BigDecimal originalAmount = amount; // Salvar valor original para mensagens e descrição
        
        // Se houver parcelas, verificar se o valor é o total ou o valor da parcela
        // Padrão comum: "1500 em 9x" = total de 1500 dividido em 9 parcelas
        // Padrão raro: "166,67 em 9x" = valor da parcela já dividido
        if (installments != null && installments > 1 && amount != null) {
            // Verificar se o valor mencionado parece ser um total (números grandes, geralmente inteiros)
            // ou um valor de parcela (números com decimais, valores menores)
            boolean isTotalAmount = isLikelyTotalAmount(amount, installments, normalizedText);
            
            if (isTotalAmount) {
                // Dividir o valor total pelo número de parcelas
                BigDecimal amountPerInstallment = amount.divide(
                    new BigDecimal(installments), 
                    2, 
                    java.math.RoundingMode.HALF_UP
                );
                System.out.println("Valor total detectado: R$ " + amount + 
                    " dividido em " + installments + "x = R$ " + amountPerInstallment + " por parcela");
                amount = amountPerInstallment; // amount agora é o valor por parcela
            } else {
                System.out.println("Valor de parcela detectado: R$ " + amount + " por parcela em " + installments + "x");
                originalAmount = amount.multiply(new BigDecimal(installments)); // Calcular total para mensagens
            }
        }
        
        // Extrair data (se mencionada)
        LocalDate extractedDate = extractDate(normalizedText, originalText);
        
        // Detectar categoria
        Long categoryId = detectCategory(normalizedText, type, userId);
        
        // Extrair descrição - melhorar extração (usar valor original para remoção)
        String description = extractDescription(originalText, normalizedText, originalAmount, installments);
        
        // Verificar informações faltantes
        List<AiTransactionResponse.MissingInfo> missingInfo = new ArrayList<>();
        
        if (amount == null) {
            missingInfo.add(new AiTransactionResponse.MissingInfo(
                "amount",
                "Valor da transação",
                "Por favor, informe o valor. Ex: '50 reais', 'R$ 1500', 'valor de 300'"
            ));
        }
        
        // Verificar se precisa de data (sempre pedir se não mencionou)
        if (extractedDate == null) {
            if (installments != null && installments > 1) {
                missingInfo.add(new AiTransactionResponse.MissingInfo(
                    "startDate",
                    "Data de início das parcelas",
                    "Por favor, informe quando começa o pagamento. Ex: 'dia 15', 'no dia 10/12', 'hoje', 'ontem', 'começando em janeiro'"
                ));
            } else {
                missingInfo.add(new AiTransactionResponse.MissingInfo(
                    "date",
                    "Data da transação",
                    "Por favor, informe a data. Ex: 'hoje', 'ontem', 'dia 15', '15/11', 'amanhã'"
                ));
            }
        }
        
        // Se há informações faltantes, retornar perguntas
        if (!missingInfo.isEmpty()) {
            StringBuilder question = new StringBuilder();
            question.append("Para criar sua transação, preciso de mais algumas informações:\n\n");
            
            for (int i = 0; i < missingInfo.size(); i++) {
                AiTransactionResponse.MissingInfo info = missingInfo.get(i);
                question.append("• ").append(info.getDescription()).append(": ").append(info.getSuggestion());
                if (i < missingInfo.size() - 1) {
                    question.append("\n");
                }
            }
            
            if (installments != null && installments > 1) {
                question.append("\n\n💡 Dica: Para compras parceladas, você pode informar assim:\n");
                question.append("\"Fiz uma compra de R$ ").append(amount != null ? amount : "[valor]");
                question.append(" em ").append(installments).append("x começando no dia 15\"");
            }
            
            return AiTransactionResponse.needsInfo(
                "Informações faltantes",
                missingInfo,
                question.toString()
            );
        }
        
        // Todas as informações estão presentes, criar transação
        // Neste ponto, sabemos que extractedDate não é null (verificado acima)
        if (extractedDate == null) {
            throw new RuntimeException("Data não pode ser null neste ponto");
        }
        
        TransactionDto dto = new TransactionDto();
        dto.setDescription(description);
        dto.setAmount(amount);
        dto.setType(type.name());
        
        // Usar data extraída (já verificamos que existe acima)
        LocalDate effectiveDate = extractedDate;
        LocalDate today = LocalDate.now();
        
        dto.setDueDate(effectiveDate);
        dto.setTransactionDate(effectiveDate);
        
        // Se a data for hoje ou no passado, marcar como pago/recebido automaticamente
        boolean isTodayOrPast = effectiveDate.isBefore(today) || effectiveDate.equals(today);
        dto.setIsPaid(isTodayOrPast);
        
        dto.setTotalInstallments(installments != null ? installments : 1);
        
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                com.fin.dto.CategoryDto categoryDto = new com.fin.dto.CategoryDto();
                categoryDto.setId(category.getId());
                categoryDto.setName(category.getName());
                categoryDto.setIcon(category.getIcon());
                categoryDto.setColor(category.getColor());
                categoryDto.setType(category.getType().name());
                dto.setCategory(categoryDto);
            }
        }
        
        // ANTES DA CONFIRMAÇÃO: Verificar se precisa de categoria (apenas para despesas)
        // Se for despesa e não tiver categoria, pedir categoria primeiro
        if (type == TransactionType.EXPENSE && dto.getCategory() == null) {
            // Buscar categorias de despesa disponíveis
            List<com.fin.dto.CategoryDto> availableCategories = categoryService.getUserCategoriesByType(userId, TransactionType.EXPENSE);
            String message = "Qual categoria para esta despesa?";
            return AiTransactionResponse.needsCategory(dto, availableCategories, message);
        }
        
        // Retornar confirmação ao invés de criar diretamente
        // Usar originalAmount (valor total) para calcular corretamente
        BigDecimal totalAmount = null;
        if (installments != null && installments > 1) {
            // originalAmount já contém o valor total (antes da divisão)
            totalAmount = originalAmount;
        }
        
        String confirmationMessage = "Por favor, confirme os dados da transação:\n\n";
        confirmationMessage += String.format("• Descrição: %s\n", dto.getDescription());
        if (installments != null && installments > 1 && totalAmount != null) {
            // Para parcelas, mostrar valor por parcela e total
            confirmationMessage += String.format("• Valor por parcela: R$ %.2f\n", amount);
            confirmationMessage += String.format("• Parcelas: %dx de R$ %.2f (Total: R$ %.2f)\n", 
                installments, amount, totalAmount);
        } else {
            confirmationMessage += String.format("• Valor: R$ %.2f\n", amount);
        }
        confirmationMessage += String.format("• Data: %s\n", formatDate(effectiveDate));
        confirmationMessage += String.format("• Tipo: %s\n", type == TransactionType.INCOME ? "Receita" : "Despesa");
        if (dto.getCategory() != null) {
            confirmationMessage += String.format("• Categoria: %s\n", dto.getCategory().getName());
        }
        confirmationMessage += "\nOs dados estão corretos?";
        
        return AiTransactionResponse.needsConfirmation(dto, confirmationMessage);
    }
    
    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
    
    private String normalizeText(String text) {
        return text.toLowerCase()
                .replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                .replace("ú", "u").replace("ü", "u")
                .replace("ç", "c");
    }
    
    private TransactionType detectTransactionType(String text) {
        // Palavras-chave para receita
        String[] incomeKeywords = {
            "recebi", "ganhei", "entrou", "salario", "salário", "renda", 
            "pagamento recebido", "venda", "vendi", "lucro"
        };
        
        // Palavras-chave para despesa
        String[] expenseKeywords = {
            "gastei", "comprei", "paguei", "despesa", "compra", "gasto",
            "fiz uma compra", "comprei uma", "paguei um", "paguei uma",
            "adquiri", "adquirir", "adquiriu", "adquirimos"
        };
        
        for (String keyword : incomeKeywords) {
            if (text.contains(keyword)) {
                return TransactionType.INCOME;
            }
        }
        
        for (String keyword : expenseKeywords) {
            if (text.contains(keyword)) {
                return TransactionType.EXPENSE;
            }
        }
        
        // Padrão: se não detectar, assume despesa
        return TransactionType.EXPENSE;
    }
    
    private BigDecimal extractAmount(String text) {
        System.out.println("DEBUG extractAmount: Procurando valor no texto: " + text);
        
        // Padrões para encontrar valores monetários (em ordem de prioridade)
        Pattern[] patterns = {
            // "de 80 reais", "de 50 reais" - padrão muito comum: "adquiri um livro de 80 reais"
            Pattern.compile("de\\s+(\\d+(?:[,\\.]\\d{2})?)\\s+reais?", Pattern.CASE_INSENSITIVE),
            // "50 reais", "50,00 reais", "R$ 50", "R$ 50,00"
            Pattern.compile("(?:r\\$\\s*)?(\\d+(?:[,\\.]\\d{2})?)\\s*(?:reais?|rs?|r\\$)", Pattern.CASE_INSENSITIVE),
            // "valor de 50", "no valor de 1500"
            Pattern.compile("valor\\s+(?:de|)\\s*(\\d+(?:[,\\.]\\d{2})?)", Pattern.CASE_INSENSITIVE),
            // "1500 em 10x" - pegar o valor antes de "em"
            Pattern.compile("(\\d+(?:[,\\.]\\d{2})?)\\s+em\\s+\\d+", Pattern.CASE_INSENSITIVE),
            // Números simples grandes (mais de 10)
            Pattern.compile("(\\d{2,}(?:[,\\.]\\d{2})?)", Pattern.CASE_INSENSITIVE)
        };
        
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = patterns[i];
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String valueStr = matcher.group(1).replace(",", ".");
                try {
                    BigDecimal value = new BigDecimal(valueStr);
                    System.out.println("DEBUG extractAmount: Valor encontrado com padrão " + i + ": R$ " + value);
                    return value;
                } catch (NumberFormatException e) {
                    System.out.println("DEBUG extractAmount: Erro ao converter valor: " + valueStr);
                    continue;
                }
            }
        }
        
        System.out.println("DEBUG extractAmount: Nenhum valor encontrado no texto");
        return null;
    }
    
    private Integer extractInstallments(String text) {
        // Padrões para encontrar parcelas: "10x", "em 10x", "10 vezes", "10 parcelas"
        Pattern[] patterns = {
            Pattern.compile("(\\d+)\\s*x", Pattern.CASE_INSENSITIVE),
            Pattern.compile("em\\s+(\\d+)\\s*x", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s+vezes", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s+parcelas?", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    int installments = Integer.parseInt(matcher.group(1));
                    if (installments > 1) {
                        return installments;
                    }
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        
        return null;
    }
    
    private Long detectCategory(String text, TransactionType type, Long userId) {
        // Buscar categorias do usuário do tipo correto
        List<Category> userCategories = categoryRepository.findByUserId(userId);
        List<Category> filteredCategories = userCategories.stream()
                .filter(c -> c.getType().name().equals(type.name()))
                .collect(java.util.stream.Collectors.toList());
        
        // Mapeamento de palavras-chave para nomes de categoria comuns
        String[][] categoryMappings = {
            {"mercado", "supermercado", "compras", "alimentacao", "alimentação"},
            {"televisao", "tv", "televisão", "eletrodomesticos", "eletrodomésticos"},
            {"restaurante", "comida", "lanche", "jantar"},
            {"transporte", "combustivel", "combustível", "gasolina", "uber", "taxi"},
            {"saude", "saúde", "medico", "médico", "farmacia", "farmácia", "medicamento"},
            {"educacao", "educação", "curso", "escola", "faculdade"},
            {"lazer", "cinema", "viagem", "turismo"},
            {"vestuario", "vestuário", "roupa", "roupas", "calçado"},
            {"moradia", "aluguel", "condominio", "condomínio", "energia", "luz", "agua", "água"},
            {"salario", "salário", "renda", "trabalho"},
            {"venda", "recebimento", "pagamento"}
        };
        
        // Tentar encontrar categoria por palavra-chave
        for (String[] keywords : categoryMappings) {
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    // Procurar categoria com nome similar
                    for (Category category : filteredCategories) {
                        String categoryName = normalizeText(category.getName());
                        if (categoryName.contains(keyword) || keyword.contains(categoryName)) {
                            return category.getId();
                        }
                    }
                }
            }
        }
        
        // Se não encontrou, tentar match parcial com nomes de categorias existentes
        for (Category category : filteredCategories) {
            String categoryName = normalizeText(category.getName());
            for (String word : text.split("\\s+")) {
                if (categoryName.contains(word) && word.length() > 3) {
                    return category.getId();
                }
            }
        }
        
        // Se ainda não encontrou, retornar null (categoria será opcional)
        return null;
    }
    
    /**
     * Extrai uma descrição limpa do texto, removendo valores, parcelas, datas e palavras funcionais
     */
    private String extractDescription(String originalText, String normalizedText, BigDecimal amount, Integer installments) {
        String description = originalText;
        
        // 1. Remover prefixos de ação no início (incluindo formas verbais como "acabei de")
        description = description.replaceAll("^(acabei de|acabei|gastei|comprei|paguei|recebi|ganhei|fiz uma compra|comprei uma|paguei um|paguei uma|fiz|comprei|paguei)\\s+", "");
        
        // Remover "compra de um/uma" especificamente
        description = description.replaceAll("^(compra de um|compra de uma|compra de|compra)\\s+", "");
        
        description = description.replaceAll("^(com|de|uma|um|o|a)\\s+", "");
        
        // Remover "de" antes de verbos (ex: "acabei de gastar")
        description = description.replaceAll("^(de gastar|de comprar|de pagar|de receber)\\s+", "");
        
        // 2. Remover valores monetários e números relacionados
        if (amount != null) {
            // Remover o valor exato mencionado
            String amountStr = amount.toString().replace(".", ",");
            description = description.replaceAll("(?i)" + Pattern.quote(amountStr), "");
            description = description.replaceAll("(?i)r\\$\\s*" + amountStr.replace(",", "[,.]"), "");
        }
        
        // 3. Remover informações de parcelas
        if (installments != null) {
            description = description.replaceAll("(?i)\\s*em\\s+" + installments + "\\s*x", "");
            description = description.replaceAll("(?i)\\s*" + installments + "\\s+vezes", "");
            description = description.replaceAll("(?i)\\s*" + installments + "\\s+parcelas?", "");
            description = description.replaceAll("(?i)\\s*parcelado\\s+em\\s+" + installments, "");
            description = description.replaceAll("(?i)\\s*parcelado", "");
        }
        
        // 4. Remover palavras relacionadas a valores
        description = description.replaceAll("(?i)\\s*(o valor de|no valor de|valor de|reais?|rs?|r\\$|valor|total|de|em)\\s*", " ");
        
        // 5. Remover datas
        description = description.replaceAll("(?i)\\s*(dia|no dia|dia)\\s+\\d+", "");
        description = description.replaceAll("\\s*\\d{1,2}/\\d{1,2}(/\\d{2,4})?", "");
        description = description.replaceAll("(?i)\\s*comecando\\s+(no|em|dia|dia)\\s*", "");
        description = description.replaceAll("(?i)\\s*comecando", "");
        
        // 6. Remover números (valores monetários) - incluir números pequenos também
        description = description.replaceAll("\\s+\\d+([,\\.]\\d{2})?\\s*", " ");
        description = description.replaceAll("\\s*\\d+([,\\.]\\d{2})?\\s*", " ");
        
        // 7. Limpar espaços múltiplos e palavras vazias
        description = description.replaceAll("\\s+", " ").trim();
        
        // Remover palavras funcionais que possam ter sobrado
        description = description.replaceAll("\\b(com|de|uma|um|o|a|no|em|para|gastar|gastei|comprei|paguei|acabei|valor|reais?)\\b", "");
        description = description.replaceAll("\\s+", " ").trim();
        
        // 8. Extrair apenas o nome do produto/item (geralmente antes de "valor" ou "em")
        // Tentar encontrar padrões como "uma televisão", "com mercado", "de uma tv", "com gasolina"
        Pattern[] productPatterns = {
            // "com [produto]" - padrão mais comum: "gastei com gasolina", "comprei com mercado"
            Pattern.compile("\\bcom\\s+([a-záàâãéêíóôõúç]{3,}(?:\\s+[a-záàâãéêíóôõúç]+)*?)(?:\\s+(?:no valor|valor|de|em|r\\$|\\d)|$)", Pattern.CASE_INSENSITIVE),
            // "compra de um/uma [produto]" - remover "compra de um/uma" e pegar o produto
            Pattern.compile("\\bcompra\\s+de\\s+(?:um|uma)\\s+([a-záàâãéêíóôõúç]+(?:\\s+[a-záàâãéêíóôõúç]+)*?)(?:\\s+(?:no valor|valor|de|em|r\\$|\\d)|$)", Pattern.CASE_INSENSITIVE),
            // "uma televisão no valor de"
            Pattern.compile("(?:uma|um|de)\\s+([a-záàâãéêíóôõúç]+(?:\\s+[a-záàâãéêíóôõúç]+)*?)(?:\\s+(?:no valor|valor|em|parcelado|r\\$))", Pattern.CASE_INSENSITIVE),
            // "comprei uma televisão de"
            Pattern.compile("(?:acabei de|acabei|comprei|paguei|gastei|fiz uma compra|comprei uma|paguei um|paguei uma)\\s+(?:uma|um|com|de)?\\s*([a-záàâãéêíóôõúç]+(?:\\s+[a-záàâãéêíóôõúç]+)*?)(?:\\s+(?:de|no valor|valor|em|com|r\\$|\\d)|$)", Pattern.CASE_INSENSITIVE),
            // "televisão no valor de 1500"
            Pattern.compile("\\b([a-záàâãéêíóôõúç]{3,}(?:\\s+[a-záàâãéêíóôõúç]+)*?)\\s+(?:no valor|valor|de|em)\\s+(?:de|r\\$|\\d)", Pattern.CASE_INSENSITIVE)
        };
        
        for (Pattern pattern : productPatterns) {
            Matcher productMatcher = pattern.matcher(originalText);
            if (productMatcher.find()) {
                String product = productMatcher.group(1).trim();
                // Validar que não é apenas uma palavra funcional
                if (product.length() > 2 && product.length() < 50 && 
                    !product.matches("^(com|de|uma|um|o|a|no|em|para|compras|compra|gastar|gastei|comprei|paguei|acabei)$")) {
                    description = product;
                    break; // Usar o primeiro match válido
                }
            }
        }
        
        // Se ainda contém "compra de um/uma", remover manualmente
        description = description.replaceAll("(?i)^compra\\s+de\\s+(?:um|uma)\\s+", "");
        description = description.replaceAll("(?i)^compra\\s+de\\s+", "");
        description = description.replaceAll("(?i)^compra\\s+", "");
        
        // 9. Se ainda está muito longo, tentar pegar apenas as primeiras palavras (até 3 palavras)
        if (description.length() > 30) {
            String[] words = description.split("\\s+");
            if (words.length > 3) {
                description = String.join(" ", java.util.Arrays.copyOf(words, 3));
            }
        }
        
        // 10. Capitalizar primeira letra de cada palavra (Title Case)
        description = capitalizeWords(description);
        
        // 11. Se a descrição ficou muito curta ou vazia, usar uma descrição genérica baseada na categoria
        if (description.trim().length() < 3) {
            description = "Compra"; // Fallback genérico
        }
        
        // 12. Usar OpenAI para corrigir erros de digitação (se API key estiver configurada)
        if (openAiApiKey != null && !openAiApiKey.isEmpty() && description.length() > 2) {
            try {
                String corrected = correctSpellingWithAI(description);
                if (corrected != null && corrected.length() > 2) {
                    description = corrected;
                    // Garantir que a formatação Title Case seja aplicada após correção
                    description = capitalizeWords(description);
                }
            } catch (Exception e) {
                // Se falhar, usar a descrição original
                System.out.println("Erro ao corrigir descrição com IA: " + e.getMessage());
            }
        }
        
        // 13. Garantir formatação final (Title Case) - mesmo que venha da IA
        description = capitalizeWords(description.trim());
        
        return description.trim();
    }
    
    /**
     * Capitaliza a primeira letra de cada palavra (Title Case)
     * Exemplo: "video game" → "Video Game"
     * Exemplo: "VIDEO GAME" → "Video Game"
     * Exemplo: "ViDeO gAmE" → "Video Game"
     */
    private String capitalizeWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // Normalizar: converter tudo para minúscula primeiro, depois capitalizar
        String normalized = text.trim().toLowerCase();
        String[] words = normalized.split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 0) {
                // Capitalizar primeira letra e manter o resto em minúscula
                String firstChar = word.substring(0, 1).toUpperCase();
                String rest = word.length() > 1 ? word.substring(1).toLowerCase() : "";
                result.append(firstChar).append(rest);
                if (i < words.length - 1) {
                    result.append(" ");
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Usa OpenAI para corrigir erros de digitação na descrição
     */
    private String correctSpellingWithAI(String description) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 50);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Você é um corretor ortográfico. Corrija apenas erros de digitação e ortografia. " +
                    "Mantenha o mesmo significado e formato. Retorne APENAS a palavra corrigida, sem explicações. " +
                    "Use Title Case (primeira letra de cada palavra em maiúscula).");
            
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Corrija a ortografia desta palavra/item: \"" + description + "\". " +
                    "Retorne APENAS a palavra corrigida no formato Title Case (ex: Video Game, Televisão, Gasolina).");
            
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
            
            // Parse da resposta JSON usando ObjectMapper
            if (response != null) {
                JsonNode jsonNode = objectMapper.readTree(response);
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    JsonNode message = firstChoice.get("message");
                    if (message != null) {
                        JsonNode content = message.get("content");
                        if (content != null) {
                            String corrected = content.asText().trim();
                            // Remover aspas e quebras de linha
                            corrected = corrected.replace("\"", "").replace("\n", "").trim();
                            if (corrected.length() > 0 && !corrected.equalsIgnoreCase("null")) {
                                return capitalizeWords(corrected);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao chamar OpenAI para correção: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Determina se o valor mencionado é um valor total (a ser dividido) ou valor da parcela
     * Heurística: valores grandes e inteiros geralmente são totais
     */
    private boolean isLikelyTotalAmount(BigDecimal amount, Integer installments, String normalizedText) {
        // Verificar se o valor tem decimais
        boolean hasDecimals = amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0;
        
        // Calcular o valor por parcela
        BigDecimal amountPerInstallment = amount.divide(new BigDecimal(installments), 4, java.math.RoundingMode.HALF_UP);
        
        // Verificar se o valor por parcela tem muitos decimais (mais de 2 casas significativas após a vírgula)
        // Exemplo: 1500 / 9 = 166.6666... (muitos decimais)
        // Exemplo: 166.67 / 9 = 18.5188... (também muitos decimais, mas se o usuário digitou 166.67, provavelmente é parcela)
        BigDecimal remainder = amountPerInstallment.remainder(BigDecimal.ONE);
        int decimalPlaces = remainder.scale();
        boolean hasRepeatingDecimals = decimalPlaces > 2 && remainder.compareTo(BigDecimal.ZERO) > 0;
        
        // Heurística principal:
        // 1. Se o valor NÃO tem decimais E o texto menciona "em Xx" ou "parcelado", é TOTAL
        // 2. Se o valor TEM decimais específicos (ex: 166,67) E o texto menciona "em Xx", 
        //    verificar se dividir resultaria em valor "redondo" (não muitos decimais)
        // 3. Se dividir resultaria em muitos decimais, provavelmente o valor já é da parcela
        
        // Padrão mais comum: "1500 em 9x" = total
        if (!hasDecimals && normalizedText.contains(" em ")) {
            return true;
        }
        
        // Padrão: "1500 parcelado em 9x" = total
        if (!hasDecimals && normalizedText.contains("parcelado")) {
            return true;
        }
        
        // Padrão: "valor de 1500 em 9x" = total
        if (normalizedText.contains("valor") && !hasDecimals) {
            return true;
        }
        
        // Se o valor tem decimais específicos (ex: 166,67) e menciona "em Xx",
        // verificar se dividir resultaria em um valor "estranho"
        // Se resultaria em muitos decimais, provavelmente o valor mencionado JÁ é da parcela
        if (hasDecimals && hasRepeatingDecimals) {
            // Se o valor dividido tem muitos decimais, provavelmente o valor original já é da parcela
            return false;
        }
        
        // Por padrão, se menciona "em Xx" e o valor é grande (>100), assumir que é total
        if (normalizedText.contains(" em ") && amount.compareTo(new BigDecimal("100")) > 0) {
            return true;
        }
        
        // Padrão padrão: assumir que é total (mais comum)
        return true;
    }
    
    /**
     * Extrai data do texto em português
     * Suporta formatos como: "dia 15", "15/12", "15/12/2024", "no dia 10", "começando em janeiro"
     */
    private LocalDate extractDate(String normalizedText, String originalText) {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        
        // Padrão 0: "ontem"
        if (normalizedText.contains("ontem")) {
            return today.minusDays(1);
        }
        
        // Padrão 0.5: "anteontem" ou "ante-ontem"
        if (normalizedText.contains("anteontem") || normalizedText.contains("ante-ontem")) {
            return today.minusDays(2);
        }
        
        // Padrão 0.6: Dias da semana (segunda, terça, quarta, etc.)
        String[] weekDays = {"domingo", "segunda", "terça", "terca", "quarta", "quinta", "sexta", "sábado", "sabado"};
        int[] dayOfWeekValues = {7, 1, 2, 2, 3, 4, 5, 6, 6}; // Java DayOfWeek: 1=Monday, 7=Sunday
        int todayDayOfWeek = today.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        
        // Verificar se há contexto de passado (gastei, paguei, comprei, recebi)
        boolean mentionsPastVerb = normalizedText.contains("gastei") || 
                                 normalizedText.contains("paguei") || 
                                 normalizedText.contains("comprei") ||
                                 normalizedText.contains("recebi") ||
                                 normalizedText.contains("gastei com") ||
                                 normalizedText.contains("paguei com");
        
        for (int i = 0; i < weekDays.length; i++) {
            if (normalizedText.contains(weekDays[i])) {
                int targetDayOfWeek = dayOfWeekValues[i];
                
                // Calcular dias até a última ocorrência desse dia da semana
                int daysBack = todayDayOfWeek - targetDayOfWeek;
                
                // Se o resultado for negativo ou zero, significa que o dia já passou ou é hoje
                if (daysBack <= 0) {
                    daysBack += 7; // Adicionar 7 dias para pegar a semana passada
                }
                
                // Se daysBack == 0, significa que é hoje (mas não deveria acontecer após o += 7)
                // Se mencionou verbo no passado, sempre assumir semana passada
                if (mentionsPastVerb && daysBack == 7) {
                    // Já está correto (semana passada)
                } else if (!mentionsPastVerb && daysBack == 7) {
                    // Se não mencionou verbo no passado e daysBack == 7, pode ser futuro
                    // Mas por padrão, se mencionou dia da semana sem contexto, assumir passado
                    // (mais comum falar "gastei na segunda" do que "vou gastar na segunda")
                }
                
                return today.minusDays(daysBack);
            }
        }
        
        // Padrão 1.5: "dia 15 de dezembro", "dia 10 de janeiro", "começando dia 18 de dezembro", "primeira parcela vai ser no dia 28 de novembro"
        // PRIORIDADE MÁXIMA: Procurar especificamente pelo padrão completo "dia X de [mês]" e extrair dia e mês juntos
        // Usar um mapa para garantir que cada nome de mês mapeia para o número correto do mês
        String[][] monthVariants = {
            {"janeiro", "1"}, {"fevereiro", "2"}, {"marco", "3"}, {"março", "3"},
            {"abril", "4"}, {"maio", "5"}, {"junho", "6"}, {"julho", "7"},
            {"agosto", "8"}, {"setembro", "9"}, {"outubro", "10"},
            {"novembro", "11"}, {"dezembro", "12"}
        };
        
        // Primeiro, tentar encontrar o padrão completo "dia X de [mês]" - isso elimina qualquer ambiguidade
        for (String[] monthData : monthVariants) {
            String monthName = monthData[0];
            int monthNumber = Integer.parseInt(monthData[1]);
            String escapedMonth = Pattern.quote(monthName);
            // Padrão completo: "dia X de [mês]" ou "no dia X de [mês]" ou "primeira parcela vai ser no dia X de [mês]"
            Pattern dayMonthPattern = Pattern.compile(
                "(?:dia|no dia|comecando\\s+dia|começando\\s+dia|primeira\\s+parcela\\s+(?:vai\\s+ser\\s+)?(?:no\\s+)?dia)\\s+(\\d{1,2})\\s+de\\s+" + escapedMonth + "\\b",
                Pattern.CASE_INSENSITIVE
            );
            Matcher dayMonthMatcher = dayMonthPattern.matcher(originalText);
            if (dayMonthMatcher.find()) {
                try {
                    int day = Integer.parseInt(dayMonthMatcher.group(1));
                    if (day >= 1 && day <= 31 && monthNumber >= 1 && monthNumber <= 12) {
                        try {
                            LocalDate date = LocalDate.of(currentYear, monthNumber, day);
                            System.out.println("DEBUG: Padrão completo encontrado: dia=" + day + ", mês=" + monthNumber + " (" + monthName + "), data=" + date);
                            // Se o mês já passou, usar próximo ano
                            if (date.isBefore(today)) {
                                date = date.plusYears(1);
                                System.out.println("DEBUG: Data ajustada para próximo ano: " + date);
                            }
                            System.out.println("DEBUG: Data extraída (padrão completo): " + date);
                            return date; // Retornar imediatamente - encontrou padrão completo
                        } catch (Exception e) {
                            System.out.println("ERROR creating LocalDate with month " + monthNumber + " and day " + day + ": " + e.getMessage());
                            // Continuar para próximo mês
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR parsing day from pattern: " + e.getMessage());
                    // Continuar para próximo mês
                }
            }
        }
        
        // Se não encontrou padrão completo, procurar mês mencionado sem dia específico
        int mentionedMonth = -1;
        String mentionedMonthStr = null;
        for (String[] monthData : monthVariants) {
            String monthName = monthData[0];
            int monthNumber = Integer.parseInt(monthData[1]);
            Pattern monthPattern = Pattern.compile("\\b" + Pattern.quote(monthName) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher monthMatcher = monthPattern.matcher(normalizedText);
            if (monthMatcher.find()) {
                mentionedMonth = monthNumber; // Usar o número do mês diretamente do mapa
                mentionedMonthStr = monthName;
                System.out.println("DEBUG: Mês genérico encontrado: " + monthName + " = mês " + mentionedMonth);
                break; // Pegar o primeiro encontrado
            }
        }
        
        if (mentionedMonth > 0 && mentionedMonth <= 12) {
            System.out.println("DEBUG: Mês mencionado encontrado: " + mentionedMonth + " (" + mentionedMonthStr + ")");
        } else if (mentionedMonth > 12) {
            System.out.println("ERROR: Mês inválido detectado: " + mentionedMonth);
        }
        
        // Padrão 1: "dia 15", "no dia 10", "dia 25"
        // Só usar este padrão se NÃO mencionou mês (para evitar conflito com "dia 15 de dezembro")
        if (mentionedMonth <= 0) {
            Pattern dayPattern = Pattern.compile("(?:dia|no dia|dia)\\s+(\\d{1,2})", Pattern.CASE_INSENSITIVE);
            Matcher dayMatcher = dayPattern.matcher(originalText);
            if (dayMatcher.find()) {
            try {
                int day = Integer.parseInt(dayMatcher.group(1));
                if (day >= 1 && day <= 31) {
                    // Tentar o mês atual primeiro
                    LocalDate date = LocalDate.of(currentYear, currentMonth, day);
                    
                    // Se a data já passou, verificar se mencionou "passado" ou similar
                    // Se não mencionou, assumir que é futuro (próximo mês)
                    boolean mentionsPast = normalizedText.contains("passado") || 
                                         normalizedText.contains("passada") ||
                                         normalizedText.contains("retro") ||
                                         normalizedText.contains("gastei") ||
                                         normalizedText.contains("paguei") ||
                                         normalizedText.contains("comprei");
                    
                    if (date.isBefore(today)) {
                        if (mentionsPast) {
                            // Se mencionou passado, usar a data passada (mês atual ou anterior)
                            return date;
                        } else {
                            // Se não mencionou, assumir futuro (próximo mês)
                            date = date.plusMonths(1);
                        }
                    }
                    return date;
                }
            } catch (Exception e) {
                // Ignorar
            }
            }
        }
        
        // Padrão 2: "15/12", "10/12/2024", "25/01"
        Pattern datePattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?");
        Matcher dateMatcher = datePattern.matcher(originalText);
        if (dateMatcher.find()) {
            try {
                int day = Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2));
                int year = dateMatcher.group(3) != null ? 
                    Integer.parseInt(dateMatcher.group(3).length() == 2 ? "20" + dateMatcher.group(3) : dateMatcher.group(3)) : 
                    currentYear;
                
                if (year < 100) {
                    year += 2000;
                }
                
                // Validar valores antes de criar a data
                if (month < 1 || month > 12) {
                    System.out.println("ERROR: Mês inválido: " + month);
                    // Ignorar e continuar
                } else if (day >= 1 && day <= 31) {
                    try {
                        LocalDate date = LocalDate.of(year, month, day);
                        
                        // Se a data já passou e não tem ano específico, verificar contexto
                        boolean mentionsPast = normalizedText.contains("passado") || 
                                             normalizedText.contains("passada") ||
                                             normalizedText.contains("retro") ||
                                             normalizedText.contains("gastei") ||
                                             normalizedText.contains("paguei") ||
                                             normalizedText.contains("comprei");
                        
                        // Se a data passou e não mencionou passado, assumir próximo ano (para compras futuras)
                        if (date.isBefore(today) && year == currentYear && !mentionsPast) {
                            date = date.plusYears(1);
                        }
                        // Se mencionou passado ou a data tem ano completo, usar a data como está
                        return date;
                    } catch (Exception e) {
                        System.out.println("ERROR creating LocalDate: " + e.getMessage());
                        // Ignorar e continuar
                    }
                }
            } catch (Exception e) {
                System.out.println("ERROR parsing date pattern: " + e.getMessage());
                // Ignorar
            }
        }
        
        // Padrão 3: "começando em janeiro", "no mês de dezembro" (só se não encontrou dia específico antes)
        // Usar a variável mentionedMonth que já foi calculada no padrão 1.5
        if (mentionedMonth > 0 && mentionedMonth <= 12) {
            // Se chegou aqui, significa que mencionou mês mas não encontrou dia específico
            // Usar o dia 15 do mês como padrão
            int day = 15;
            try {
                LocalDate date = LocalDate.of(currentYear, mentionedMonth, day);
                // Se o mês já passou, usar próximo ano
                if (date.isBefore(today)) {
                    date = date.plusYears(1);
                }
                System.out.println("DEBUG: Data extraída (só mês, usando dia 15): " + date);
                return date;
            } catch (Exception e) {
                System.out.println("ERROR creating LocalDate in padrão 3 with month " + mentionedMonth + ": " + e.getMessage());
                // Ignorar e continuar
            }
        }
        
        // Padrão 4: "hoje", "amanha", "amanhã"
        if (normalizedText.contains("hoje")) {
            return today;
        }
        if (normalizedText.contains("amanha") || normalizedText.contains("amanhã")) {
            return today.plusDays(1);
        }
        
        return null;
    }
}

