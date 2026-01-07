-- Script SQL para criar usuário admin
-- Execute este script no banco de dados MySQL

-- Hash BCrypt para a senha "gustavo123"
-- Este hash foi gerado usando BCryptPasswordEncoder
INSERT INTO users (email, password, name, role, referral_code, created_at, updated_at)
VALUES (
  'gustavo.abussamra@gmail.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'Gustavo Admin',
  'ADMIN',
  CONCAT('ADMIN', FLOOR(RAND() * 1000000)),
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE email=email;




