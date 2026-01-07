package com.fin.service;

import com.fin.dto.PageResponse;
import com.fin.dto.TransactionDto;
import com.fin.model.Account;
import com.fin.model.Category;
import com.fin.model.Installment;
import com.fin.model.Transaction;
import com.fin.model.User;
import com.fin.repository.AccountRepository;
import com.fin.repository.CategoryRepository;
import com.fin.repository.InstallmentRepository;
import com.fin.repository.TransactionRepository;
import com.fin.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private InstallmentRepository installmentRepository;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AutomationRuleService automationRuleService;
    
    @Autowired
    private KafkaTransactionProducer kafkaTransactionProducer;
    
    public List<TransactionDto> getUserTransactions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // OTIMIZAÇÃO: Usar query otimizada com JOIN FETCH para evitar N+1 queries
        List<Transaction> mainTransactions = transactionRepository.findMainTransactionsByUserId(userId);
        return mainTransactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    // Método paginado para transações principais
    public PageResponse<TransactionDto> getUserTransactionsPaged(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionPage = transactionRepository.findMainTransactionsByUserIdPaged(userId, pageable);
        
        List<TransactionDto> content = transactionPage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(content, page, size, transactionPage.getTotalElements());
    }
    
    public List<TransactionDto> getAllUserTransactions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // OTIMIZAÇÃO: Usar query otimizada com JOIN FETCH para evitar N+1 queries
        List<Transaction> transactions = transactionRepository.findByUserIdWithCategory(userId);
        return transactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca apenas transações parceladas (transações principais com totalInstallments > 1)
     */
    public List<TransactionDto> getInstallmentTransactions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // OTIMIZAÇÃO: Usar query otimizada ao invés de filtrar em memória
        List<Transaction> installmentTransactions = transactionRepository.findInstallmentTransactionsByUserId(userId);
        
        return installmentTransactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<TransactionDto> getInstallmentsByParentId(Long parentId, Long userId) {
        Transaction parentTransaction = transactionRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!parentTransaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        // Buscar todas as parcelas (transações filhas)
        List<Transaction> installments = transactionRepository.findByUserId(userId).stream()
                .filter(t -> parentId.equals(t.getParentTransactionId()))
                .sorted((t1, t2) -> {
                    if (t1.getInstallmentNumber() != null && t2.getInstallmentNumber() != null) {
                        return t1.getInstallmentNumber().compareTo(t2.getInstallmentNumber());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        
        return installments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public TransactionDto createBalanceUpdateTransaction(BigDecimal targetBalance, String description, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // Calcular saldo atual (usando todas as transações até hoje)
        LocalDate today = LocalDate.now();
        BigDecimal currentBalance = getBalance(userId, LocalDate.of(1900, 1, 1), today); // Desde o início dos tempos até hoje
        
        // Calcular diferença necessária para atingir o saldo desejado
        BigDecimal difference = targetBalance.subtract(currentBalance);
        
        // Se não há diferença, não precisa criar transação
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            throw new RuntimeException("O saldo já está no valor desejado de R$ " + targetBalance);
        }
        
        Transaction transaction = new Transaction();
        
        // Descrição padrão ou personalizada
        String transactionDescription = description != null && !description.trim().isEmpty() 
            ? description 
            : String.format("Ajuste de saldo: de R$ %.2f para R$ %.2f", currentBalance, targetBalance);
        
        transaction.setDescription(transactionDescription);
        
        // O valor da transação é o valor absoluto da diferença
        transaction.setAmount(difference.abs());
        
        // Se a diferença é positiva, é receita; se negativa, é despesa
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            transaction.setType(Transaction.TransactionType.INCOME);
        } else {
            transaction.setType(Transaction.TransactionType.EXPENSE);
        }
        
        transaction.setDueDate(LocalDate.now());
        transaction.setTransactionDate(LocalDate.now());
        transaction.setUser(user);
        transaction.setIsPaid(true); // Saldo atualizado é sempre considerado pago
        transaction.setIsInstallment(false);
        transaction.setPaidDate(LocalDate.now());
        
        transaction = transactionRepository.save(transaction);
        
        System.out.println(String.format("Ajuste de saldo criado: Saldo atual: R$ %.2f, Saldo desejado: R$ %.2f, Diferença: R$ %.2f (%s)",
            currentBalance, targetBalance, difference.abs(), 
            difference.compareTo(BigDecimal.ZERO) > 0 ? "Receita" : "Despesa"));
        
        return convertToDto(transaction);
    }
    
    public TransactionDto getTransaction(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        return convertToDto(transaction);
    }
    
    @Transactional
    public TransactionDto createTransaction(TransactionDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // Usar dueDate como data principal (vencimento), se não existir, usar transactionDate
        LocalDate effectiveDate = dto.getDueDate() != null ? dto.getDueDate() : 
                                  (dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now());
        dto.setDueDate(effectiveDate);
        dto.setTransactionDate(effectiveDate); // Usar a mesma data para transactionDate
        
        // Se isPaid não for informado, usar false para transações futuras/parceladas
        if (dto.getIsPaid() == null) {
            dto.setIsPaid(false);
        }
        
        // Se for parcelada, definir flag
        if (dto.getTotalInstallments() != null && dto.getTotalInstallments() > 1) {
            dto.setIsInstallment(true);
        } else {
            dto.setIsInstallment(false);
            dto.setTotalInstallments(1);
        }
        
        System.out.println("=== ENVIANDO TRANSAÇÃO PARA KAFKA ===");
        System.out.println("Total de parcelas informado: " + dto.getTotalInstallments());
        System.out.println("Valor: " + dto.getAmount());
        System.out.println("Data de vencimento (dueDate): " + dto.getDueDate());
        System.out.println("Data da transação (transactionDate): " + dto.getTransactionDate());
        System.out.println("Effective date calculada: " + effectiveDate);
        System.out.println("UserId: " + userId);
        
        // Enviar para Kafka - o consumer vai persistir no banco
        // Para transações parceladas, o consumer vai criar todas as parcelas
        kafkaTransactionProducer.sendTransaction(dto, userId);
        
        // Retornar o DTO imediatamente (sem ID ainda, será gerado pelo consumer)
        // O frontend pode fazer polling ou usar WebSocket para atualizar quando a transação for persistida
        // Por enquanto, retornamos o DTO como está
        System.out.println(">>> Transação enviada para Kafka com sucesso");
        return dto;
    }
    
    @Transactional
    private void createInstallments(Transaction parentTransaction, BigDecimal installmentAmount, Integer totalInstallments, LocalDate startDate) {
        System.out.println("=== INICIANDO CRIAÇÃO DE PARCELAS ===");
        System.out.println("Transação pai ID: " + parentTransaction.getId());
        System.out.println("Total de parcelas: " + totalInstallments);
        System.out.println("Valor de cada parcela: " + installmentAmount);
        System.out.println("Data inicial: " + startDate);
        
        // O valor informado já é o valor de cada parcela, não precisa dividir
        List<Installment> installments = new ArrayList<>();
        List<Transaction> installmentTransactions = new ArrayList<>();
        
        // Criar parcelas: a primeira será no mês da startDate, as demais nos meses seguintes
        for (int i = 1; i <= totalInstallments; i++) {
            // Parcela 1: startDate + 0 meses = startDate
            // Parcela 2: startDate + 1 mês
            // Parcela 3: startDate + 2 meses
            // etc.
            LocalDate dueDate = startDate.plusMonths(i - 1);
            
            Installment installment = new Installment();
            installment.setTransaction(parentTransaction);
            installment.setInstallmentNumber(i);
            installment.setTotalInstallments(totalInstallments);
            installment.setAmount(installmentAmount); // Mesmo valor para todas as parcelas
            installment.setDueDate(dueDate);
            installment.setIsPaid(false);
            installments.add(installment);
            
            // Criar transação para cada parcela
            Transaction installmentTransaction = new Transaction();
            installmentTransaction.setDescription(parentTransaction.getDescription() + " - Parcela " + i + "/" + totalInstallments);
            installmentTransaction.setAmount(installmentAmount); // Mesmo valor da parcela
            installmentTransaction.setType(parentTransaction.getType());
            installmentTransaction.setDueDate(dueDate); // Data de vencimento
            installmentTransaction.setTransactionDate(dueDate); // Usar a mesma data (vencimento)
            installmentTransaction.setUser(parentTransaction.getUser());
            installmentTransaction.setCategory(parentTransaction.getCategory());
            installmentTransaction.setIsPaid(false);
            installmentTransaction.setIsInstallment(true);
            installmentTransaction.setParentTransactionId(parentTransaction.getId());
            installmentTransaction.setInstallmentNumber(i);
            installmentTransaction.setTotalInstallments(totalInstallments);
            installmentTransactions.add(installmentTransaction);
            
            System.out.println("Criada parcela " + i + "/" + totalInstallments + " - Data: " + dueDate + ", Valor: " + installmentAmount);
        }
        
        System.out.println("Total de parcelas (Installment) criadas: " + installments.size());
        System.out.println("Total de transações criadas: " + installmentTransactions.size());
        
        // IMPORTANTE: Salvar primeiro as parcelas (Installments)
        List<Installment> savedInstallments = installmentRepository.saveAll(installments);
        installmentRepository.flush(); // Garantir que as parcelas foram salvas
        System.out.println("Installments salvos no banco: " + savedInstallments.size());
        
        // Depois salvar todas as transações de parcela
        // Salvar uma por uma para garantir que todas sejam persistidas
        List<Transaction> savedInstallmentTransactions = new ArrayList<>();
        for (int i = 0; i < installmentTransactions.size(); i++) {
            Transaction installmentTransaction = installmentTransactions.get(i);
            try {
                // Salvar diretamente no repositório e fazer flush imediato
                Transaction saved = transactionRepository.save(installmentTransaction);
                transactionRepository.flush(); // Flush imediato após cada salvamento
                savedInstallmentTransactions.add(saved);
                System.out.println(">>> Transação " + (i + 1) + "/" + installmentTransactions.size() + " salva: ID=" + saved.getId() + 
                                 ", Parcela=" + saved.getInstallmentNumber() + ", ParentID=" + saved.getParentTransactionId() + 
                                 ", Data=" + saved.getDueDate() + ", Valor=" + saved.getAmount());
            } catch (Exception e) {
                System.err.println(">>> ERRO ao salvar transação " + (i + 1) + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Erro ao criar parcela " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        // Flush forçado para garantir que todas foram salvas
        transactionRepository.flush();
        System.out.println("Flush executado. Total de transações antes do flush: " + savedInstallmentTransactions.size());
        
        // Verificação final
        System.out.println("=== VERIFICAÇÃO FINAL ===");
        System.out.println("Total de transações salvas na memória: " + savedInstallmentTransactions.size());
        
        // Verificar no banco se todas foram salvas - fazer uma nova consulta
        transactionRepository.flush(); // Garantir flush final
        List<Transaction> allUserTransactions = transactionRepository.findByUserId(parentTransaction.getUser().getId());
        List<Transaction> verifyTransactions = allUserTransactions.stream()
                .filter(t -> parentTransaction.getId().equals(t.getParentTransactionId()))
                .sorted((t1, t2) -> {
                    if (t1.getInstallmentNumber() != null && t2.getInstallmentNumber() != null) {
                        return t1.getInstallmentNumber().compareTo(t2.getInstallmentNumber());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        
        System.out.println("Transações encontradas no banco para parent ID " + parentTransaction.getId() + ": " + verifyTransactions.size());
        for (Transaction t : verifyTransactions) {
            System.out.println("  - Parcela " + t.getInstallmentNumber() + ": ID=" + t.getId() + ", Data=" + t.getDueDate() + ", Valor=" + t.getAmount());
        }
        
        if (verifyTransactions.size() != totalInstallments) {
            System.err.println("ERRO CRÍTICO: Esperado " + totalInstallments + " parcelas, mas encontrado apenas " + verifyTransactions.size());
            throw new RuntimeException("Erro ao criar parcelas: foram criadas apenas " + verifyTransactions.size() + " de " + totalInstallments + " parcelas");
        } else {
            System.out.println("SUCESSO: Todas as " + totalInstallments + " parcelas foram criadas e salvas corretamente no banco!");
        }
    }
    
    @Transactional
    public TransactionDto updateTransaction(Long id, TransactionDto dto, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        transaction.setDescription(dto.getDescription());
        transaction.setAmount(dto.getAmount());
        transaction.setType(Transaction.TransactionType.valueOf(dto.getType()));
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setDueDate(dto.getDueDate());
        
        // Atualizar status de pagamento
        if (dto.getIsPaid() != null) {
            transaction.setIsPaid(dto.getIsPaid());
            if (dto.getIsPaid() && transaction.getPaidDate() == null) {
                transaction.setPaidDate(LocalDate.now());
            } else if (!dto.getIsPaid()) {
                transaction.setPaidDate(null);
            }
        }
        
        // Se for parcela, atualizar também a parcela correspondente
        if (transaction.getParentTransactionId() != null) {
            Long parentId = transaction.getParentTransactionId();
            Integer installmentNum = transaction.getInstallmentNumber();
            Boolean isPaidStatus = transaction.getIsPaid();
            
            List<Installment> installments = installmentRepository.findByTransactionId(parentId);
            Installment installment = installments.stream()
                    .filter(i -> i.getInstallmentNumber().equals(installmentNum))
                    .findFirst()
                    .orElse(null);
            if (installment != null) {
                installment.setIsPaid(isPaidStatus);
                if (isPaidStatus) {
                    installment.setPaidDate(LocalDate.now());
                } else {
                    installment.setPaidDate(null);
                }
                installmentRepository.save(installment);
            }
        }
        
        if (dto.getCategory() != null && dto.getCategory().getId() != null) {
            Category category = categoryRepository.findById(dto.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            transaction.setCategory(category);
        }
        
        if (dto.getAccountId() != null) {
            Account account = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
            if (!account.getUser().getId().equals(userId)) {
                throw new RuntimeException("Acesso negado à conta");
            }
            transaction.setAccount(account);
        }
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Aplicar regras de automação
        try {
            automationRuleService.applyRulesToTransaction(userId, savedTransaction);
            // Recarregar a transação caso tenha sido modificada pelas regras
            savedTransaction = transactionRepository.findById(savedTransaction.getId()).orElse(savedTransaction);
        } catch (Exception e) {
            // Log do erro mas não falha a atualização da transação
            System.err.println("Erro ao aplicar regras de automação: " + e.getMessage());
        }
        
        return convertToDto(savedTransaction);
    }
    
    @Transactional
    public void deleteTransaction(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        transactionRepository.delete(transaction);
    }
    
    public BigDecimal getBalance(Long userId, LocalDate startDate, LocalDate endDate) {
        // Buscar TODAS as transações do usuário
        List<Transaction> allTransactions = transactionRepository.findByUserId(userId);
        
        // CRITÉRIOS PARA CONTAR NO SALDO:
        // 1. isPaid DEVE ser explicitamente true (não null, não false)
        // 2. NÃO contar transações parceladas (isInstallment = true)
        // 3. Apenas transações simples (isInstallment = false ou null)
        
        BigDecimal income = allTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                .filter(t -> {
                    // Apenas contar se isPaid for explicitamente true (Boolean.TRUE)
                    // Não contar se for null ou false
                    return Boolean.TRUE.equals(t.getIsPaid());
                })
                .filter(t -> {
                    // NÃO contar transações pai parceladas (isInstallment = true E parentTransactionId = null)
                    // Essas têm valor zero e são apenas ilustrativas
                    if (t.getIsInstallment() != null && t.getIsInstallment() && t.getParentTransactionId() == null) {
                        return false; // Transação pai parcelada não conta
                    }
                    // Contar transações simples (não parceladas)
                    if (t.getIsInstallment() == null || !t.getIsInstallment()) {
                        return true;
                    }
                    // Contar parcelas individuais pagas
                    return t.getParentTransactionId() != null;
                })
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal expense = allTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> {
                    // Apenas contar se isPaid for explicitamente true (Boolean.TRUE)
                    // Não contar se for null ou false
                    return Boolean.TRUE.equals(t.getIsPaid());
                })
                .filter(t -> {
                    // NÃO contar transações pai parceladas (isInstallment = true E parentTransactionId = null)
                    // Essas têm valor zero e são apenas ilustrativas
                    if (t.getIsInstallment() != null && t.getIsInstallment() && t.getParentTransactionId() == null) {
                        return false; // Transação pai parcelada não conta
                    }
                    // Contar transações simples (não parceladas)
                    if (t.getIsInstallment() == null || !t.getIsInstallment()) {
                        return true;
                    }
                    // Contar parcelas individuais pagas
                    return t.getParentTransactionId() != null;
                })
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return income.subtract(expense);
    }
    
    @Transactional
    public TransactionDto markAsPaid(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        transaction.setIsPaid(true);
        transaction.setPaidDate(LocalDate.now());
        
        // Se for parcela, atualizar também a parcela correspondente
        if (transaction.getParentTransactionId() != null) {
            List<Installment> installments = installmentRepository.findByTransactionId(transaction.getParentTransactionId());
            Installment installment = installments.stream()
                    .filter(i -> i.getInstallmentNumber().equals(transaction.getInstallmentNumber()))
                    .findFirst()
                    .orElse(null);
            if (installment != null) {
                installment.setIsPaid(true);
                installment.setPaidDate(LocalDate.now());
                installmentRepository.save(installment);
            }
        }
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Aplicar regras de automação
        try {
            automationRuleService.applyRulesToTransaction(userId, savedTransaction);
            // Recarregar a transação caso tenha sido modificada pelas regras
            savedTransaction = transactionRepository.findById(savedTransaction.getId()).orElse(savedTransaction);
        } catch (Exception e) {
            // Log do erro mas não falha a atualização da transação
            System.err.println("Erro ao aplicar regras de automação: " + e.getMessage());
        }
        
        return convertToDto(savedTransaction);
    }
    
    @Transactional
    public TransactionDto markAsUnpaid(Long id, Long userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        transaction.setIsPaid(false);
        transaction.setPaidDate(null);
        
        // Se for parcela, atualizar também a parcela correspondente
        if (transaction.getParentTransactionId() != null) {
            List<Installment> installments = installmentRepository.findByTransactionId(transaction.getParentTransactionId());
            Installment installment = installments.stream()
                    .filter(i -> i.getInstallmentNumber().equals(transaction.getInstallmentNumber()))
                    .findFirst()
                    .orElse(null);
            if (installment != null) {
                installment.setIsPaid(false);
                installment.setPaidDate(null);
                installmentRepository.save(installment);
            }
        }
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Aplicar regras de automação
        try {
            automationRuleService.applyRulesToTransaction(userId, savedTransaction);
            // Recarregar a transação caso tenha sido modificada pelas regras
            savedTransaction = transactionRepository.findById(savedTransaction.getId()).orElse(savedTransaction);
        } catch (Exception e) {
            // Log do erro mas não falha a atualização da transação
            System.err.println("Erro ao aplicar regras de automação: " + e.getMessage());
        }
        
        return convertToDto(savedTransaction);
    }
    
    public List<TransactionDto> getUpcomingTransactions(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate fiveDaysFromNow = today.plusDays(5);
        
        // OTIMIZAÇÃO: Usar query otimizada ao invés de filtrar em memória
        List<Transaction> transactions = transactionRepository.findUpcomingTransactionsByUserId(userId, today, fiveDaysFromNow);
        
        return transactions.stream()
                .map(this::convertToDto)
                .sorted((t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()))
                .collect(Collectors.toList());
    }
    
    public List<TransactionDto> getOverdueTransactions(Long userId) {
        LocalDate today = LocalDate.now();
        
        // OTIMIZAÇÃO: Usar query otimizada ao invés de filtrar em memória
        List<Transaction> transactions = transactionRepository.findOverdueTransactionsByUserId(userId, today);
        
        return transactions.stream()
                .map(this::convertToDto)
                .sorted((t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()))
                .collect(Collectors.toList());
    }
    
    private TransactionDto convertToDto(Transaction transaction) {
        System.out.println("convertToDto chamado para transação ID=" + transaction.getId() + 
                         ", description=" + transaction.getDescription());
        
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setDescription(transaction.getDescription());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType().name());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setIsPaid(transaction.getIsPaid());
        dto.setIsInstallment(transaction.getIsInstallment());
        dto.setParentTransactionId(transaction.getParentTransactionId());
        dto.setInstallmentNumber(transaction.getInstallmentNumber());
        dto.setTotalInstallments(transaction.getTotalInstallments());
        dto.setDueDate(transaction.getDueDate());
        dto.setPaidDate(transaction.getPaidDate());
        
        System.out.println("  Valores setados: isInstallment=" + transaction.getIsInstallment() + 
                         ", parentTransactionId=" + transaction.getParentTransactionId() + 
                         ", totalInstallments=" + transaction.getTotalInstallments());
        
        // Calcular dias até vencimento
        if (transaction.getDueDate() != null) {
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, transaction.getDueDate());
            dto.setDaysUntilDue((int) days);
            dto.setIsOverdue(days < 0 && (transaction.getIsPaid() == null || !transaction.getIsPaid()));
        }
        
        if (transaction.getCategory() != null) {
            com.fin.dto.CategoryDto categoryDto = new com.fin.dto.CategoryDto();
            categoryDto.setId(transaction.getCategory().getId());
            categoryDto.setName(transaction.getCategory().getName());
            categoryDto.setIcon(transaction.getCategory().getIcon());
            categoryDto.setColor(transaction.getCategory().getColor());
            categoryDto.setType(transaction.getCategory().getType().name());
            dto.setCategory(categoryDto);
        }
        
        if (transaction.getAccount() != null) {
            dto.setAccountId(transaction.getAccount().getId());
            com.fin.dto.AccountDto accountDto = new com.fin.dto.AccountDto();
            accountDto.setId(transaction.getAccount().getId());
            accountDto.setName(transaction.getAccount().getName());
            accountDto.setType(transaction.getAccount().getType().name());
            accountDto.setBalance(transaction.getAccount().getBalance());
            dto.setAccount(accountDto);
        }
        
        // Calcular status das parcelas se for uma transação pai parcelada
        // IMPORTANTE: Sempre calcular para transações pai parceladas, mesmo que não tenha parcelas ainda
        if (transaction.getIsInstallment() != null && transaction.getIsInstallment() 
            && transaction.getParentTransactionId() == null 
            && transaction.getTotalInstallments() != null && transaction.getTotalInstallments() > 1) {
            
            System.out.println("=== CALCULANDO STATUS DE PARCELAS ===");
            System.out.println("Transação ID: " + transaction.getId());
            System.out.println("isInstallment: " + transaction.getIsInstallment());
            System.out.println("parentTransactionId: " + transaction.getParentTransactionId());
            System.out.println("totalInstallments: " + transaction.getTotalInstallments());
            
            try {
                // Buscar todas as parcelas filhas desta transação diretamente pelo parentTransactionId
                Long parentId = transaction.getId();
                
                System.out.println("Buscando parcelas para parentId: " + parentId);
                
                // Usar query direta do repository
                List<Transaction> childInstallments = transactionRepository.findByParentTransactionId(parentId);
                
                System.out.println("Parcelas filhas encontradas: " + childInstallments.size());
            childInstallments.forEach(t -> {
                System.out.println("  Parcela ID=" + t.getId() + ", installmentNumber=" + t.getInstallmentNumber() + 
                                 ", isPaid=" + t.getIsPaid() + " (tipo: " + (t.getIsPaid() != null ? t.getIsPaid().getClass().getName() : "null") + ")");
            });
            
            int totalCount = childInstallments.size();
            int paidCount = (int) childInstallments.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getIsPaid()))
                    .count();
            
            // Se não encontrou parcelas, usar o totalInstallments da transação pai
            if (totalCount == 0) {
                totalCount = transaction.getTotalInstallments();
                System.out.println("Nenhuma parcela encontrada, usando totalInstallments: " + totalCount);
            }
            
            dto.setTotalInstallmentsCount(totalCount);
            dto.setPaidInstallmentsCount(paidCount);
            
            // Calcular valor total somando todas as parcelas (para transações pai com amount = 0)
            if (transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal totalAmount = childInstallments.stream()
                        .map(Transaction::getAmount)
                        .filter(amt -> amt != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    dto.setAmount(totalAmount); // Atualizar o valor total no DTO
                    System.out.println("Valor total calculado das parcelas: R$ " + totalAmount);
                }
            }
            
            System.out.println("RESULTADO: paidCount=" + paidCount + ", totalCount=" + totalCount);
            System.out.println("DTO valores setados: paidInstallmentsCount=" + dto.getPaidInstallmentsCount() + 
                             ", totalInstallmentsCount=" + dto.getTotalInstallmentsCount() + 
                             ", amount=" + dto.getAmount());
            System.out.println("=====================================");
            } catch (Exception e) {
                System.out.println("ERRO ao calcular parcelas: " + e.getMessage());
                e.printStackTrace();
                // Em caso de erro, usar valores padrão
                dto.setTotalInstallmentsCount(transaction.getTotalInstallments());
                dto.setPaidInstallmentsCount(0);
            }
        } else {
            // Debug para ver por que não está entrando no if
            if (transaction.getTotalInstallments() != null && transaction.getTotalInstallments() > 1) {
                System.out.println("NÃO entrou no cálculo - Transação ID=" + transaction.getId() + 
                                 ", isInstallment=" + transaction.getIsInstallment() + 
                                 ", parentTransactionId=" + transaction.getParentTransactionId() +
                                 ", totalInstallments=" + transaction.getTotalInstallments());
            }
        }
        
        return dto;
    }
    
    public List<TransactionDto> getMonthlyTransactions(Long userId, LocalDate month) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // Calcular início e fim do mês
        YearMonth yearMonth = YearMonth.from(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // Buscar transações do mês usando dueDate (data de vencimento)
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        
        // Filtrar transações do mês selecionado
        List<Transaction> monthlyTransactions = transactions.stream()
                .filter(t -> {
                    // Usar dueDate se disponível, senão usar transactionDate
                    LocalDate transactionDate = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                    return transactionDate != null && 
                           !transactionDate.isBefore(startDate) && 
                           !transactionDate.isAfter(endDate);
                })
                // Excluir transações pai parceladas (valor zero, apenas ilustrativas)
                .filter(t -> {
                    if (t.getIsInstallment() != null && t.getIsInstallment() && t.getParentTransactionId() == null) {
                        return false; // Transação pai parcelada não aparece na lista
                    }
                    return true;
                })
                .sorted(Comparator.comparing((Transaction t) -> {
                    LocalDate date = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                    return date != null ? date : LocalDate.MIN;
                }).reversed())
                .collect(Collectors.toList());
        
        return monthlyTransactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    public List<com.fin.controller.TransactionController.CategoryStatsDto> getMonthlyCategoryStats(Long userId, LocalDate month) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // Calcular início e fim do mês
        YearMonth yearMonth = YearMonth.from(month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // Buscar transações do mês
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        
        // Filtrar transações do mês selecionado (apenas despesas pagas)
        List<Transaction> monthlyExpenses = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                .filter(t -> Boolean.TRUE.equals(t.getIsPaid())) // Apenas pagas
                .filter(t -> {
                    LocalDate transactionDate = t.getDueDate() != null ? t.getDueDate() : t.getTransactionDate();
                    return transactionDate != null && 
                           !transactionDate.isBefore(startDate) && 
                           !transactionDate.isAfter(endDate);
                })
                // Excluir transações pai parceladas (valor zero)
                .filter(t -> {
                    if (t.getIsInstallment() != null && t.getIsInstallment() && t.getParentTransactionId() == null) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        // Agrupar por categoria e calcular totais
        Map<Category, List<Transaction>> groupedByCategory = monthlyExpenses.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(Transaction::getCategory));
        
        // Converter para DTOs e ordenar por valor total (maior para menor)
        List<com.fin.controller.TransactionController.CategoryStatsDto> stats = groupedByCategory.entrySet().stream()
                .map(entry -> {
                    Category category = entry.getKey();
                    List<Transaction> categoryTransactions = entry.getValue();
                    
                    BigDecimal totalAmount = categoryTransactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return new com.fin.controller.TransactionController.CategoryStatsDto(
                            category.getId(),
                            category.getName(),
                            category.getIcon(),
                            category.getColor(),
                            totalAmount,
                            (long) categoryTransactions.size()
                    );
                })
                .sorted(Comparator.comparing(com.fin.controller.TransactionController.CategoryStatsDto::getTotalAmount).reversed())
                .collect(Collectors.toList());
        
        return stats;
    }
}

