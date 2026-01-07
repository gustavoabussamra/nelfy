package com.fin.service;

import com.fin.dto.ReferralDto;
import com.fin.dto.ReferralCommissionDto;
import com.fin.model.Referral;
import com.fin.model.ReferralCommission;
import com.fin.model.Subscription;
import com.fin.model.User;
import com.fin.repository.ReferralCommissionRepository;
import com.fin.repository.ReferralRepository;
import com.fin.repository.SubscriptionRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReferralService {
    
    @Autowired
    private ReferralRepository referralRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    @Autowired
    private ReferralCommissionRepository referralCommissionRepository;
    
    /**
     * Gera um código de referência único para o usuário
     */
    @Transactional
    public String generateReferralCode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Se já tem código, retorna o existente
        if (user.getReferralCode() != null && !user.getReferralCode().isEmpty()) {
            return user.getReferralCode();
        }
        
        // Gera novo código único
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (referralRepository.findByReferralCode(code).isPresent());
        
        user.setReferralCode(code);
        userRepository.save(user);
        
        return code;
    }
    
    /**
     * Processa um convite usando código de referência
     */
    @Transactional
    public ReferralDto processReferral(String referralCode, Long newUserId) {
        // Busca o usuário que convidou pelo código de referência
        User referrer = userRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new RuntimeException("Código de referência inválido"));
        
        // Verifica se não está se auto-convidando
        if (referrer.getId().equals(newUserId)) {
            throw new RuntimeException("Você não pode usar seu próprio código de referência");
        }
        
        // Verifica se o novo usuário já foi convidado por alguém
        Optional<Referral> existingReferred = referralRepository.findByReferredId(newUserId);
        if (existingReferred.isPresent()) {
            throw new RuntimeException("Você já foi convidado por outro usuário");
        }
        
        // Cria o registro de referência
        User referred = userRepository.findById(newUserId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Gera um código único para este registro de referência
        String uniqueCode = referralCode + "-" + newUserId;
        
        Referral referral = new Referral();
        referral.setReferrer(referrer);
        referral.setReferred(referred);
        referral.setReferralCode(uniqueCode);
        referral.setRewardGiven(false);
        
        referral = referralRepository.save(referral);
        
        return convertToDto(referral);
    }
    
    /**
     * Processa comissão quando um indicado paga sua mensalidade
     * Chamado pelo PaymentService quando um pagamento é processado
     */
    @Transactional
    public void processCommission(Long referredUserId, Subscription.SubscriptionPlan plan, LocalDateTime paymentDate) {
        // Busca o referral do usuário que pagou
        Optional<Referral> referralOpt = referralRepository.findByReferredId(referredUserId);
        if (referralOpt.isEmpty()) {
            return; // Usuário não foi indicado por ninguém
        }
        
        Referral referral = referralOpt.get();
        
        // Se o plano for FREE, não há comissão
        if (plan == Subscription.SubscriptionPlan.FREE) {
            return;
        }
        
        int year = paymentDate.getYear();
        int month = paymentDate.getMonthValue();
        
        // Verifica se já existe comissão para este mês
        Optional<ReferralCommission> existingCommission = 
            referralCommissionRepository.findByReferralAndPaymentYearAndPaymentMonth(referral, year, month);
        
        if (existingCommission.isPresent()) {
            return; // Já processado
        }
        
        // Calcula a comissão (10% da mensalidade)
        BigDecimal monthlyAmount = BigDecimal.valueOf(plan.getPrice());
        BigDecimal commissionRate = BigDecimal.valueOf(0.10);
        BigDecimal commissionAmount = monthlyAmount.multiply(commissionRate);
        
        // Cria o registro de comissão
        ReferralCommission commission = new ReferralCommission();
        commission.setReferral(referral);
        commission.setPaymentYear(year);
        commission.setPaymentMonth(month);
        commission.setSubscriptionPlan(plan);
        commission.setMonthlyAmount(monthlyAmount);
        commission.setCommissionRate(commissionRate);
        commission.setCommissionAmount(commissionAmount);
        commission.setPaymentDate(paymentDate);
        
        referralCommissionRepository.save(commission);
    }
    
    /**
     * Lista todas as referências de um usuário com detalhes de pagamentos e comissões
     */
    public List<ReferralDto> getUserReferrals(Long userId) {
        List<Referral> referrals = referralRepository.findByReferrerId(userId);
        
        if (referrals.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Otimização: Buscar todas as subscriptions e comissões de uma vez (batch queries)
        List<Long> referredUserIds = referrals.stream()
                .map(r -> r.getReferred().getId())
                .collect(Collectors.toList());
        
        // Buscar todas as subscriptions de uma vez
        List<Subscription> subscriptions = subscriptionRepository.findByUserIdIn(referredUserIds);
        Map<Long, Subscription> subscriptionMap = subscriptions.stream()
                .collect(Collectors.toMap(s -> s.getUser().getId(), s -> s));
        
        // Buscar todas as comissões de uma vez
        List<Long> referralIds = referrals.stream()
                .map(Referral::getId)
                .collect(Collectors.toList());
        
        List<ReferralCommission> allCommissions = referralCommissionRepository.findByReferralIdIn(referralIds);
        Map<Long, List<ReferralCommission>> commissionsMap = allCommissions.stream()
                .collect(Collectors.groupingBy(c -> c.getReferral().getId()));
        
        // Converter para DTOs usando os dados já carregados
        return referrals.stream()
                .map(referral -> convertToDtoWithCommissions(
                    referral, 
                    subscriptionMap.get(referral.getReferred().getId()),
                    commissionsMap.getOrDefault(referral.getId(), new ArrayList<>())
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Converte Referral para DTO incluindo informações de comissões (passadas e futuras)
     * Versão otimizada que recebe os dados já carregados
     */
    private ReferralDto convertToDtoWithCommissions(Referral referral, Subscription subscription, List<ReferralCommission> paidCommissions) {
        ReferralDto dto = convertToDto(referral);
        
        // Ordena as comissões pagas
        List<ReferralCommission> sortedPaidCommissions = paidCommissions.stream()
                .sorted((a, b) -> {
                    int yearCompare = b.getPaymentYear().compareTo(a.getPaymentYear());
                    if (yearCompare != 0) return yearCompare;
                    return b.getPaymentMonth().compareTo(a.getPaymentMonth());
                })
                .collect(Collectors.toList());
        
        // Define informações da assinatura do indicado
        if (subscription != null) {
            dto.setReferredPlan(subscription.getPlan().name());
            dto.setReferredPlanPrice(subscription.getPlan().getPrice());
            dto.setSubscriptionEndDate(subscription.getEndDate());
        }
        
        List<ReferralCommissionDto> commissionDtos = new ArrayList<>();
        
        // Adiciona comissões já pagas
        for (ReferralCommission commission : paidCommissions) {
            ReferralCommissionDto commissionDto = convertCommissionToDto(commission);
            commissionDto.setIsPaid(true);
            commissionDtos.add(commissionDto);
        }
        
        // Calcula comissões futuras esperadas se a assinatura estiver ativa e não for FREE
        if (subscription != null && subscription.getIsActive() &&
            subscription.getPlan() != Subscription.SubscriptionPlan.FREE &&
            subscription.getEndDate() != null) {
            
            List<ReferralCommissionDto> futureCommissions = calculateFutureCommissions(
                referral, subscription, sortedPaidCommissions);
            commissionDtos.addAll(futureCommissions);
        }
        
        // Ordena por ano e mês (mais antigo primeiro)
        commissionDtos.sort((a, b) -> {
            int yearCompare = a.getPaymentYear().compareTo(b.getPaymentYear());
            if (yearCompare != 0) return yearCompare;
            return a.getPaymentMonth().compareTo(b.getPaymentMonth());
        });
        
        // Calcula totais
        BigDecimal totalPaid = sortedPaidCommissions.stream()
                .map(ReferralCommission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFuture = commissionDtos.stream()
                .filter(c -> !c.getIsPaid())
                .map(ReferralCommissionDto::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        dto.setCommissions(commissionDtos);
        dto.setTotalPayments(sortedPaidCommissions.size());
        dto.setTotalCommission(totalPaid);
        dto.setExpectedFutureCommission(totalFuture);
        
        return dto;
    }
    
    /**
     * Calcula comissões futuras esperadas baseadas no plano atual e data de término
     */
    private List<ReferralCommissionDto> calculateFutureCommissions(
            Referral referral, Subscription subscription, List<ReferralCommission> paidCommissions) {
        
        List<ReferralCommissionDto> futureCommissions = new ArrayList<>();
        
        LocalDate now = LocalDate.now();
        LocalDate endDate = subscription.getEndDate().toLocalDate();
        Subscription.SubscriptionPlan plan = subscription.getPlan();
        
        // Se a assinatura já expirou, não há comissões futuras
        if (endDate.isBefore(now)) {
            return futureCommissions;
        }
        
        // Calcula a partir do mês atual
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth endMonth = YearMonth.from(endDate);
        YearMonth startMonth = subscription.getStartDate() != null ? 
            YearMonth.from(subscription.getStartDate()) : currentMonth;
        
        // Cria um conjunto de meses já pagos para evitar duplicatas
        java.util.Set<YearMonth> paidMonths = paidCommissions.stream()
                .map(c -> YearMonth.of(c.getPaymentYear(), c.getPaymentMonth()))
                .collect(java.util.stream.Collectors.toSet());
        
        // Calcula comissões mês a mês desde o mês atual até a data de término
        // Considera que cada mês da assinatura gera uma comissão
        // Limita a 24 meses no futuro para evitar performance issues
        YearMonth maxFutureMonth = currentMonth.plusMonths(24);
        YearMonth actualEndMonth = endMonth.isAfter(maxFutureMonth) ? maxFutureMonth : endMonth;
        
        YearMonth month = currentMonth;
        int count = 0;
        final int MAX_FUTURE_COMMISSIONS = 24; // Limite de 24 meses
        
        while (!month.isAfter(actualEndMonth) && count < MAX_FUTURE_COMMISSIONS) {
            // Se já foi pago, pula
            if (paidMonths.contains(month)) {
                month = month.plusMonths(1);
                continue;
            }
            
            // Só inclui meses que estão dentro do período da assinatura
            if (month.isBefore(startMonth)) {
                month = month.plusMonths(1);
                continue;
            }
            
            // Cria comissão futura esperada
            ReferralCommissionDto futureCommission = new ReferralCommissionDto();
            futureCommission.setId(null); // Não tem ID pois ainda não foi pago
            futureCommission.setPaymentYear(month.getYear());
            futureCommission.setPaymentMonth(month.getMonthValue());
            futureCommission.setSubscriptionPlan(plan.name());
            futureCommission.setMonthlyAmount(BigDecimal.valueOf(plan.getPrice()));
            futureCommission.setCommissionRate(BigDecimal.valueOf(0.10));
            futureCommission.setCommissionAmount(
                BigDecimal.valueOf(plan.getPrice()).multiply(BigDecimal.valueOf(0.10)));
            futureCommission.setPaymentDate(null); // Ainda não foi pago
            futureCommission.setCreatedAt(null);
            futureCommission.setIsPaid(false); // É futuro
            
            futureCommissions.add(futureCommission);
            month = month.plusMonths(1);
            count++;
        }
        
        return futureCommissions;
    }
    
    private ReferralCommissionDto convertCommissionToDto(ReferralCommission commission) {
        ReferralCommissionDto dto = new ReferralCommissionDto();
        dto.setId(commission.getId());
        dto.setPaymentYear(commission.getPaymentYear());
        dto.setPaymentMonth(commission.getPaymentMonth());
        dto.setSubscriptionPlan(commission.getSubscriptionPlan().name());
        dto.setMonthlyAmount(commission.getMonthlyAmount());
        dto.setCommissionRate(commission.getCommissionRate());
        dto.setCommissionAmount(commission.getCommissionAmount());
        dto.setPaymentDate(commission.getPaymentDate());
        dto.setCreatedAt(commission.getCreatedAt());
        dto.setIsPaid(true); // Comissões do banco são sempre pagas
        return dto;
    }
    
    /**
     * Conta quantos convites um usuário fez
     */
    public Long countUserReferrals(Long userId) {
        return referralRepository.countByReferrerId(userId);
    }
    
    /**
     * Calcula o total de comissões recebidas por um usuário
     */
    public BigDecimal getTotalCommissions(Long userId) {
        List<ReferralCommission> commissions = referralCommissionRepository.findByReferralReferrerId(userId);
        return commissions.stream()
                .map(ReferralCommission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calcula o total de comissões futuras esperadas por um usuário
     */
    public BigDecimal getExpectedFutureCommissions(Long userId) {
        List<Referral> referrals = referralRepository.findByReferrerId(userId);
        
        if (referrals.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Otimização: Buscar todas as subscriptions e comissões de uma vez (batch queries)
        List<Long> referredUserIds = referrals.stream()
                .map(r -> r.getReferred().getId())
                .collect(Collectors.toList());
        
        List<Subscription> subscriptions = subscriptionRepository.findByUserIdIn(referredUserIds);
        Map<Long, Subscription> subscriptionMap = subscriptions.stream()
                .collect(Collectors.toMap(s -> s.getUser().getId(), s -> s));
        
        List<Long> referralIds = referrals.stream()
                .map(Referral::getId)
                .collect(Collectors.toList());
        
        List<ReferralCommission> allCommissions = referralCommissionRepository.findByReferralIdIn(referralIds);
        Map<Long, List<ReferralCommission>> commissionsMap = allCommissions.stream()
                .collect(Collectors.groupingBy(c -> c.getReferral().getId()));
        
        BigDecimal total = BigDecimal.ZERO;
        
        for (Referral referral : referrals) {
            Subscription subscription = subscriptionMap.get(referral.getReferred().getId());
            if (subscription != null && subscription.getIsActive() && 
                subscription.getPlan() != Subscription.SubscriptionPlan.FREE &&
                subscription.getEndDate() != null) {
                
                List<ReferralCommission> paidCommissions = commissionsMap.getOrDefault(referral.getId(), new ArrayList<>());
                
                List<ReferralCommissionDto> futureCommissions = calculateFutureCommissions(
                    referral, subscription, paidCommissions);
                
                BigDecimal referralFutureTotal = futureCommissions.stream()
                        .map(ReferralCommissionDto::getCommissionAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                total = total.add(referralFutureTotal);
            }
        }
        
        return total;
    }
    
    private ReferralDto convertToDto(Referral referral) {
        ReferralDto dto = new ReferralDto();
        dto.setId(referral.getId());
        dto.setReferrerId(referral.getReferrer().getId());
        dto.setReferrerName(referral.getReferrer().getName());
        dto.setReferredId(referral.getReferred().getId());
        dto.setReferredName(referral.getReferred().getName());
        dto.setReferralCode(referral.getReferralCode());
        dto.setRewardGiven(referral.getRewardGiven());
        if (referral.getRewardType() != null) {
            dto.setRewardType(referral.getRewardType().name());
        }
        dto.setRewardValue(referral.getRewardValue());
        dto.setRewardedAt(referral.getRewardedAt());
        dto.setCreatedAt(referral.getCreatedAt());
        return dto;
    }
}

