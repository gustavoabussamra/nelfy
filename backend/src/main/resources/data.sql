-- Script para criar usuário admin
-- A senha será criptografada via BCrypt
-- Este script será executado apenas se a tabela users estiver vazia

-- Hash BCrypt para a senha "gustavo123"
-- Para gerar: usar BCryptPasswordEncoder ou executar via endpoint
-- Hash gerado: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

-- NOTA: Este INSERT precisa ser executado via aplicação Java para criptografar a senha corretamente
-- Ou você pode executar manualmente após gerar o hash BCrypt

-- Exemplo de INSERT (substitua o hash pela senha criptografada):
-- INSERT INTO users (email, password, name, role, referral_code, created_at, updated_at)
-- VALUES (
--   'gustavo.abussamra@gmail.com',
--   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- senha: gustavo123
--   'Gustavo Admin',
--   'ADMIN',
--   'ADMIN' || FLOOR(RAND() * 1000000),
--   NOW(),
--   NOW()
-- )
-- ON DUPLICATE KEY UPDATE email=email;




