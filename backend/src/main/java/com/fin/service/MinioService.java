package com.fin.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
public class MinioService {
    
    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;
    
    @Value("${minio.access-key:minioadmin}")
    private String minioAccessKey;
    
    @Value("${minio.secret-key:minioadmin123}")
    private String minioSecretKey;
    
    private MinioClient minioClient;
    
    private MinioClient getMinioClient() {
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(minioAccessKey, minioSecretKey)
                    .build();
        }
        return minioClient;
    }
    
    /**
     * Garante que o bucket existe
     */
    private void ensureBucketExists(String bucketName) {
        try {
            MinioClient client = getMinioClient();
            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar/criar bucket no MinIO: " + e.getMessage());
        }
    }
    
    /**
     * Faz upload de um arquivo para o MinIO
     */
    public String uploadFile(String bucketName, String fileName, MultipartFile file) {
        try {
            ensureBucketExists(bucketName);
            
            MinioClient client = getMinioClient();
            String objectName = fileName;
            
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer upload para MinIO: " + e.getMessage());
        }
    }
    
    /**
     * Obtém URL pré-assinada para acessar o arquivo
     */
    public String getFileUrl(String bucketName, String objectName) {
        try {
            MinioClient client = getMinioClient();
            return client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(60 * 60 * 24) // 24 horas
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter URL do arquivo: " + e.getMessage());
        }
    }
    
    /**
     * Obtém o arquivo como InputStream
     */
    public InputStream getFile(String bucketName, String objectName) {
        try {
            MinioClient client = getMinioClient();
            return client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter arquivo do MinIO: " + e.getMessage());
        }
    }
    
    /**
     * Deleta um arquivo do MinIO
     */
    public void deleteFile(String bucketName, String objectName) {
        try {
            MinioClient client = getMinioClient();
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar arquivo do MinIO: " + e.getMessage());
        }
    }
}

