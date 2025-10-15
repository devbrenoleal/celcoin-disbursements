# Sistema de Desembolso (Disbursement)

Este projeto é um microsserviço desenvolvido em Java com Spring Boot, projetado para gerenciar e processar lotes de desembolsos financeiros. A arquitetura é orientada a eventos, utilizando Apache Kafka para garantir um processamento assíncrono, resiliente e escalável.

## Visão Geral da Arquitetura

O sistema funciona recebendo solicitações de desembolso via uma API REST. Cada solicitação pode conter um lote de várias transações, que podem ser de diferentes tipos (como PIX e TED).

1.  **API Gateway**: O `DisbursementController` expõe endpoints para criar lotes de desembolso e consultar seu status.
2.  **Persistência**: As informações do lote (`disbursement_batch`) e suas etapas (`disbursement_step`) são salvas em um banco de dados relacional, gerenciado via Liquibase.
3.  **Mensageria**: Para desembolsos de execução imediata ou agendada, eventos são publicados em tópicos do Apache Kafka. Cada tipo de pagamento tem seu próprio tópico para evitar bloqueios (`Head-of-Line Blocking`).
4.  **Processamento Assíncrono**: `Consumers` do Kafka (`DisbursementRequestConsumer`) escutam os tópicos e acionam o `DisbursementProcessingService`.
5.  **Orquestração e Estratégia**: O `DisbursementOrchestrator` utiliza o padrão *Strategy* para invocar a estratégia de pagamento correta (`PixDisbursementStrategy` ou `TedDisbursementStrategy`).
6.  **Adapters de Pagamento**: As estratégias delegam a chamada para `Adapters` específicos (`PixAdapter`, `TedAdapter`), que são responsáveis pela comunicação com os gateways de pagamento externos.
7.  **Notificações (Webhooks)**: A aplicação expõe endpoints (`NotificationController`) para receber atualizações de status assíncronas dos sistemas de pagamento (ex: confirmação de um PIX).
8.  **Agendamento**: Um `DisbursementSchedulerService` executa periodicamente para disparar lotes com agendamento futuro ou recorrente (diário, semanal, mensal, anual).
9.  **Idempotência**: Um `IdempotencyService`, com suporte da tabela `processed_events`, garante que mensagens e agendamentos não sejam processados mais de uma vez.

## Principais Recursos

-   **Criação de Lotes de Desembolso**: Envio de múltiplos pagamentos em uma única requisição.
-   **Múltiplos Meios de Pagamento**: Suporte extensível a diferentes canais, iniciando com PIX e TED.
-   **Processamento Assíncrono**: Uso de Kafka para desacoplar a requisição do processamento, aumentando a resiliência e a performance.
-   **Agendamento**: Suporte para desembolsos imediatos, agendados e recorrentes.
-   **Consulta de Status**: Endpoint para acompanhar o status de um lote e de cada transação individual.
-   **Retentativas e Dead Letter Queue (DLT)**: Mecanismo de retentativa automática para mensagens com falha e um tópico DLT para análise de erros persistentes.
-   **Segurança**: Autenticação e autorização via OAuth2/JWT para os endpoints principais.

## Tecnologias Utilizadas

-   **Java 25**
-   **Spring Boot 3.5.6**
-   **Spring Data JPA** (com Hibernate)
-   **Spring Kafka**
-   **Spring Security** (com OAuth2/JWT Resource Server)
-   **Liquibase** (para migrações de banco de dados)
-   **MySQL Connector**
-   **Lombok**
-   **Testcontainers** (para testes de integração)

---

## Endpoints da API

### `POST /disbursements`

Cria um novo lote de desembolso.

**Request Body:**

```json
{
  "clientCode": "UNIQUE_CLIENT_CODE_123",
  "schedule": {
    "type": "IMMEDIATE", // IMMEDIATE, SCHEDULED, RECURRENT
    "date": "2025-10-26T10:00:00" // Necessário para SCHEDULED e RECURRENT
  },
  "disbursements": [
    {
      "type": "PIX",
      "disbursementStep": {
        "amount": 150.75,
        "creditParty": {
          // ... detalhes do destinatário do PIX
        },
        "initiationType": "MANUAL"
      }
    },
    {
      "type": "TED",
      "disbursementStep": {
        "amount": 2300.00,
        "creditParty": {
          // ... detalhes do destinatário da TED
        }
      }
    }
  ]
}
```

**Success Response (200 OK):**

```json
{
  "batchId": "uuid-gerado-pelo-sistema",
  "status": "PROCESSING"
}
```

### `GET /disbursements/{clientCode}/status`

Consulta o status de um lote de desembolso.

**Success Response (200 OK):**

```json
{
  "batchId": "uuid-gerado-pelo-sistema",
  "status": "PROCESSING",
  "clientCode": "UNIQUE_CLIENT_CODE_123",
  "steps": [
    {
      "stepId": "uuid-step-1",
      "status": "COMPLETED",
      "externalId": "external-transaction-id-pix"
    },
    {
      "stepId": "uuid-step-2",
      "status": "PENDING",
      "externalId": null
    }
  ]
}
```

### `POST /notifications/pix`

Endpoint de webhook para receber notificações de status de transações PIX. A segurança deste endpoint deve ser garantida por verificação de assinatura (ex: HMAC), conforme anotado no código.

**Request Body:**

```json
{
  "externalId": "external-transaction-id-pix",
  "status": "CONFIRMED",
  "failureReason": null
}
```

---

## Como Executar o Projeto

### Pré-requisitos

-   Java 25+
-   Docker e Docker Compose (para rodar Kafka e MySQL/PostgreSQL)
-   Maven
-   Um Identity Provider compatível com OAuth2/JWT (como Keycloak) para a segurança.
-   O SpringSecurity está desabilitado, mas está pronto para utilizar um IdP para autenticação das requisições.

### 1. Configuração do Ambiente

Utilize o Docker Compose para subir as instâncias do Kafka, Zookeeper e do banco de dados (ex: MySQL).

### 2. Configuração da Aplicação

Configure o arquivo `application.properties` ou `application.yml` com as informações de conexão do banco de dados, do Kafka e do seu provedor de identidade.

**Exemplo (`application.properties`):**

```properties
# Spring Datasource
spring.datasource.url=jdbc:mysql://localhost:3306/disbursement_db
spring.datasource.username=user
spring.datasource.password=password

# Liquibase
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml

# Spring Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.group-id=disbursement-processor
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*

# Spring Security (OAuth2)
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/your-realm
```

### 3. Build e Execução

Compile e execute a aplicação usando o Maven:

```bash
# Compilar o projeto
mvn clean install

# Executar a aplicação
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080` (ou na porta configurada).
