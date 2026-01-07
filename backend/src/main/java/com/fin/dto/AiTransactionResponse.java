package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTransactionResponse {
    private boolean success;
    private TransactionDto transaction; // Preenchido quando success = true
    private String message; // Mensagem de sucesso ou erro
    private List<MissingInfo> missingInfo; // Lista de informações faltantes
    private String suggestedQuestion; // Pergunta sugerida para o usuário
    private boolean needsCategory; // Se precisa selecionar categoria (apenas para despesas)
    private List<CategoryDto> availableCategories; // Lista de categorias disponíveis (quando needsCategory = true)
    
    public static AiTransactionResponse success(TransactionDto transaction, String message) {
        AiTransactionResponse response = new AiTransactionResponse();
        response.success = true;
        response.transaction = transaction;
        response.message = message;
        return response;
    }
    
    public static AiTransactionResponse needsInfo(String message, List<MissingInfo> missingInfo, String suggestedQuestion) {
        AiTransactionResponse response = new AiTransactionResponse();
        response.success = false;
        response.message = message;
        response.missingInfo = missingInfo;
        response.suggestedQuestion = suggestedQuestion;
        return response;
    }
    
    public static AiTransactionResponse needsConfirmation(TransactionDto transaction, String message) {
        AiTransactionResponse response = new AiTransactionResponse();
        response.success = false; // Não é sucesso ainda, precisa confirmar
        response.transaction = transaction; // Dados da transação para confirmação
        response.message = message;
        return response;
    }
    
    public static AiTransactionResponse needsCategory(TransactionDto transaction, List<CategoryDto> availableCategories, String message) {
        AiTransactionResponse response = new AiTransactionResponse();
        response.success = false;
        response.transaction = transaction;
        response.message = message;
        response.needsCategory = true;
        response.availableCategories = availableCategories;
        return response;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingInfo {
        private String field; // "amount", "date", "category", "installments"
        private String description; // Descrição do que está faltando
        private String suggestion; // Sugestão de como informar
    }
}


