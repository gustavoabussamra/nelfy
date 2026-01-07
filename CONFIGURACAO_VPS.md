# Configuração para VPS Hostinger

## 📋 Resumo das Alterações de Portas

Este projeto foi configurado para rodar na VPS **72.61.134.94** sem conflitos com o projeto TagPetz.

### 🔌 Portas Configuradas

| Serviço | Porta Externa | Porta Interna | Observação |
|---------|--------------|---------------|------------|
| **MySQL** | 3307 | 3306 | TagPetz usa 3306 |
| **Backend** | 8085 | 8080 | TagPetz usa 8080 |
| **Frontend** | 3002 | 3000 | TagPetz usa 3000 |
| **Landing Page** | 3001 | 80 | - |
| **MinIO** | 9002 | 9000 | TagPetz usa 9000 |
| **MinIO Console** | 9003 | 9001 | TagPetz usa 9001 |
| **Redis** | 6380 | 6379 | - |
| **Kafka** | 9094 | 9092 | - |
| **Kafka (Interno)** | 9095 | 9093 | - |
| **Zookeeper** | 2182 | 2181 | - |
| **Kafka UI** | 8083 | 8080 | TagPetz usa 8081 (phpMyAdmin) |
| **Transaction Consumer** | 8084 | 8082 | - |

### 🌐 URLs de Acesso

- **Frontend**: http://72.61.134.94:3002
- **Landing Page**: http://72.61.134.94:3001
- **Backend API**: http://72.61.134.94:8085/api
- **Kafka UI**: http://72.61.134.94:8083
- **MinIO Console**: http://72.61.134.94:9003

### 🔒 Configurações de CORS

O CORS foi configurado para aceitar requisições de:
- `http://72.61.134.94:3002` (Frontend na VPS)
- `http://72.61.134.94:3001` (Landing Page na VPS)
- `http://localhost:3002` (Desenvolvimento local)
- `http://localhost:3001` (Desenvolvimento local)
- `http://localhost:3000` (Desenvolvimento local alternativo)

### 🚀 Como Executar na VPS

1. **Clone ou faça upload do projeto na VPS**

2. **Certifique-se de que o TagPetz está rodando nas portas padrão**

3. **Execute o docker-compose:**
```bash
docker-compose up -d
```

4. **Verifique os logs:**
```bash
docker-compose logs -f backend
docker-compose logs -f frontend
```

5. **Acesse o sistema:**
   - Frontend: http://72.61.134.94:3002
   - Landing Page: http://72.61.134.94:3001

### 📝 Variáveis de Ambiente

As seguintes variáveis são usadas automaticamente:

**Frontend:**
- `REACT_APP_API_URL`: http://72.61.134.94:8085/api

**Landing Page (build time):**
- `VITE_API_URL`: http://72.61.134.94:8085
- `VITE_FRONTEND_URL`: http://72.61.134.94:3002

### ⚠️ Importante

- **Firewall**: Certifique-se de que as portas 3001, 3002, 8085, 3307, 9002, 9003, 6380, 9094, 9095, 2182, 8083, 8084 estão abertas no firewall da VPS
- **Nginx/Apache**: Se você usar um proxy reverso, configure para redirecionar para essas portas
- **SSL/HTTPS**: Para produção, configure SSL usando Let's Encrypt ou similar

### 🔧 Troubleshooting

**Problema: CORS Error**
- Verifique se o IP está correto em `SecurityConfig.java` e `application.properties`
- Certifique-se de que o backend está rodando na porta 8085

**Problema: Frontend não conecta ao backend**
- Verifique a variável `REACT_APP_API_URL` no docker-compose.yml
- Verifique os logs do frontend: `docker-compose logs frontend`

**Problema: Porta já em uso**
- Verifique se o TagPetz não está usando a mesma porta
- Use `netstat -tulpn | grep PORTA` para verificar portas em uso

### 📊 Comparação com TagPetz

| Serviço | TagPetz | Nelfy (Fin) | Status |
|---------|---------|-------------|--------|
| MySQL | 3306 | 3307 | ✅ Sem conflito |
| Backend | 8080 | 8085 | ✅ Sem conflito |
| Frontend | 3000 | 3002 | ✅ Sem conflito |
| MinIO | 9000/9001 | 9002/9003 | ✅ Sem conflito |
| phpMyAdmin | 8081 | - | ✅ Sem conflito |
| Kafka UI | - | 8083 | ✅ Sem conflito |

---

**Última atualização**: Configurado para VPS 72.61.134.94
