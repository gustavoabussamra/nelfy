-- Script para corrigir a role do usuário admin
-- Execute este script no banco de dados MySQL

USE nelfy_system;

-- Ver todos os usuários e suas roles
SELECT id, email, name, role, LENGTH(role) as role_length FROM users;

-- Atualizar a role para ADMIN (maiúsculas) para usuários admin
-- IMPORTANTE: Substitua 'admin@nelfy.com' pelo email do seu usuário admin
UPDATE users SET role = 'ADMIN' WHERE email LIKE '%admin%' OR email = 'admin@nelfy.com';

-- Verificar após atualização
SELECT id, email, name, role, LENGTH(role) as role_length FROM users WHERE role = 'ADMIN' OR email LIKE '%admin%';

-- Se necessário, atualizar manualmente pelo ID
-- UPDATE users SET role = 'ADMIN' WHERE id = 1;










