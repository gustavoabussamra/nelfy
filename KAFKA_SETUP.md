# Configuração do Kafka

## Arquitetura

O sistema agora utiliza uma arquitetura de microserviços com Kafka:

1. **API Principal (Backend)** - Porta 8080
   - Produz mensagens de transações no tópico `transactions`
   - Não persiste diretamente no banco (apenas para transações)

2. **Transaction Consumer Service** - Porta 8082
   - Consome mensagens do tópico `transactions`
   - Persiste transações no banco de dados
   - Processa transações parceladas criando todas as parcelas

3. **Kafka** - Porta 9092
   - Broker de mensagens
   - Tópico: `transactions` (3 partições)

4. **Kafka UI** - Porta 8081
   - Interface web para monitorar o Kafka
   - **Login**: admin
   - **Senha**: kafka123

## Serviços no Docker

- `fin_mysql` - Banco de dados MySQL
- `fin_backend` - API principal (produtor Kafka)
- `fin_frontend` - Frontend React
- `fin_zookeeper` - Zookeeper para Kafka
- `fin_kafka` - Kafka Broker
- `fin_kafka_ui` - Interface web do Kafka
- `fin_transaction_consumer` - Consumer que persiste transações

## Acessos

- Frontend: http://localhost:3000
- API Principal: http://localhost:8080
- Consumer Service: http://localhost:8082
- Kafka UI: http://localhost:8081
  - Usuário: `admin`
  - Senha: `kafka123`

## Fluxo de Criação de Transação

1. Usuário cria transação via frontend
2. Frontend chama API principal (`/api/transactions`)
3. API principal valida e envia mensagem para Kafka
4. Consumer Service recebe mensagem do Kafka
5. Consumer Service persiste no banco de dados
6. Frontend pode fazer polling ou usar WebSocket para atualizar

## Notas

- Apenas **transações** passam pelo Kafka
- **Categorias** continuam sendo persistidas diretamente (baixo volume)
- O consumer processa transações parceladas criando todas as parcelas automaticamente








