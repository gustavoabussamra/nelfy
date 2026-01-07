FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Configurar timezone para São Paulo
RUN apk add --no-cache tzdata
ENV TZ=America/Sao_Paulo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Instalar Maven
RUN apk add --no-cache maven

# Copiar pom.xml primeiro para cache de dependências
COPY pom.xml .

# Baixar dependências Maven (cache)
RUN mvn dependency:go-offline -B || true

# Copiar código fonte
COPY src ./src

# Construir a aplicação usando Maven instalado
RUN mvn clean package -DskipTests

# Expor porta
EXPOSE 8082

# Comando para executar com timezone configurado
CMD ["java", "-Duser.timezone=America/Sao_Paulo", "-jar", "target/transaction-consumer-0.0.1-SNAPSHOT.jar"]








