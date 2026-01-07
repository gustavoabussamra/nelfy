package com.fin.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class TransactionConsumerApplication {
    
    @PostConstruct
    public void init() {
        // Configurar timezone padrão para América/São Paulo
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }
    
    public static void main(String[] args) {
        // Garantir que o timezone seja configurado antes de iniciar a aplicação
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        SpringApplication.run(TransactionConsumerApplication.class, args);
    }
}








