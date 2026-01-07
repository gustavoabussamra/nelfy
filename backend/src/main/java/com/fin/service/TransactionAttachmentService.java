package com.fin.service;

import com.fin.dto.TransactionAttachmentDto;
import com.fin.model.Transaction;
import com.fin.model.TransactionAttachment;
import com.fin.model.User;
import com.fin.repository.TransactionAttachmentRepository;
import com.fin.repository.TransactionRepository;
import com.fin.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionAttachmentService {
    
    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;
    
    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;
    
    @Value("${minio.secret-key:minioadmin123}")
    private String minioSecretKey;
    
    @Value("${minio.bucket-name:fin-attachments}")
    private String bucketName;
    
    @Autowired
    private TransactionAttachmentRepository attachmentRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    private MinioClient minioClient;
    
    private MinioClient getMinioClient() {
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(minioAccessKey, minioSecretKey)
                    .build();
            
            // Criar bucket se não existir
            try {
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                }
            } catch (Exception e) {
                throw new RuntimeException("Erro ao configurar MinIO: " + e.getMessage());
            }
        }
        return minioClient;
    }
    
    /**
     * Faz upload de um anexo para uma transação
     */
    @Transactional
    public TransactionAttachmentDto uploadAttachment(Long transactionId, MultipartFile file, String description, Long userId) {
        // Verificar se o usuário tem assinatura ativa
        if (!subscriptionService.isSubscriptionActive(userId)) {
            throw new RuntimeException("Assinatura expirada. Renove sua assinatura para continuar.");
        }
        
        // Verificar se a transação pertence ao usuário
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        // Gerar nome único para o arquivo
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName != null && originalFileName.contains(".") 
            ? originalFileName.substring(originalFileName.lastIndexOf(".")) 
            : "";
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Salvar arquivo no MinIO
        String objectName = "transactions/" + transactionId + "/" + uniqueFileName;
        try {
            MinioClient client = getMinioClient();
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upload para MinIO: " + e.getMessage());
        }
        
        // Criar registro no banco
        TransactionAttachment attachment = new TransactionAttachment();
        attachment.setTransaction(transaction);
        attachment.setUser(user);
        attachment.setFileName(originalFileName);
        attachment.setFilePath(objectName); // Armazenar o caminho do objeto no MinIO
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setDescription(description);
        
        attachment = attachmentRepository.save(attachment);
        
        return convertToDto(attachment);
    }
    
    /**
     * Lista anexos de uma transação
     */
    public List<TransactionAttachmentDto> getTransactionAttachments(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        
        if (!transaction.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        List<TransactionAttachment> attachments = attachmentRepository.findByTransactionId(transactionId);
        return attachments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtém um anexo específico
     */
    public TransactionAttachmentDto getAttachment(Long attachmentId, Long userId) {
        TransactionAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Anexo não encontrado"));
        
        if (!attachment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        return convertToDto(attachment);
    }
    
    /**
     * Deleta um anexo
     */
    @Transactional
    public void deleteAttachment(Long attachmentId, Long userId) {
        TransactionAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Anexo não encontrado"));
        
        if (!attachment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        // Deletar arquivo do MinIO
        try {
            MinioClient client = getMinioClient();
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(attachment.getFilePath())
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar arquivo do MinIO: " + e.getMessage());
        }
        
        // Deletar registro
        attachmentRepository.delete(attachment);
    }
    
    /**
     * Obtém o arquivo do MinIO
     */
    public InputStream getAttachmentFile(Long attachmentId, Long userId) {
        TransactionAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Anexo não encontrado"));
        
        if (!attachment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Acesso negado");
        }
        
        try {
            MinioClient client = getMinioClient();
            return client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(attachment.getFilePath())
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter arquivo do MinIO: " + e.getMessage());
        }
    }
    
    private TransactionAttachmentDto convertToDto(TransactionAttachment attachment) {
        TransactionAttachmentDto dto = new TransactionAttachmentDto();
        dto.setId(attachment.getId());
        dto.setTransactionId(attachment.getTransaction().getId());
        dto.setFileName(attachment.getFileName());
        dto.setFilePath(attachment.getFilePath());
        dto.setFileType(attachment.getFileType());
        dto.setFileSize(attachment.getFileSize());
        dto.setDescription(attachment.getDescription());
        dto.setDownloadUrl("/api/attachments/" + attachment.getId() + "/download");
        dto.setCreatedAt(attachment.getCreatedAt());
        return dto;
    }
}

