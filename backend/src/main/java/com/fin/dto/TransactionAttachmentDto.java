package com.fin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAttachmentDto {
    private Long id;
    private Long transactionId;
    private String fileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String description;
    private String downloadUrl; // URL para download
    private LocalDateTime createdAt;
}




