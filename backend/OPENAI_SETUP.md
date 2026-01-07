# Configuração da API OpenAI

## 📋 Informações sobre Custos

### Preços Atuais (GPT-4o-mini)
- **Input**: ~US$ 0,150 por 1M tokens
- **Output**: ~US$ 0,600 por 1M tokens
- **Custo por análise**: ~US$ 0,01 - 0,03

### Custo Estimado Mensal
- **100 análises**: ~US$ 1-3/mês
- **1.000 análises**: ~US$ 10-30/mês
- **10.000 análises**: ~US$ 100-300/mês

### 💰 Crédito Inicial
A OpenAI oferece **US$ 5 de crédito gratuito** para novos usuários testarem a API.

## 🔑 Como Obter a API Key

1. Acesse: https://platform.openai.com/api-keys
2. Faça login ou crie uma conta
3. Clique em "Create new secret key"
4. Copie a chave (ela só aparece uma vez!)

## ⚙️ Como Configurar

### Opção 1: Variável de Ambiente (Recomendado)
```bash
export OPENAI_API_KEY=sk-...
```

### Opção 2: Docker Compose
Adicione no arquivo `docker-compose.yml`:
```yaml
services:
  backend:
    environment:
      - OPENAI_API_KEY=sk-...
```

### Opção 3: application.properties
```properties
openai.api.key=sk-...
```

## 🎯 Otimizações de Custo

O código já está otimizado para:
- ✅ Usar GPT-4o-mini (modelo mais barato)
- ✅ Limitar tokens de resposta (max_tokens: 1000)
- ✅ Prompts concisos
- ✅ Fallback para análise básica se API key não estiver configurada

## 🔄 Alternativas Gratuitas

Se preferir não usar OpenAI, o sistema funciona com análise básica:
- Análise de padrões de gastos
- Top categorias
- Recomendações genéricas
- Cálculo de economia potencial

## 📊 Monitoramento de Uso

Acompanhe seu uso em: https://platform.openai.com/usage

## ⚠️ Importante

- **Nunca compartilhe sua API key publicamente**
- Configure limites de uso na OpenAI para evitar surpresas
- A API key é gratuita, mas o uso é cobrado por tokens
- Comece com o crédito de US$ 5 para testar









