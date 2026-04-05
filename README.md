# Order Processing System

An event-driven microservices system built with Spring Boot and Apache Kafka. Demonstrates distributed systems patterns including event-driven communication, idempotency, dead-letter queues, optimistic locking, and distributed tracing via correlation IDs.

---

## Architecture

```
Client → API Gateway (8080)
              ↓
    ┌─────────┴──────────┐
    ↓                    ↓
Order Service      Payment Service
   (8081)              (8082)
    ↓                    ↓
    └─────────┬──────────┘
              ↓
      Notification Service
           (8083)
```

### Kafka Topics

| Topic | Producer | Consumers |
|---|---|---|
| `order.created` | Order Service | Payment Service |
| `payment.processed` | Payment Service | Order Service, Notification Service |
| `payment.failed` | Payment Service | Order Service, Notification Service |
| `order.completed` | Order Service | Notification Service |
| `payment.failed.DLT` | Spring Kafka (auto) | — |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.4.5 |
| Message broker | Apache Kafka (KRaft mode) |
| Cache / idempotency | Redis 7 |
| Database | PostgreSQL 16 |
| Schema migrations | Flyway |
| Service mesh | Spring Cloud Gateway |
| Containerisation | Docker + Docker Compose |
| Build | Maven (multi-module) |

---

## Running Locally

**Prerequisites:** Docker Desktop

```bash
# 1. Clone and enter the project
git clone https://github.com/pixelm123/order-processing-system
cd order-processing-system

# 2. Create your env file
cp .env.example .env
# Edit .env and set a real JWT_SECRET (min 32 characters)

# 3. Start infrastructure
docker compose up postgres redis kafka kafka-ui -d

# 4. Build and start all services
docker compose up --build order-service payment-service notification-service api-gateway
```

**Kafka UI** is available at http://localhost:8090 — inspect topics, consumer groups, and messages.

### Running Services Individually (IntelliJ / VS Code)

Start infrastructure via Docker Compose, then run each Spring Boot app directly:

```bash
# Each service reads from application.yml (local profile by default)
# Infrastructure must be running on localhost:5432, 6379, 9092
```

---

## API Reference

All requests go through the API Gateway on port **8080**. Include a JWT in the `Authorization: Bearer <token>` header for all non-actuator endpoints.

### Order Service

#### Create Order
```
POST /orders
Content-Type: application/json

{
  "customerId": "cust-123",
  "totalAmount": 99.99,
  "idempotencyKey": "client-generated-uuid"
}
```

Response `201 Created`:
```json
{
  "id": "uuid",
  "customerId": "cust-123",
  "totalAmount": 99.99,
  "status": "PENDING",
  "correlationId": "uuid",
  "createdAt": "2026-04-05T12:00:00Z",
  "updatedAt": "2026-04-05T12:00:00Z"
}
```

#### Get Order
```
GET /orders/{id}
```

#### List Orders (paginated)
```
GET /orders?page=0&size=20&sort=createdAt,desc
```

### Payment Service

#### Get Payment by Order ID
```
GET /payments/{orderId}
```

### Notification Service

#### Get Notifications for Order
```
GET /notifications/{orderId}
```

---

## Design Decisions

### Why Kafka instead of REST between services

REST calls between services create temporal coupling — if the payment service is down, order creation fails. With Kafka, the order service publishes an event and continues; the payment service processes it whenever it is ready. Services can be deployed, restarted, or scaled independently without affecting each other. The message log also acts as an audit trail and enables replaying events for debugging or recovery.

### Database per service

Each service owns its own PostgreSQL schema (`orders_db`, `payments_db`, `notifications_db`). This enforces bounded contexts — the payment service cannot query the orders table directly, which prevents tight schema coupling. It also allows each service to evolve its schema and scale its database independently.

### How idempotency works

**Order creation:** The client generates a UUID `idempotencyKey` and includes it in the request. The order service checks for an existing order with that key before inserting. On a duplicate, it returns `409 Conflict` instead of creating a second order. The key is stored as a unique constraint in PostgreSQL.

