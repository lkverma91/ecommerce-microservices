# E-commerce Microservices — Architecture Planning Document

> **Tech Stack:** Spring Boot (Java 17) | React (Vite) | PostgreSQL | Eureka | Spring Cloud Gateway | OpenFeign | Kafka | JWT | Docker | Config Server | Centralized Logging | Zipkin

---

## 1. Complete Microservices Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL CLIENTS                                     │
│                    (Web Browser / Mobile App / Third-party)                        │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (Spring Cloud Gateway)                         │
│         Routing | Rate Limiting | JWT Validation | CORS | Load Balancing          │
└─────────────────────────────────────────────────────────────────────────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    ▼                   ▼                   ▼
┌───────────────────────┐   ┌───────────────────┐   ┌───────────────────┐
│   SERVICE DISCOVERY   │   │   CONFIG SERVER   │   │   AUTH SERVICE    │
│   (Eureka Server)     │   │   (Spring Config) │   │   (JWT)           │
└───────────────────────┘   └───────────────────┘   └───────────────────┘
                    │                   │                   │
        ┌───────────┼───────────────────┼───────────────────┼───────────┐
        ▼           ▼                   ▼                   ▼           ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
   │  User   │ │ Product │ │ Order   │ │Inventory│ │ Payment │ │Notification│
   │ Service │ │ Service │ │ Service │ │ Service │ │ Service │ │ Service  │
   └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘
        │           │           │           │           │           │
        ▼           ▼           ▼           ▼           ▼           ▼
   [PostgreSQL] [PostgreSQL] [PostgreSQL] [PostgreSQL] [PostgreSQL] [PostgreSQL]
   
   ┌─────────────────────────────────────────────────────────────────────────┐
   │   KAFKA (Event Bus)          │   ZIPKIN + LOGGING (ELK/Loki)             │
   └─────────────────────────────────────────────────────────────────────────┘
