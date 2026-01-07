-- Script para inserir comissões de teste para o usuário ID 1
-- Este script cria um usuário de teste indicado, um referral e 3 comissões

-- 1. Criar usuário de teste indicado (se não existir)
INSERT INTO users (email, password, name, role, referral_code, created_at, updated_at)
SELECT 
    'teste.indicado@example.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', -- senha: teste123
    'Usuário Teste Indicado',
    'USER',
    CONCAT('TEST', FLOOR(RAND() * 1000000)),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'teste.indicado@example.com'
);

-- 2. Obter o ID do usuário indicado criado
SET @referred_user_id = (SELECT id FROM users WHERE email = 'teste.indicado@example.com' LIMIT 1);

-- 3. Obter o referral_code do usuário ID 1 (referrer)
SET @referrer_code = (SELECT referral_code FROM users WHERE id = 1 LIMIT 1);

-- 4. Criar o referral (se não existir)
INSERT INTO referrals (referrer_id, referred_id, referral_code, reward_given, created_at, updated_at)
SELECT 
    1, -- usuário ID 1 é o referrer
    @referred_user_id,
    CONCAT(@referrer_code, '-', @referred_user_id),
    0, -- reward_given = false
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM referrals WHERE referred_id = @referred_user_id
);

-- 5. Obter o ID do referral criado
SET @referral_id = (SELECT id FROM referrals WHERE referred_id = @referred_user_id LIMIT 1);

-- 6. Deletar comissões existentes para este referral (para evitar duplicatas)
DELETE FROM referral_commissions WHERE referral_id = @referral_id;

-- 7. Inserir 3 comissões de teste (últimos 3 meses)
-- Mês atual - Plano PREMIUM (R$ 59,90)
INSERT INTO referral_commissions (
    referral_id, 
    payment_year, 
    payment_month, 
    subscription_plan, 
    monthly_amount, 
    commission_rate, 
    commission_amount, 
    payment_date, 
    created_at
)
VALUES (
    @referral_id,
    YEAR(NOW()),
    MONTH(NOW()),
    'PREMIUM',
    59.90,
    0.10,
    5.99, -- 10% de 59.90
    NOW(),
    NOW()
);

-- Mês anterior - Plano BASIC (R$ 29,90)
INSERT INTO referral_commissions (
    referral_id, 
    payment_year, 
    payment_month, 
    subscription_plan, 
    monthly_amount, 
    commission_rate, 
    commission_amount, 
    payment_date, 
    created_at
)
VALUES (
    @referral_id,
    YEAR(DATE_SUB(NOW(), INTERVAL 1 MONTH)),
    MONTH(DATE_SUB(NOW(), INTERVAL 1 MONTH)),
    'BASIC',
    29.90,
    0.10,
    2.99, -- 10% de 29.90
    DATE_SUB(NOW(), INTERVAL 1 MONTH),
    NOW()
);

-- 2 meses atrás - Plano ENTERPRISE (R$ 149,90)
INSERT INTO referral_commissions (
    referral_id, 
    payment_year, 
    payment_month, 
    subscription_plan, 
    monthly_amount, 
    commission_rate, 
    commission_amount, 
    payment_date, 
    created_at
)
VALUES (
    @referral_id,
    YEAR(DATE_SUB(NOW(), INTERVAL 2 MONTH)),
    MONTH(DATE_SUB(NOW(), INTERVAL 2 MONTH)),
    'ENTERPRISE',
    149.90,
    0.10,
    14.99, -- 10% de 149.90
    DATE_SUB(NOW(), INTERVAL 2 MONTH),
    NOW()
);

-- Verificar resultado
SELECT 
    'Comissões criadas com sucesso!' AS mensagem,
    COUNT(*) AS total_comissoes,
    SUM(commission_amount) AS total_valor
FROM referral_commissions 
WHERE referral_id = @referral_id;



