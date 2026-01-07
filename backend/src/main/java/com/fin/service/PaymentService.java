package com.fin.service;

import com.fin.dto.PaymentRequestDto;
import com.fin.dto.PaymentResponseDto;
import com.fin.model.Subscription;
import com.fin.repository.SubscriptionRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private ReferralService referralService;
    
    @Value("${payment.mercadopago.public-key:}")
    private String mercadoPagoPublicKey;
    
    @Value("${payment.mercadopago.access-token:}")
    private String mercadoPagoAccessToken;
    
    @Value("${payment.mercadopago.enabled:false}")
    private Boolean mercadoPagoEnabled;
    
    /**
     * Cria uma solicitação de pagamento
     * Por enquanto, simula o pagamento. Em produção, integrar com Mercado Pago
     */
    @Transactional
    public PaymentResponseDto createPaymentRequest(Long userId, PaymentRequestDto request) {
        // Verificar se o plano existe
        Subscription.SubscriptionPlan plan;
        try {
            plan = Subscription.SubscriptionPlan.valueOf(request.getPlan());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Plano inválido");
        }
        
        // Se Mercado Pago estiver habilitado, criar pagamento real
        if (mercadoPagoEnabled && !mercadoPagoAccessToken.isEmpty()) {
            return createMercadoPagoPayment(userId, plan, request);
        }
        
        // Modo de teste: simular pagamento aprovado automaticamente
        return createTestPayment(userId, plan, request);
    }
    
    /**
     * Cria pagamento no Mercado Pago (implementação futura)
     */
    private PaymentResponseDto createMercadoPagoPayment(Long userId, Subscription.SubscriptionPlan plan, PaymentRequestDto request) {
        // TODO: Integrar com SDK do Mercado Pago
        // Por enquanto, retorna simulação
        String paymentId = UUID.randomUUID().toString();
        
        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(paymentId);
        response.setPaymentUrl("https://www.mercadopago.com.br/checkout/v1/redirect?pref_id=" + paymentId);
        response.setStatus("PENDING");
        response.setMessage("Redirecione para o link de pagamento");
        
        return response;
    }
    
    /**
     * Cria pagamento de teste (aprovado automaticamente)
     */
    @Transactional
    private PaymentResponseDto createTestPayment(Long userId, Subscription.SubscriptionPlan plan, PaymentRequestDto request) {
        // Aprovar pagamento automaticamente em modo de teste
        subscriptionService.updateSubscription(userId, plan);
        
        // Processa comissão de referral se aplicável
        referralService.processCommission(userId, plan, LocalDateTime.now());
        
        String paymentId = UUID.randomUUID().toString();
        
        PaymentResponseDto response = new PaymentResponseDto();
        response.setPaymentId(paymentId);
        response.setPaymentUrl(null);
        response.setStatus("APPROVED");
        response.setMessage("Pagamento aprovado (modo de teste)");
        
        return response;
    }
    
    /**
     * Processa webhook de pagamento (chamado pelo gateway)
     */
    @Transactional
    public void processPaymentWebhook(String paymentId, String status) {
        // TODO: Implementar processamento de webhook
        // Buscar paymentId no banco e atualizar assinatura
    }
}