```

### Core Principles

- **Domain-Driven Design (DDD):** Each service owns a bounded context.
- **Database per Service:** Each microservice has its own PostgreSQL database.
- **API-First:** REST APIs with consistent contracts; OpenAPI/Swagger for documentation.
- **Event-Driven:** Kafka for async communication (orders, payments, notifications).

---

## 2. Required Microservices (Complete List)

| # | Service | Responsibility | Port (Dev) | Key Dependencies |
|---|---------|----------------|------------|------------------|
| 1 | **API Gateway** | Route requests, validate JWT, rate limit, CORS | 8080 | Eureka, Config |
| 2 | **Config Server** | Centralized configuration management | 8888 | Git/Native |
| 3 | **Eureka Server** | Service discovery and registration | 8761 | Config |
| 4 | **Auth Service** | User registration, login, JWT issuance/validation | 9001 | PostgreSQL, Redis (optional) |
| 5 | **User Service** | User profile, addresses, preferences | 9002 | PostgreSQL |
| 6 | **Product Service** | Product catalog, categories, search | 9003 | PostgreSQL |
| 7 | **Order Service** | Order creation, status, history | 9004 | PostgreSQL, Kafka |
| 8 | **Inventory Service** | Stock management, reservations | 9005 | PostgreSQL, Kafka |
| 9 | **Payment Service** | Payments, refunds, payment history | 9006 | PostgreSQL, Kafka |
| 10 | **Notification Service** | Email, SMS, in-app notifications | 9007 | Kafka |
| 11 | **Frontend (React)** | Web UI (Vite) | 5173 | API Gateway |

### Supporting Infrastructure

| Component | Purpose | Port |
|-----------|---------|------|
| PostgreSQL (per service) | Persistence | 5432+ |
| Kafka | Message broker | 9092 |
| Zipkin | Distributed tracing | 9411 |
| ELK Stack / Loki | Centralized logging | Various |
| Redis (optional) | Session/token cache | 6379 |

---

## 3. System Design Diagram Explanation

### 3.1 Request Flow (Synchronous)

1. **Client** → Sends HTTP request to API Gateway (e.g., `GET /api/products`).
2. **API Gateway** → Validates JWT (if protected route), routes to Product Service via Eureka.
3. **Product Service** → Queries its DB, returns response.
4. **Response** flows back through Gateway to Client.

### 3.2 Event Flow (Asynchronous via Kafka)

1. **Order Service** → Creates order, publishes `OrderPlacedEvent` to Kafka topic.
2. **Inventory Service** → Consumes event, reserves stock.
3. **Payment Service** → Consumes event, processes payment.
4. **Notification Service** → Consumes event, sends confirmation email/SMS.

### 3.3 Service Interaction Matrix

| Consumer | Provider | Communication | Use Case |
|----------|----------|---------------|----------|
| API Gateway | All Services | HTTP (via Eureka) | Route requests |
| Order Service | User, Product, Inventory | OpenFeign | Validate user, product, stock |
| Order Service | Inventory, Payment, Notification | Kafka | Publish events |
| Payment Service | Order Service | OpenFeign / Kafka | Update order status |
| Frontend | API Gateway | HTTP/REST | All UI operations |

---

## 4. Folder Structure for Each Service

### 4.1 Monorepo Root Structure

```
ecommerce-microservices/
├── config-repo/                    # Git repo for Config Server (or embedded config)
├── api-gateway/
├── config-server/
├── eureka-server/
├── services/
│   ├── auth-service/
│   ├── user-service/
│   ├── product-service/
│   ├── order-service/
│   ├── inventory-service/
│   ├── payment-service/
│   └── notification-service/
├── frontend/                       # React (Vite)
├── docker-compose.yml
├── docker-compose.prod.yml
└── README.md
```

### 4.2 Spring Boot Service Structure (e.g., `order-service`)

```
order-service/
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/orderservice/
│   │   │   ├── OrderServiceApplication.java
│   │   │   ├── config/              # Feign, Security, Kafka config
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── client/              # OpenFeign clients
│   │   │   ├── event/               # Kafka producer/consumer
│   │   │   └── exception/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── bootstrap.yml        # Config Server bootstrap
│   └── test/
├── Dockerfile
└── pom.xml
```

### 4.3 Frontend Structure (React + Vite)

```
frontend/
├── public/
├── src/
│   ├── api/           # API client (Axios/fetch)
│   ├── components/
│   ├── hooks/
│   ├── pages/
│   ├── store/         # State (e.g., Zustand/Redux)
│   ├── utils/
│   ├── App.tsx
│   └── main.tsx
├── index.html
├── vite.config.ts
├── package.json
└── Dockerfile
```

---

## 5. Local vs Production Configuration

### 5.1 Config Server Strategy

- **Config Server** fetches config from a **config-repo** (Git) or **native profile**.
- Each service uses `bootstrap.yml` to connect to Config Server on startup.
- Profiles: `local`, `dev`, `staging`, `prod`.

### 5.2 Local Development

| Aspect | Approach |
|--------|----------|
| **Config source** | `config-repo` with `application-{service}-local.yml` |
| **Eureka** | All services register with local Eureka (`localhost:8761`) |
| **Databases** | Docker Compose spins up separate PostgreSQL containers per service |
| **Kafka** | Single Kafka + Zookeeper in Docker |
| **Secrets** | Plain text in local YAML (never commit real secrets) |
| **URLs** | `localhost` with respective ports |

**Example local config file:** `config-repo/order-service-local.yml`

```yaml
# Conceptual only - no code implementation
server:
  port: 9004
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/order_db
eureka:
  instance:
    hostname: localhost
```

### 5.3 Production

| Aspect | Approach |
|--------|----------|
| **Config source** | Config Server points to private Git repo or Vault |
| **Secrets** | External secret manager (Vault, AWS Secrets, K8s Secrets) |
| **Eureka** | Services register with internal hostnames (e.g., `order-service`) |
| **Databases** | Managed PostgreSQL (RDS, Cloud SQL) or dedicated DB per service |
| **Kafka** | Managed Kafka (Confluent, MSK) or Kubernetes Kafka operator |
| **URLs** | Internal DNS / service names |

### 5.4 Profile Activation

- **Local:** `spring.profiles.active=local` (or `SPRING_PROFILES_ACTIVE=local`)
- **Production:** Set via environment variable in Docker/K8s.

---

## 6. Database per Service Concept

### 6.1 What It Means

- **One database per microservice** — no shared database.
- Each service owns its schema and tables.
- No direct DB access between services (only via APIs or events).

### 6.2 Benefits

- **Loose coupling:** Services can change schema independently.
- **Technology flexibility:** Could use different DBs (e.g., MongoDB for catalog search).
- **Failure isolation:** DB outage in one service doesn’t crash others.
- **Scalability:** Scale databases per service needs.

### 6.3 Service → Database Mapping

| Service | Database | Key Entities (Conceptual) |
|---------|----------|---------------------------|
| Auth | `auth_db` | Users, refresh_tokens |
| User | `user_db` | Profiles, addresses, preferences |
| Product | `product_db` | Products, categories, images |
| Order | `order_db` | Orders, order_items |
| Inventory | `inventory_db` | Stock, reservations |
| Payment | `payment_db` | Payments, transactions |
| Notification | `notification_db` | Notification logs, templates |

### 6.4 Data Consistency Across Services

- **Saga pattern:** For distributed transactions (e.g., Order → Inventory → Payment).
- **Eventual consistency:** Use Kafka events to sync state.
- **Compensation:** If Payment fails, Inventory releases reservation via compensating event.

---

## 7. Communication Flow (Step-by-Step)

### 7.1 User Places an Order (End-to-End)

```
Step 1:  User (Frontend) → POST /api/orders
         → API Gateway (validates JWT)

