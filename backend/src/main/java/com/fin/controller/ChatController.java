package com.fin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> handleMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String response = generateResponse(message);
        
        Map<String, Object> result = new HashMap<>();
        result.put("response", response);
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }
    
    private String generateResponse(String userMessage) {
        if (userMessage == null) {
            return "Olá! Como posso ajudá-lo?";
        }
        
        String message = userMessage.toLowerCase();
        
        if (message.contains("preço") || message.contains("valor") || message.contains("quanto")) {
            return "Temos planos a partir de R$ 0 (grátis)! O plano Básico custa R$ 29,90/mês, Premium R$ 59,90/mês e Empresarial R$ 149,90/mês. Todos incluem 30 dias grátis! 🎉";
        }
        
        if (message.contains("trial") || message.contains("teste") || message.contains("grátis")) {
            return "Sim! Oferecemos 30 dias grátis em todos os planos. Você pode testar todas as funcionalidades sem compromisso. Não precisa de cartão de crédito para começar! ✨";
        }
        
        if (message.contains("funcionalidade") || message.contains("recurso") || message.contains("faz")) {
            return "O Nelfy oferece: automação inteligente com IA, dashboard executivo, múltiplas contas, metas financeiras, orçamentos, relatórios avançados, detecção de anomalias e muito mais! 🚀";
        }
        
        if (message.contains("humano") || message.contains("atendente") || message.contains("pessoa") || 
            message.contains("operador") || message.contains("suporte humano") || message.contains("falar com alguém") ||
            message.contains("conversar com") || message.contains("whatsapp") || message.contains("whats")) {
            return "REDIRECT_WHATSAPP:5511999999999"; // Retorna código especial para redirecionamento
        }
        
        if (message.contains("suporte") || message.contains("ajuda") || message.contains("problema")) {
            return "Estou aqui para ajudar! Você pode me perguntar sobre planos, funcionalidades, preços ou qualquer dúvida. Se precisar falar com um atendente humano, digite 'falar com humano' ou 'whatsapp' e eu te transfiro! 💬";
        }
        
        if (message.contains("cadastro") || message.contains("registro") || message.contains("criar conta")) {
            return "Para criar sua conta, clique no botão 'Começar Grátis' no topo da página. É rápido e fácil! 🎯";
        }
        
        if (message.contains("pagamento") || message.contains("cartão") || message.contains("pagar")) {
            return "Aceitamos cartão de crédito através do Mercado Pago. O pagamento é seguro e processado automaticamente. Você pode cancelar a qualquer momento! 💳";
        }
        
        return "Entendi! Posso ajudá-lo com informações sobre nossos planos, funcionalidades ou qualquer dúvida sobre o Nelfy. O que você gostaria de saber? 😊";
    }
}

