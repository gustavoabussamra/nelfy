package com.fin.service;

import com.fin.dto.CreateWithdrawalRequestDto;
import com.fin.dto.WithdrawalRequestDto;
import com.fin.model.Category;
import com.fin.model.Transaction;
import com.fin.model.User;
import com.fin.model.WithdrawalRequest;
import com.fin.repository.CategoryRepository;
import com.fin.repository.TransactionRepository;
import com.fin.repository.UserRepository;
import com.fin.repository.WithdrawalRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WithdrawalService {
    
    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ReferralService referralService;
    
    @Autowired
    private MinioService minioService;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Value("${minio.bucket.name:fin-receipts}")
    private String receiptsBucketName;
    
    /**
     * Calcula o valor disponível para saque (comissões recebidas - já solicitadas)
     */
    public BigDecimal getAvailableBalance(Long userId) {
        BigDecimal totalCommissions = referralService.getTotalCommissions(userId);
        
        // Soma todos os saques já solicitados (pendentes, em processamento ou concluídos)
        List<WithdrawalRequest> withdrawals = withdrawalRequestRepository.findByUserAndStatusIn(
            userRepository.findById(userId).orElseThrow(),
            List.of(
                WithdrawalRequest.WithdrawalStatus.PENDING,
                WithdrawalRequest.WithdrawalStatus.PROCESSING,
                WithdrawalRequest.WithdrawalStatus.COMPLETED
            )
        );
        
        BigDecimal totalWithdrawn = withdrawals.stream()
                .map(WithdrawalRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalCommissions.subtract(totalWithdrawn);
    }
    
    /**
     * Cria uma solicitação de saque
     */
    @Transactional
    public WithdrawalRequestDto createWithdrawalRequest(Long userId, CreateWithdrawalRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Validação: Só pode solicitar 1x por dia
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        
        WithdrawalRequest existingRequest = withdrawalRequestRepository
                .findFirstByUserAndCreatedAtAfterOrderByCreatedAtDesc(user, startOfDay)
                .orElse(null);
        
        if (existingRequest != null) {
            throw new RuntimeException("Você já solicitou um saque hoje. Tente novamente amanhã.");
        }
        
        // Validação: Valor disponível
        BigDecimal availableBalance = getAvailableBalance(userId);
        if (request.getAmount().compareTo(availableBalance) > 0) {
            throw new RuntimeException("Valor solicitado excede o saldo disponível. Saldo disponível: R$ " + 
                availableBalance.toPlainString());
        }
        
        // Validação: Valor mínimo
        if (request.getAmount().compareTo(BigDecimal.valueOf(10.00)) < 0) {
            throw new RuntimeException("Valor mínimo para saque é R$ 10,00");
        }
        
        // Cria a solicitação
        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setUser(user);
        withdrawal.setAmount(request.getAmount());
        withdrawal.setPixKey(request.getPixKey());
        withdrawal.setPixKeyType(request.getPixKeyType());
        withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.PENDING);
        
        withdrawal = withdrawalRequestRepository.save(withdrawal);
        
        return convertToDto(withdrawal);
    }
    
    /**
     * Lista solicitações de saque do usuário
     */
    public List<WithdrawalRequestDto> getUserWithdrawals(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        List<WithdrawalRequest> withdrawals = withdrawalRequestRepository.findByUserOrderByCreatedAtDesc(user);
        return withdrawals.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Lista solicitações de saque do usuário com paginação
     */
    public Page<WithdrawalRequestDto> getUserWithdrawals(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        Page<WithdrawalRequest> withdrawals = withdrawalRequestRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return withdrawals.map(this::convertToDto);
    }
    
    /**
     * Lista todas as solicitações de saque (para admin)
     */
    public Page<WithdrawalRequestDto> getAllWithdrawals(Pageable pageable) {
        Page<WithdrawalRequest> withdrawals = withdrawalRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        return withdrawals.map(this::convertToDto);
    }
    
    /**
     * Lista solicitações pendentes (para admin)
     */
    public Page<WithdrawalRequestDto> getPendingWithdrawals(Pageable pageable) {
        Page<WithdrawalRequest> withdrawals = withdrawalRequestRepository
                .findByStatusOrderByCreatedAtDesc(WithdrawalRequest.WithdrawalStatus.PENDING, pageable);
        return withdrawals.map(this::convertToDto);
    }
    
    /**
     * Atualiza status da solicitação (para admin)
     */
    @Transactional
    public WithdrawalRequestDto updateWithdrawalStatus(Long withdrawalId, WithdrawalRequest.WithdrawalStatus status, 
                                                       Long adminId, String notes) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Solicitação de saque não encontrada"));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin não encontrado"));
        
        withdrawal.setStatus(status);
        withdrawal.setProcessedBy(admin);
        withdrawal.setProcessedAt(LocalDateTime.now());
        if (notes != null) {
            withdrawal.setNotes(notes);
        }
        
        withdrawal = withdrawalRequestRepository.save(withdrawal);
        
        // Se o status for COMPLETED, criar transação de receita para o usuário
        if (status == WithdrawalRequest.WithdrawalStatus.COMPLETED) {
            createWithdrawalTransaction(withdrawal);
        }
        
        return convertToDto(withdrawal);
    }
    
    /**
     * Faz upload do comprovante PIX
     */
    @Transactional
    public WithdrawalRequestDto uploadReceipt(Long withdrawalId, MultipartFile file, Long adminId) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Solicitação de saque não encontrada"));
        
        try {
            // Validar arquivo
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Arquivo não fornecido ou está vazio");
            }
            
            // Validar nome do arquivo
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                originalFileName = "comprovante_" + System.currentTimeMillis() + ".jpg";
            }
            
            // Upload para MinIO
            String fileName = "withdrawal_" + withdrawalId + "_" + System.currentTimeMillis() + 
                             "_" + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String filePath = minioService.uploadFile(receiptsBucketName, fileName, file);
            
            withdrawal.setReceiptFilePath(filePath);
            withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.COMPLETED);
            withdrawal.setProcessedBy(userRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin não encontrado")));
            withdrawal.setProcessedAt(LocalDateTime.now());
            
            withdrawal = withdrawalRequestRepository.save(withdrawal);
            
            // Criar transação de receita para o usuário
            createWithdrawalTransaction(withdrawal);
            
            return convertToDto(withdrawal);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao fazer upload do comprovante: " + e.getMessage() + 
                    (e.getCause() != null ? " - Causa: " + e.getCause().getMessage() : ""));
        }
    }
    
    /**
     * Cria uma transação de receita quando o saque é confirmado
     */
    @Transactional
    private void createWithdrawalTransaction(WithdrawalRequest withdrawal) {
        User user = withdrawal.getUser();
        
        // Buscar ou criar categoria "Receitas do site"
        Category category = categoryRepository.findByUserAndName(user, "Receitas do site")
                .stream()
                .findFirst()
                .orElse(null);
        
        if (category == null) {
            category = new Category();
            category.setName("Receitas do site");
            category.setIcon("💰");
            category.setColor("#10B981");
            category.setType(com.fin.model.TransactionType.INCOME);
            category.setUser(user);
            category = categoryRepository.save(category);
        }
        
        // Criar transação de receita
        Transaction transaction = new Transaction();
        transaction.setDescription("Pagamento do sistema");
        transaction.setAmount(withdrawal.getAmount());
        transaction.setType(Transaction.TransactionType.INCOME);
        transaction.setTransactionDate(LocalDate.now());
        transaction.setDueDate(LocalDate.now());
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setIsPaid(true);
        transaction.setIsInstallment(false);
        transaction.setPaidDate(LocalDate.now());
        
        transactionRepository.save(transaction);
    }
    
    /**
     * Obtém URL para visualizar o comprovante
     */
    public String getReceiptUrl(Long withdrawalId, Long userId) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Solicitação de saque não encontrada"));
        
        // Verifica se o usuário tem permissão (é o dono ou é admin)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!withdrawal.getUser().getId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Acesso negado");
        }
        
        if (withdrawal.getReceiptFilePath() == null || withdrawal.getReceiptFilePath().isEmpty()) {
            throw new RuntimeException("Comprovante não disponível");
        }
        
        try {
            return minioService.getFileUrl(receiptsBucketName, withdrawal.getReceiptFilePath());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter URL do comprovante: " + e.getMessage());
        }
    }
    
    /**
     * Obtém o arquivo do comprovante como InputStream
     */
    public InputStream getReceiptFile(Long withdrawalId, Long userId) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Solicitação de saque não encontrada"));
        
        // Verifica se o usuário tem permissão (é o dono ou é admin)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!withdrawal.getUser().getId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Acesso negado");
        }
        
        if (withdrawal.getReceiptFilePath() == null || withdrawal.getReceiptFilePath().isEmpty()) {
            throw new RuntimeException("Comprovante não disponível");
        }
        
        return minioService.getFile(receiptsBucketName, withdrawal.getReceiptFilePath());
    }
    
    /**
     * Obtém o caminho do arquivo do comprovante (para detectar tipo MIME)
     */
    public String getReceiptFilePath(Long withdrawalId, Long userId) {
        WithdrawalRequest withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Solicitação de saque não encontrada"));
        
        // Verifica se o usuário tem permissão (é o dono ou é admin)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (!withdrawal.getUser().getId().equals(userId) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Acesso negado");
        }
        
        return withdrawal.getReceiptFilePath();
    }
    
    private WithdrawalRequestDto convertToDto(WithdrawalRequest withdrawal) {
        WithdrawalRequestDto dto = new WithdrawalRequestDto();
        dto.setId(withdrawal.getId());
        dto.setUserId(withdrawal.getUser().getId());
        dto.setUserName(withdrawal.getUser().getName());
        dto.setUserEmail(withdrawal.getUser().getEmail());
        dto.setAmount(withdrawal.getAmount());
        dto.setStatus(withdrawal.getStatus().name());
        dto.setPixKey(withdrawal.getPixKey());
        dto.setPixKeyType(withdrawal.getPixKeyType());
        dto.setNotes(withdrawal.getNotes());
        dto.setCreatedAt(withdrawal.getCreatedAt());
        dto.setUpdatedAt(withdrawal.getUpdatedAt());
        
        if (withdrawal.getProcessedBy() != null) {
            dto.setProcessedById(withdrawal.getProcessedBy().getId());
            dto.setProcessedByName(withdrawal.getProcessedBy().getName());
        }
        dto.setProcessedAt(withdrawal.getProcessedAt());
        
        // URL do comprovante será gerada quando necessário
        if (withdrawal.getReceiptFilePath() != null && !withdrawal.getReceiptFilePath().isEmpty()) {
            dto.setReceiptFileUrl("/api/withdrawals/" + withdrawal.getId() + "/receipt");
        }
        
        return dto;
    }
}