Step 2:  API Gateway → Order Service (via Eureka)

Step 3:  Order Service:
         - OpenFeign → User Service: Validate user exists
         - OpenFeign → Product Service: Validate products, get prices
         - OpenFeign → Inventory Service: Check and reserve stock

Step 4:  Order Service:
         - Saves order in order_db
         - Publishes OrderPlacedEvent to Kafka topic "order-placed"

Step 5:  Kafka Consumers (parallel):
         - Inventory Service: Confirm reservation, update stock
         - Payment Service: Process payment
         - Notification Service: Send order confirmation email

Step 6:  Payment Service:
         - On success: Publishes PaymentCompletedEvent
         - Order Service consumes: Updates order status to CONFIRMED

Step 7:  On Payment failure:
         - Payment Service publishes PaymentFailedEvent
         - Inventory Service: Releases reservation (compensating action)
         - Order Service: Updates order status to CANCELLED
```

### 7.2 User Logs In

```
Step 1:  Frontend → POST /api/auth/login → API Gateway → Auth Service

Step 2:  Auth Service validates credentials, issues JWT

Step 3:  Frontend stores JWT, sends in Authorization header for subsequent requests
```

### 7.3 User Browses Products

```
Step 1:  Frontend → GET /api/products → API Gateway → Product Service

Step 2:  Product Service queries product_db, returns list

Step 3:  (Optional) Product Service could call Inventory Service via Feign for stock info
```

---

## 8. Best Industry Practices

### 8.1 Security

- **JWT:** Short-lived access tokens, refresh tokens for renewal.
- **API Gateway:** Validate JWT on protected routes; propagate user context.
- **Secrets:** Never commit; use env vars or secret managers in prod.
- **HTTPS:** Enforce in production.

### 8.2 Resilience

- **Circuit Breaker:** Use Resilience4j with OpenFeign.
- **Retry:** Retry failed Feign calls with exponential backoff.
- **Bulkhead:** Limit concurrent calls to prevent cascading failures.

### 8.3 Observability

- **Logging:** Structured logs (JSON); send to centralized store (ELK/Loki).
- **Tracing:** Zipkin with Spring Cloud Sleuth; trace ID propagated across services.
- **Metrics:** Prometheus + Grafana; expose actuator endpoints.

### 8.4 API Design

- **REST:** Resource-based URLs, proper HTTP verbs and status codes.
- **Versioning:** URL path (`/api/v1/orders`) or header.
- **Pagination:** For list endpoints.
- **OpenAPI:** Swagger/OpenAPI docs for all services.

### 8.5 Deployment

- **Containerization:** Docker for each service.
- **Orchestration:** Docker Compose for local; Kubernetes for production.
- **Health checks:** Actuator `/actuator/health` for liveness/readiness.

### 8.6 Development

- **Contract testing:** Pact or Spring Cloud Contract for Feign interfaces.
- **Integration tests:** TestContainers for PostgreSQL, Kafka.
- **CI/CD:** Build, test, and deploy each service independently.

---

## Summary

| Topic | Summary |
|-------|---------|
| **Architecture** | API Gateway + Eureka + Config + 7 domain services + Kafka |
| **Services** | Auth, User, Product, Order, Inventory, Payment, Notification |
| **DB Strategy** | One PostgreSQL database per service |
| **Sync** | OpenFeign for request/response between services |
| **Async** | Kafka for events (order, payment, notifications) |
| **Config** | Config Server with profile-based YAML (local vs prod) |
| **Tracing** | Zipkin with Sleuth for distributed tracing |

---

*Next step: Implement each service following this architecture.*
