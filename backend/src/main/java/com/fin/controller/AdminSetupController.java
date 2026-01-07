package com.fin.controller;

import com.fin.model.Referral;
import com.fin.model.ReferralCommission;
import com.fin.model.Subscription;
import com.fin.model.User;
import com.fin.repository.ReferralCommissionRepository;
import com.fin.repository.ReferralRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller temporário para criar usuário admin
 * Pode ser removido após criar o primeiro admin
 */
@RestController
@RequestMapping("/api/admin-setup")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminSetupController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ReferralRepository referralRepository;
    
    @Autowired
    private ReferralCommissionRepository referralCommissionRepository;
    
    @PostMapping("/create-admin")
    public String createAdmin() {
        // Verificar se já existe admin com este email
        if (userRepository.findByEmail("gustavo.abussamra@gmail.com").isPresent()) {
            return "Usuário admin já existe!";
        }
        
        User admin = new User();
        admin.setEmail("gustavo.abussamra@gmail.com");
        admin.setPassword(passwordEncoder.encode("gustavo123"));
        admin.setName("Gustavo Admin");
        admin.setRole("ADMIN");
        admin.setReferralCode("ADMIN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(admin);
        
        return "Usuário admin criado com sucesso!\nEmail: gustavo.abussamra@gmail.com\nSenha: gustavo123";
    }
    
    @PostMapping("/add-test-commissions")
    public String addTestCommissions() {
        // Buscar ou criar o usuário teste@gmail.com
        Optional<User> userOpt = userRepository.findByEmail("teste@gmail.com");
        User user;
        
        if (userOpt.isEmpty()) {
            // Criar o usuário se não existir
            user = new User();
            user.setEmail("teste@gmail.com");
            user.setPassword(passwordEncoder.encode("teste123"));
            user.setName("Usuário Teste");
            user.setRole("USER");
            user.setReferralCode("TESTE" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user = userRepository.save(user);
        } else {
            user = userOpt.get();
        }
        
        // Buscar ou criar um usuário de teste que foi indicado
        Optional<User> testUserOpt = userRepository.findByEmail("teste.indicado@example.com");
        User testUser;
        
        if (testUserOpt.isEmpty()) {
            testUser = new User();
            testUser.setEmail("teste.indicado@example.com");
            testUser.setPassword(passwordEncoder.encode("teste123"));
            testUser.setName("Usuário Teste Indicado");
            testUser.setRole("USER");
            testUser.setReferralCode("TEST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            testUser = userRepository.save(testUser);
        } else {
            testUser = testUserOpt.get();
        }
        
        // Verificar se já existe referral para este usuário de teste
        Optional<Referral> existingReferral = referralRepository.findByReferredId(testUser.getId());
        Referral referral;
        
        if (existingReferral.isPresent()) {
            referral = existingReferral.get();
        } else {
            // Criar o referral
            referral = new Referral();
            referral.setReferrer(user);
            referral.setReferred(testUser);
            referral.setReferralCode(user.getReferralCode() + "-" + testUser.getId());
            referral.setRewardGiven(false);
            referral = referralRepository.save(referral);
        }
        
        // Criar 3 comissões de teste (últimos 3 meses)
        LocalDateTime now = LocalDateTime.now();
        
        // Mês atual - Plano PREMIUM (R$ 59,90)
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        Optional<ReferralCommission> existing1 = referralCommissionRepository
            .findByReferralAndPaymentYearAndPaymentMonth(referral, currentYear, currentMonth);
        
        if (existing1.isEmpty()) {
            ReferralCommission commission1 = new ReferralCommission();
            commission1.setReferral(referral);
            commission1.setPaymentYear(currentYear);
            commission1.setPaymentMonth(currentMonth);
            commission1.setSubscriptionPlan(Subscription.SubscriptionPlan.PREMIUM);
            commission1.setMonthlyAmount(BigDecimal.valueOf(59.90));
            commission1.setCommissionRate(BigDecimal.valueOf(0.10));
            commission1.setCommissionAmount(BigDecimal.valueOf(5.99)); // 10% de 59.90
            commission1.setPaymentDate(now);
            referralCommissionRepository.save(commission1);
        }
        
        // Mês anterior - Plano BASIC (R$ 29,90)
        LocalDateTime lastMonth = now.minusMonths(1);
        int lastYear = lastMonth.getYear();
        int lastMonthValue = lastMonth.getMonthValue();
        Optional<ReferralCommission> existing2 = referralCommissionRepository
            .findByReferralAndPaymentYearAndPaymentMonth(referral, lastYear, lastMonthValue);
        
        if (existing2.isEmpty()) {
            ReferralCommission commission2 = new ReferralCommission();
            commission2.setReferral(referral);
            commission2.setPaymentYear(lastYear);
            commission2.setPaymentMonth(lastMonthValue);
            commission2.setSubscriptionPlan(Subscription.SubscriptionPlan.BASIC);
            commission2.setMonthlyAmount(BigDecimal.valueOf(29.90));
            commission2.setCommissionRate(BigDecimal.valueOf(0.10));
            commission2.setCommissionAmount(BigDecimal.valueOf(2.99)); // 10% de 29.90
            commission2.setPaymentDate(lastMonth);
            referralCommissionRepository.save(commission2);
        }
        
        // 2 meses atrás - Plano ENTERPRISE (R$ 149,90)
        LocalDateTime twoMonthsAgo = now.minusMonths(2);
        int twoMonthsYear = twoMonthsAgo.getYear();
        int twoMonthsMonth = twoMonthsAgo.getMonthValue();
        Optional<ReferralCommission> existing3 = referralCommissionRepository
            .findByReferralAndPaymentYearAndPaymentMonth(referral, twoMonthsYear, twoMonthsMonth);
        
        if (existing3.isEmpty()) {
            ReferralCommission commission3 = new ReferralCommission();
            commission3.setReferral(referral);
            commission3.setPaymentYear(twoMonthsYear);
            commission3.setPaymentMonth(twoMonthsMonth);
            commission3.setSubscriptionPlan(Subscription.SubscriptionPlan.ENTERPRISE);
            commission3.setMonthlyAmount(BigDecimal.valueOf(149.90));
            commission3.setCommissionRate(BigDecimal.valueOf(0.10));
            commission3.setCommissionAmount(BigDecimal.valueOf(14.99)); // 10% de 149.90
            commission3.setPaymentDate(twoMonthsAgo);
            referralCommissionRepository.save(commission3);
        }
        
        BigDecimal total = BigDecimal.valueOf(5.99).add(BigDecimal.valueOf(2.99)).add(BigDecimal.valueOf(14.99));
        
        return String.format(
            "Comissões de teste criadas com sucesso!\n\n" +
            "Total de comissões: R$ %.2f\n" +
            "- Mês atual (PREMIUM): R$ 5,99\n" +
            "- Mês anterior (BASIC): R$ 2,99\n" +
            "- 2 meses atrás (ENTERPRISE): R$ 14,99\n\n" +
            "Agora você pode testar o sistema de saques!",
            total.doubleValue()
        );
    }
}


