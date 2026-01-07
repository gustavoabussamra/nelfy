# Sistema de Aprendizado com IA

## Visão Geral

O sistema de aprendizado com IA permite que o sistema melhore continuamente o reconhecimento de padrões em textos de entrada do usuário, aprendendo com cada interação e usando OpenAI para processar e extrair informações financeiras.

## Como Funciona

### 1. Fluxo de Processamento (Otimizado para Reduzir Custos)

Quando um usuário digita um texto para criar uma transação:

1. **Estratégia 1: Cache de Padrões Aprendidos** ⚡ (ECONOMIA DE CUSTOS):
   - Sistema verifica se existe padrão similar já aprendido no banco
   - Busca padrões com alta confiança (≥70%) e similaridade (≥80%)
   - Se encontrar, usa diretamente SEM chamar OpenAI
   - **Economia: 100% do custo da chamada OpenAI**

2. **Estratégia 2: Aprendizado com OpenAI** (apenas se necessário):
   - Se padrão não encontrado no banco
   - Envia o texto + contexto histórico para OpenAI
   - OpenAI analisa e extrai informações estruturadas
   - Os padrões aprendidos são salvos no banco para futuras reutilizações

3. **Estratégia 3: Fallback para Padrões Aprendidos** (se OpenAI falhar):
   - Se OpenAI retornar erro
   - Sistema busca padrões aprendidos existentes (mesmo com baixa confiança)
   - Usa o padrão mais similar encontrado (≥50% similaridade)
   - **Garantia: Sistema sempre funciona mesmo com falha na OpenAI**

4. **Estratégia 4: Padrões Hardcoded** (último recurso):
   - Se nenhum padrão aprendido estiver disponível
   - Usa os padrões regex hardcoded tradicionais

### 2. Estrutura de Dados

#### Tabela: `ai_learning_patterns`

Armazena:
- **Texto original** e **normalizado** do usuário
- **Informações extraídas**: tipo, valor, parcelas, descrição, data, categoria
- **Padrões aprendidos** (JSON): padrões únicos identificados pelo OpenAI
- **Score de confiança**: 0.0 a 1.0
- **Notas de processamento**: observações do OpenAI sobre o processamento

### 3. Aprendizado Contínuo

O sistema aprende de duas formas:

1. **Automático**: Cada vez que um usuário cria uma transação, o padrão é salvo
2. **Treinamento Manual**: Endpoint `/api/transactions/ai/train` processa padrões não processados

### 4. Busca de Padrões Similares

O sistema pode buscar padrões similares para um texto:
- Identifica palavras-chave no texto
- Busca padrões históricos com essas palavras
- Retorna padrões ordenados por confiança

## Configuração

### application.properties

```properties
# Habilitar/desabilitar aprendizado com OpenAI
ai.use-learning=true

# OpenAI API Key (necessário para usar aprendizado)
openai.api.key=sua-chave-aqui
```

### Variáveis de Ambiente

```bash
AI_USE_LEARNING=true
OPENAI_API_KEY=sua-chave-aqui
```

## Endpoints

### POST `/api/transactions/ai/create`

Cria uma transação a partir de texto em linguagem natural.

**Request:**
```json
{
  "text": "gastei com mercado o valor de 50 reais"
}
```

**Response:**
```json
{
  "success": true,
  "transaction": { ... },
  "message": "Transação criada com sucesso!"
}
```

### POST `/api/transactions/ai/train`

Treina o sistema processando padrões históricos não processados.

**Response:**
```json
{
  "success": true,
  "message": "Sistema de aprendizado treinado com sucesso"
}
```

## Melhorias Futuras

1. **Cache de Padrões**: Cachear padrões mais usados para reduzir chamadas à OpenAI
2. **Aprendizado por Usuário**: Aprender preferências específicas de cada usuário
3. **Validação Manual**: Permitir que usuários corrijam extrações e re-treinar
4. **Métricas**: Dashboard de acurácia e padrões mais usados

## Otimizações de Custo

### Sistema de Cache Inteligente

O sistema implementa cache inteligente que **reduz drasticamente os custos**:

1. **Primeira vez**: Chama OpenAI e salva padrão (~$0.0005-0.001)
2. **Próximas vezes**: Usa padrão do banco (custo: $0.00)
3. **Economia**: Após algumas utilizações, custo médio cai para ~$0.0001-0.0002 por transação

### Cálculo de Similaridade

- **Similaridade ≥80%**: Usa padrão do banco diretamente (sem OpenAI)
- **Similaridade 50-80%**: Usa como fallback se OpenAI falhar
- **Similaridade <50%**: Chama OpenAI para aprender novo padrão

### Custos OpenAI (quando necessário)

O sistema usa `gpt-4o-mini` que é mais econômico:
- ~$0.15 por 1M tokens de entrada
- ~$0.60 por 1M tokens de saída

Cada processamento usa aproximadamente:
- 500-1000 tokens de entrada (contexto + histórico)
- 200-500 tokens de saída (JSON estruturado)

**Estimativa inicial**: ~$0.0005-0.001 por transação processada.
**Estimativa após aprendizado**: ~$0.0001-0.0002 por transação (reutilização de padrões).

## Desabilitar Aprendizado

Se quiser desabilitar o aprendizado e usar apenas padrões hardcoded:

```properties
ai.use-learning=false
```

Ou não configure a chave da OpenAI (o sistema automaticamente usará fallback).

