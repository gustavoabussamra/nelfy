-- Script para remover subscriptions de usuários admin
-- Execute este script no banco de dados MySQL

USE fin_db;

-- Ver subscriptions de admins
SELECT s.id, s.user_id, u.email, u.role, s.plan, s.is_active
FROM subscriptions s
INNER JOIN users u ON s.user_id = u.id
WHERE u.role = 'ADMIN';

-- Deletar subscriptions de admins
DELETE s FROM subscriptions s
INNER JOIN users u ON s.user_id = u.id
WHERE u.role = 'ADMIN';

-- Verificar se foi deletado
SELECT s.id, s.user_id, u.email, u.role, s.plan, s.is_active
FROM subscriptions s
INNER JOIN users u ON s.user_id = u.id
WHERE u.role = 'ADMIN';










