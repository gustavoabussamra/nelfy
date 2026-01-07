# 💰 Nelfy - Sistema de Controle Financeiro SaaS

Nelfy - Sua vida e seu negócio em equilíbrio.

Sistema completo de controle financeiro desenvolvido como SaaS (Software as a Service) com backend em Java Spring Boot, frontend em React e banco de dados MySQL. Todas as funcionalidades estão prontas para uso e o sistema está configurado para rodar via Docker Compose.

## 🚀 Funcionalidades

### 📊 Gestão Financeira
- ✅ Cadastro de receitas e despesas
- ✅ Categorização de transações
- ✅ Dashboard com gráficos e estatísticas
- ✅ Relatórios de saldo por período
- ✅ Histórico completo de transações

### 👥 Sistema de Usuários
- ✅ Autenticação com JWT
- ✅ Registro e login de usuários
- ✅ Perfil do usuário
- ✅ Controle de acesso por usuário

### 💳 Sistema de Assinatura
- ✅ Planos de assinatura (Grátis, Básico, Premium, Empresarial)
- ✅ Controle de expiração de assinaturas
- ✅ Verificação automática de assinatura ativa
- ✅ Upgrade/downgrade de planos
- ✅ Período de teste gratuito (30 dias)

### 🎨 Interface Moderna
- ✅ Design moderno inspirado no Organizze
- ✅ Interface responsiva
- ✅ Gráficos interativos
- ✅ Notificações toast
- ✅ Layout intuitivo e agradável

## 🛠️ Tecnologias

### Backend
- **Java 17**
- **Spring Boot 3.1.5**
- **Spring Security** (JWT)
- **Spring Data JPA**
- **MySQL 8.0**
- **Maven**

### Frontend
- **React 18**
- **React Router DOM**
- **Axios**
- **Chart.js** (Gráficos)
- **React Toastify** (Notificações)
- **Date-fns** (Manipulação de datas)

### Infraestrutura
- **Docker** e **Docker Compose**
- **MySQL** (Banco de dados)

## 📋 Pré-requisitos

- Docker e Docker Compose instalados
- Git (opcional, para clonar o repositório)

## 🚀 Como Executar

### 1. Clone o repositório (ou navegue até a pasta do projeto)

```bash
cd Fin
```

### 2. Execute o Docker Compose

```bash
docker-compose up -d
```

Este comando irá:
- Criar e iniciar o container do MySQL
- Criar e iniciar o container do Backend (Spring Boot)
- Criar e iniciar o container do Frontend (React)

### 3. Acesse a aplicação

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **MySQL**: localhost:3306

### 4. Primeiro acesso

**Usuário Administrador (criado automaticamente):**
- **Email:** admin@nelfy.com
- **Senha:** admin123
- **Plano:** Enterprise (10 anos de validade)

**Para usuários normais:**
1. Acesse http://localhost:3000
2. Clique em "Cadastre-se"
3. Crie sua conta (você receberá 30 dias grátis automaticamente)
4. Faça login e comece a usar!

## 📁 Estrutura do Projeto

```
Nelfy/
├── backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/fin/
│   │       │   ├── config/          # Configurações (JWT, Security)
│   │       │   ├── controller/      # Controllers REST
│   │       │   ├── dto/             # Data Transfer Objects
│   │       │   ├── model/           # Entidades JPA
│   │       │   ├── repository/      # Repositórios JPA
│   │       │   ├── security/        # Segurança e JWT
│   │       │   └── service/         # Lógica de negócio
│   │       └── resources/
│   │           └── application.properties
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── components/              # Componentes React
│   │   ├── context/                 # Context API (Auth)
│   │   ├── pages/                   # Páginas da aplicação
│   │   ├── services/                # Serviços API
│   │   ├── App.js
│   │   └── index.js
│   ├── Dockerfile
│   └── package.json
├── docker-compose.yml
└── README.md
```

## 🔐 Autenticação

O sistema utiliza JWT (JSON Web Tokens) para autenticação. Ao fazer login ou registro, um token é gerado e armazenado no localStorage do navegador. Este token é enviado automaticamente em todas as requisições subsequentes.

