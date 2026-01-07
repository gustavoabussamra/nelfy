package com.fin.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Override
    public void run(String... args) throws Exception {
        // Inicialização de dados removida - controle de admin agora é feito via banco de dados
        // Para criar um usuário admin, use o painel administrativo ou atualize diretamente no banco
    }
}

