package com.fin.controller;

import com.fin.dto.TransactionAttachmentDto;
import com.fin.security.SecurityUtil;
import com.fin.service.TransactionAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "http://localhost:3000")
public class TransactionAttachmentController {
    
    @Autowired
    private TransactionAttachmentService attachmentService;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @PostMapping("/transaction/{transactionId}")
    public ResponseEntity<TransactionAttachmentDto> uploadAttachment(
            @PathVariable Long transactionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            TransactionAttachmentDto attachment = attachmentService.uploadAttachment(transactionId, file, description, userId);
            return ResponseEntity.ok(attachment);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upload do anexo: " + e.getMessage());
        }
    }
    
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<TransactionAttachmentDto>> getTransactionAttachments(@PathVariable Long transactionId) {
        Long userId = securityUtil.getCurrentUserId();
        List<TransactionAttachmentDto> attachments = attachmentService.getTransactionAttachments(transactionId, userId);
        return ResponseEntity.ok(attachments);
    }
    
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            InputStream inputStream = attachmentService.getAttachmentFile(attachmentId, userId);
            
            // Obter nome do arquivo do anexo
            TransactionAttachmentDto attachment = attachmentService.getAttachment(attachmentId, userId);
            
            Resource resource = new InputStreamResource(inputStream);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao baixar anexo: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        try {
            Long userId = securityUtil.getCurrentUserId();
            attachmentService.deleteAttachment(attachmentId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar anexo: " + e.getMessage());
        }
    }
}

