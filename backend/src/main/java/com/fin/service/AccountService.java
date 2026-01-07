package com.fin.service;

import com.fin.dto.AccountDto;
import com.fin.model.Account;
import com.fin.model.User;
import com.fin.repository.AccountRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private PlanLimitsService planLimitsService;
    
    /**
     * Lista todas as contas do usuário
     */
    public List<AccountDto> getUserAccounts(Long userId) {
        List<Account> accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        return accounts.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtém uma conta específica
     */
    public AccountDto getAccount(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        return convertToDto(account);
    }
    
    /**
     * Cria uma nova conta
     */
    @Transactional
    public AccountDto createAccount(AccountDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // Verificar limite de contas baseado no plano
        if (!canCreateAccount(userId)) {
            throw new RuntimeException("Limite de contas atingido para seu plano. Faça upgrade para criar mais contas.");
        }
        
        Account account = new Account();
        account.setName(dto.getName());
        account.setType(Account.AccountType.valueOf(dto.getType()));
        account.setBalance(dto.getBalance() != null ? dto.getBalance() : BigDecimal.ZERO);
        account.setBankName(dto.getBankName());
        account.setAccountNumber(dto.getAccountNumber());
        account.setAgency(dto.getAgency());
        account.setDescription(dto.getDescription());
        account.setIsActive(true);
        account.setUser(user);
        
        account = accountRepository.save(account);
        return convertToDto(account);
    }
    
    /**
     * Atualiza uma conta
     */
    @Transactional
    public AccountDto updateAccount(Long accountId, AccountDto dto, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        account.setName(dto.getName());
        account.setType(Account.AccountType.valueOf(dto.getType()));
        if (dto.getBalance() != null) {
            account.setBalance(dto.getBalance());
        }
        account.setBankName(dto.getBankName());
        account.setAccountNumber(dto.getAccountNumber());
        account.setAgency(dto.getAgency());
        account.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) {
            account.setIsActive(dto.getIsActive());
        }
        
        account = accountRepository.save(account);
        return convertToDto(account);
    }
    
    /**
     * Deleta uma conta (soft delete)
     */
    @Transactional
    public void deleteAccount(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        account.setIsActive(false);
        accountRepository.save(account);
    }
    
    /**
     * Calcula o saldo total consolidado de todas as contas
     */
    public BigDecimal getTotalBalance(Long userId) {
        List<Account> accounts = accountRepository.findByUserIdAndIsActiveTrue(userId);
        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Verifica se o usuário pode criar mais contas baseado no plano
     */
    private boolean canCreateAccount(Long userId) {
        // Verificar plano do usuário
        if (!subscriptionService.isSubscriptionActive(userId)) {
            return false;
        }
        
        // Contar contas ativas
        long activeAccountsCount = accountRepository.findByUserIdAndIsActiveTrue(userId).size();
        
        // Limites por plano (pode ser melhorado consultando o plano real)
        // FREE: 1 conta, BASIC: 3 contas, PREMIUM: ilimitado, ENTERPRISE: ilimitado
        // Por enquanto, vamos permitir até 10 contas para todos (pode ser ajustado)
        return activeAccountsCount < 10;
    }
    
    private AccountDto convertToDto(Account account) {
        AccountDto dto = new AccountDto();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setType(account.getType().name());
        dto.setBalance(account.getBalance());
        dto.setBankName(account.getBankName());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setAgency(account.getAgency());
        dto.setDescription(account.getDescription());
        dto.setIsActive(account.getIsActive());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        return dto;
    }
}