### Endpoints de Autenticação

- `POST /api/auth/register` - Registrar novo usuário
- `POST /api/auth/login` - Fazer login

## 📊 APIs Disponíveis

### Transações
- `GET /api/transactions` - Listar todas as transações do usuário
- `GET /api/transactions/{id}` - Obter transação específica
- `POST /api/transactions` - Criar nova transação
- `PUT /api/transactions/{id}` - Atualizar transação
- `DELETE /api/transactions/{id}` - Excluir transação
- `GET /api/transactions/balance` - Obter saldo por período

### Categorias
- `GET /api/categories` - Listar todas as categorias do usuário
- `POST /api/categories` - Criar nova categoria
- `PUT /api/categories/{id}` - Atualizar categoria
- `DELETE /api/categories/{id}` - Excluir categoria

### Assinatura
- `GET /api/subscriptions/me` - Obter assinatura do usuário atual
- `PUT /api/subscriptions/me?plan={PLAN}` - Atualizar plano de assinatura

## 💰 Planos de Assinatura

### Grátis (FREE)
- 30 dias grátis
- Transações ilimitadas

### Básico (BASIC) - R$ 29,90/mês
- Transações ilimitadas
- Categorias personalizadas
- Relatórios básicos

### Premium (PREMIUM) - R$ 59,90/mês
- Tudo do Básico
- Relatórios avançados
- Exportação de dados
- Suporte prioritário

### Empresarial (ENTERPRISE) - R$ 99,90/mês
- Tudo do Premium
- Múltiplos usuários
- API personalizada
- Suporte 24/7

## 🔧 Configuração

### Variáveis de Ambiente

#### Backend (application.properties)
```properties
# Database
spring.datasource.url=jdbc:mysql://mysql:3306/nelfy_system
spring.datasource.username=nelfy_user
spring.datasource.password=nelfy_password

# JWT
jwt.secret=your-super-secret-jwt-key-change-in-production-min-256-bits
jwt.expiration=86400000
```

#### Frontend
A URL da API é configurada via variável de ambiente:
- `REACT_APP_API_URL=http://localhost:8080/api`

### Banco de Dados

O MySQL é configurado automaticamente via Docker Compose:
- **Database**: nelfy_system
- **User**: nelfy_user
- **Password**: nelfy_password
- **Port**: 3306

## 🛑 Parar a Aplicação

```bash
docker-compose down
```

Para remover também os volumes (dados do banco):

```bash
docker-compose down -v
```

## 📝 Notas Importantes

1. **Segurança**: Em produção, altere o `jwt.secret` para uma chave segura e única.
2. **Banco de Dados**: Os dados são persistidos em um volume Docker. Para backup, copie o volume `mysql_data`.
3. **Performance**: Para produção, considere adicionar cache (Redis) e otimizações de banco de dados.
4. **Pagamentos**: Este sistema não inclui integração com gateways de pagamento. Você precisará integrar com serviços como Stripe, PagSeguro, etc.

## 🎯 Próximos Passos (Sugestões)

- [ ] Integração com gateway de pagamento (Stripe, PagSeguro)
- [ ] Envio de emails (confirmação, recuperação de senha, notificações)
- [ ] Exportação de relatórios em PDF/Excel
- [ ] Dashboard com mais gráficos e análises
- [ ] Sistema de metas financeiras
- [ ] Aplicativo mobile (React Native)
- [ ] Integração com bancos (Open Banking)
- [ ] Sistema de tags para transações
- [ ] Anexos de comprovantes
- [ ] Relatórios personalizados

## 📄 Licença

Este projeto foi desenvolvido como um sistema SaaS completo. Você pode usá-lo como base para seu próprio produto.

## 🤝 Suporte

Para dúvidas ou problemas, verifique os logs dos containers:

```bash
# Ver logs do backend
docker-compose logs backend

# Ver logs do frontend
docker-compose logs frontend

# Ver logs do MySQL
docker-compose logs mysql

# Ver todos os logs
docker-compose logs -f
```

---

**Desenvolvido com ❤️ para um sistema financeiro completo e moderno**

