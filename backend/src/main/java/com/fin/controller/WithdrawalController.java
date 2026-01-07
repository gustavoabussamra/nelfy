package com.fin.controller;

import com.fin.dto.CreateWithdrawalRequestDto;
import com.fin.dto.PageResponse;
import com.fin.dto.WithdrawalRequestDto;
import com.fin.model.WithdrawalRequest;
import com.fin.security.SecurityUtil;
import com.fin.service.WithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/withdrawals")
@CrossOrigin(origins = "http://localhost:3000")
public class WithdrawalController {
    
    @Autowired
    private WithdrawalService withdrawalService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    /**
     * Obtém saldo disponível para saque
     */
    @GetMapping("/available-balance")
    public ResponseEntity<Map<String, Object>> getAvailableBalance() {
        Long userId = securityUtil.getCurrentUserId();
        BigDecimal availableBalance = withdrawalService.getAvailableBalance(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("availableBalance", availableBalance);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cria uma solicitação de saque
     */
    @PostMapping
    public ResponseEntity<WithdrawalRequestDto> createWithdrawalRequest(
            @RequestBody CreateWithdrawalRequestDto request) {
        Long userId = securityUtil.getCurrentUserId();
        WithdrawalRequestDto withdrawal = withdrawalService.createWithdrawalRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(withdrawal);
    }
    
    /**
     * Lista solicitações de saque do usuário
     */
    @GetMapping("/my-withdrawals")
    public ResponseEntity<?> getMyWithdrawals(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Long userId = securityUtil.getCurrentUserId();
        
        // Se page e size forem fornecidos, retorna paginado
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<WithdrawalRequestDto> withdrawals = withdrawalService.getUserWithdrawals(userId, pageable);
            
            PageResponse<WithdrawalRequestDto> response = PageResponse.of(
                withdrawals.getContent(),
                withdrawals.getNumber(),
                withdrawals.getSize(),
                withdrawals.getTotalElements()
            );
            
            return ResponseEntity.ok(response);
        }
        
        // Caso contrário, retorna lista completa (compatibilidade)
        List<WithdrawalRequestDto> withdrawals = withdrawalService.getUserWithdrawals(userId);
        return ResponseEntity.ok(withdrawals);
    }
    
    /**
     * Lista todas as solicitações de saque (admin)
     */
    @GetMapping("/admin/all")
    public ResponseEntity<PageResponse<WithdrawalRequestDto>> getAllWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!securityUtil.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequestDto> withdrawals = withdrawalService.getAllWithdrawals(pageable);
        
        PageResponse<WithdrawalRequestDto> response = PageResponse.of(
            withdrawals.getContent(),
            withdrawals.getNumber(),
            withdrawals.getSize(),
            withdrawals.getTotalElements()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lista solicitações pendentes (admin)
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<PageResponse<WithdrawalRequestDto>> getPendingWithdrawals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!securityUtil.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequestDto> withdrawals = withdrawalService.getPendingWithdrawals(pageable);
        
        PageResponse<WithdrawalRequestDto> response = PageResponse.of(
            withdrawals.getContent(),
            withdrawals.getNumber(),
            withdrawals.getSize(),
            withdrawals.getTotalElements()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Atualiza status da solicitação (admin)
     */
    @PutMapping("/admin/{id}/status")
    public ResponseEntity<WithdrawalRequestDto> updateWithdrawalStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        if (!securityUtil.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Long adminId = securityUtil.getCurrentUserId();
        WithdrawalRequest.WithdrawalStatus status = 
            WithdrawalRequest.WithdrawalStatus.valueOf(request.get("status"));
        String notes = request.get("notes");
        
        WithdrawalRequestDto withdrawal = withdrawalService.updateWithdrawalStatus(id, status, adminId, notes);
        return ResponseEntity.ok(withdrawal);
    }
    
    /**
     * Faz upload do comprovante PIX (admin)
     */
    @PostMapping("/admin/{id}/receipt")
    public ResponseEntity<?> uploadReceipt(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            if (!securityUtil.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Acesso negado"));
            }
            
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Arquivo não fornecido"));
            }
            
            // Validar tamanho do arquivo (máximo 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "Arquivo muito grande. Tamanho máximo: 10MB"));
            }
            
            Long adminId = securityUtil.getCurrentUserId();
            WithdrawalRequestDto withdrawal = withdrawalService.uploadReceipt(id, file, adminId);
            return ResponseEntity.ok(withdrawal);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Erro ao fazer upload do comprovante"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro inesperado ao fazer upload do comprovante: " + e.getMessage()));
        }
    }
    
    /**
     * Obtém URL do comprovante
     */
    @GetMapping("/{id}/receipt-url")
    public ResponseEntity<Map<String, String>> getReceiptUrl(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        String url = withdrawalService.getReceiptUrl(id, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("url", url);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Obtém o arquivo do comprovante
     */
    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> getReceipt(@PathVariable Long id) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            InputStream fileStream = withdrawalService.getReceiptFile(id, userId);
            
            byte[] fileBytes = fileStream.readAllBytes();
            fileStream.close();
            
            // Detectar tipo MIME baseado na extensão do arquivo
            String filePath = withdrawalService.getReceiptFilePath(id, userId);
            String contentType = "image/jpeg"; // padrão
            if (filePath != null) {
                String lowerPath = filePath.toLowerCase();
                if (lowerPath.endsWith(".png")) {
                    contentType = "image/png";
                } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (lowerPath.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (lowerPath.endsWith(".gif")) {
                    contentType = "image/gif";
                } else if (lowerPath.endsWith(".webp")) {
                    contentType = "image/webp";
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("inline", "comprovante-pix.jpg");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