**Payment processing:** Before processing an `order.created` event, the payment service calls Redis `SETNX` (set if not exists) with a key scoped to the `orderId`. If the key already exists, the event is a replay (crash recovery, rebalance) and is skipped. The TTL prevents unbounded Redis growth.

### Dead Letter Topic

The payment service uses Spring Kafka's `DefaultErrorHandler` with `DeadLetterPublishingRecoverer`. If processing an `order.created` event throws after 3 retries with exponential backoff, the message is written to `payment.failed.DLT` instead of being discarded or blocking the partition. Messages in the DLT can be inspected in Kafka UI, replayed manually, or consumed by a separate alerting process. Without a DLT, a poison-pill message would block all subsequent messages on that partition indefinitely.

### Retry with exponential backoff

Transient failures (database timeouts, Redis blips) should be retried, but retrying immediately in a tight loop amplifies load during an outage. The payment service uses `ExponentialBackOff` starting at 1 second, doubling on each attempt, for up to 3 retries. Deserialization errors are explicitly excluded from retries since they will never succeed.

### Order status state machine

```
PENDING → CONFIRMED → SHIPPED → DELIVERED
PENDING → CANCELLED
```

Valid transitions are enforced inside `Order.transitionTo()`, not in application logic or HTTP handlers. This means no code path — REST endpoint, Kafka consumer, or future background job — can put an order into an invalid state. Attempting an invalid transition throws `IllegalStateException`, which the global exception handler maps to `422 Unprocessable Entity`.

### Optimistic locking

The `Order` entity has a `@Version` column. If two concurrent writes (e.g., two payment events arriving at the same time for the same order) both read the same version, the second write will fail with `ObjectOptimisticLockingFailureException`. This is surfaced as `409 Conflict`. The alternative — pessimistic locking — holds a database row lock for the duration of the transaction, reducing throughput under concurrent load.

### Correlation IDs for distributed tracing

Each incoming HTTP request is assigned a `correlationId` (from the `X-Correlation-Id` header, or generated if absent) by `CorrelationIdFilter` in each service. The ID is placed in the SLF4J MDC so every log line for that request includes it automatically. It is stored on the `Order` and `Payment` entities and propagated in all Kafka events, so a single request can be traced across all four services by grepping logs for one ID — without requiring a full distributed tracing stack like Jaeger or Zipkin.

---

## Project Structure

```
order-processing-system/
├── pom.xml                          Parent POM
├── docker-compose.yml
├── init-db.sql                      Creates orders_db, payments_db, notifications_db
├── api-gateway/
│   └── src/main/java/.../gateway/
│       └── filter/JwtAuthFilter     Global JWT validation, correlation ID injection
├── order-service/
│   └── src/main/java/.../order/
│       ├── domain/Order             @Version optimistic locking, transitionTo() state machine
│       ├── domain/OrderStatus       canTransitionTo() encodes all valid transitions
│       ├── service/OrderService     Idempotency check, @Cacheable, @CacheEvict
│       ├── kafka/OrderEventPublisher
│       └── kafka/PaymentEventConsumer
├── payment-service/
│   └── src/main/java/.../payment/
│       ├── service/PaymentService   Redis SETNX idempotency, 80/20 simulation
│       ├── config/KafkaConfig       ExponentialBackOff + DeadLetterPublishingRecoverer
│       └── kafka/OrderEventConsumer
└── notification-service/
    └── src/main/java/.../notification/
        ├── service/NotificationService  Structured log output + PostgreSQL history
        └── kafka/NotificationEventConsumer
```

---

## Known Limitations

- **No outbox pattern:** `OrderService.createOrder()` writes to PostgreSQL and then publishes to Kafka in the same method but not the same transaction. If the process crashes between the two, the order exists in the database but the Kafka event is never sent. A production system would use the transactional outbox pattern (write event to a DB table inside the same transaction, poll and publish separately).
- **Simulated payment:** The payment service uses `Random` to simulate 80% success. Replace with a real payment provider adapter.
- **In-memory JWT validation only:** The gateway validates tokens but does not issue them. A real deployment would integrate with an identity provider (Keycloak, Auth0, etc.).
