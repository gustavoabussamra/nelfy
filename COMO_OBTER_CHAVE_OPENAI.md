# 🔑 Como Obter e Configurar a Chave da API OpenAI

## 📍 Onde Obter a Chave

### 1. Acesse o Site da OpenAI
👉 **URL**: https://platform.openai.com/api-keys

### 2. Faça Login ou Crie uma Conta
- Se já tem conta: faça login
- Se não tem: crie uma conta gratuita (pode usar Google/GitHub)

### 3. Crie uma Nova Chave
1. Clique em **"Create new secret key"**
2. Dê um nome para a chave (ex: "Fin System - Dev")
3. **COPIE A CHAVE IMEDIATAMENTE** - ela só aparece uma vez!
4. Formato da chave: `sk-proj-...` ou `sk-...`

### 💰 Crédito Gratuito
- Novos usuários recebem **US$ 5 de crédito gratuito**
- Perfeito para testar o sistema
- Crédito não expira (mas tem data limite de uso)

---

## ⚙️ Como Configurar no Sistema

### **Opção 1: Docker Compose (RECOMENDADO)** ⭐

1. Abra o arquivo `docker-compose.yml`
2. Adicione a chave na seção `environment` do backend:
```yaml
backend:
  environment:
    OPENAI_API_KEY: sk-sua-chave-aqui
```

3. Ou crie um arquivo `.env` na raiz do projeto:
```bash
OPENAI_API_KEY=sk-sua-chave-aqui
```

4. E adicione no `docker-compose.yml`:
```yaml
backend:
  env_file:
    - .env
```

### **Opção 2: Variável de Ambiente no Sistema**

**Windows (PowerShell):**
```powershell
$env:OPENAI_API_KEY="sk-sua-chave-aqui"
```

**Linux/Mac:**
```bash
export OPENAI_API_KEY=sk-sua-chave-aqui
```

### **Opção 3: application.properties**

Edite `backend/src/main/resources/application.properties`:
```properties
openai.api.key=sk-sua-chave-aqui
```

⚠️ **ATENÇÃO**: Não commite este arquivo no Git com a chave!

---

## 🧪 Como Testar

### 1. Verificar se a chave está configurada
Após configurar, faça rebuild:
```bash
docker-compose down
docker-compose up --build -d
```

### 2. Testar a IA de Transações
- Vá para a página de Transações
- Use o campo de input da IA
- Digite: "gastei com mercado o valor de 50 reais"
- A IA deve criar a transação automaticamente

### 3. Testar Análise Financeira
- Vá para o Dashboard
- A análise financeira deve aparecer com recomendações da IA

---

## 💵 Custos e Monitoramento

### Preços (GPT-4o-mini - modelo mais barato)
- **Input**: ~US$ 0,15 por 1M tokens
- **Output**: ~US$ 0,60 por 1M tokens
- **Custo por transação**: ~US$ 0,0005-0,001

### Monitoramento
- Acompanhe uso em: https://platform.openai.com/usage
- Configure limites em: https://platform.openai.com/account/billing/limits

### Estimativa de Custos
- **100 transações/mês**: ~US$ 0,05-0,10
- **1.000 transações/mês**: ~US$ 0,50-1,00
- **10.000 transações/mês**: ~US$ 5,00-10,00

---

## 🔒 Segurança

- ✅ **NUNCA** compartilhe sua API key publicamente
- ✅ **NUNCA** commite a chave no Git
- ✅ Use variáveis de ambiente ou arquivo `.env` (não versionado)
- ✅ Configure limites de uso na OpenAI
- ✅ Monitore o uso regularmente

---

## ❓ Problemas Comuns

### "OpenAI API key não configurada"
- Verifique se a variável `OPENAI_API_KEY` está configurada
- Faça rebuild do Docker: `docker-compose up --build -d`

### "Erro ao chamar OpenAI"
- Verifique se a chave está correta
- Verifique se tem crédito disponível
- Verifique os logs do backend

### Sistema não usa OpenAI
- O sistema funciona sem OpenAI (usando padrões hardcoded)
- Para habilitar: configure `OPENAI_API_KEY`
- Para desabilitar: `AI_USE_LEARNING=false`

---

## 📚 Links Úteis

- **Obter Chave**: https://platform.openai.com/api-keys
- **Monitorar Uso**: https://platform.openai.com/usage
- **Configurar Limites**: https://platform.openai.com/account/billing/limits
- **Documentação API**: https://platform.openai.com/docs

---

**Pronto!** Após configurar a chave, o sistema usará IA para:
- ✅ Criar transações a partir de texto natural
- ✅ Corrigir ortografia automaticamente
- ✅ Aprender padrões de uso
- ✅ Gerar análises financeiras inteligentes








