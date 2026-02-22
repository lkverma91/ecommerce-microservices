# E-commerce Microservices — Integration Guide

A beginner-friendly guide to how all parts work together.

---

## 1. Complete Request Flow (User → Gateway → Services → DB)

### Overview

```
┌─────────┐     ┌──────────────┐     ┌─────────────────┐     ┌────────────┐
│  User   │────▶│ API Gateway  │────▶│ Microservices   │────▶│ PostgreSQL │
│(Browser)│     │  (Port 8080) │     │ (via Eureka)    │     │ (per svc)  │
└─────────┘     └──────────────┘     └─────────────────┘     └────────────┘
                      │
                      ▼
               ┌──────────────┐
               │   Eureka     │  ◀── Services register here
               │  (Port 8761) │
               └──────────────┘
```

### Example: User browses products

| Step | Where | What happens |
|------|-------|--------------|
| 1 | **Browser** | User visits `http://localhost:3000/products` |
| 2 | **Frontend** | React fetches `GET /api/products` (relative URL) |
| 3 | **Nginx / Proxy** | Request goes to `http://localhost:8080/api/products` (or via Vite proxy in dev) |
| 4 | **API Gateway** | Receives request, matches route `Path=/api/products/**` |
| 5 | **API Gateway** | Asks Eureka: “Where is `product-service`?” |
| 6 | **Eureka** | Returns instance: `product-service:9003` |
| 7 | **API Gateway** | Strips `/api`, forwards `GET /products` to product-service |
| 8 | **Product Service** | Controller → Service → Repository |
| 9 | **Product Service** | JPA queries PostgreSQL `product_db` |
| 10 | **Response** | DB → Service → Controller → Gateway → Frontend → User |

### Example: User places an order

| Step | Component | Action |
|------|-----------|--------|
| 1 | Frontend | `POST /api/orders` with `{ userId, items }` |
| 2 | API Gateway | Routes to **order-service** |
| 3 | Order Service | Calls **user-service** (Feign): “Does user 1 exist?” |
| 4 | Order Service | Calls **product-service** (Feign): “Get product prices” |
| 5 | Order Service | Calls **inventory-service** (Feign): “Is stock available?” |
| 6 | Order Service | Saves order in **order_db** (PostgreSQL) |
| 7 | Order Service | Publishes `OrderPlacedEvent` to **Kafka** topic `order-placed` |
| 8 | Inventory Service | Consumes event → reserves stock |
| 9 | Payment Service | Consumes event → creates payment record |

---

## 2. JWT Flow (Frontend ↔ Backend)

### Current vs. Target Setup

Today the backend has no dedicated Auth API. The frontend expects:

- **Register**: `POST /api/users` (works now)
- **Login**: `POST /api/auth/login` (backend not implemented yet)
- In dev, the frontend falls back to `GET /api/users/email/{email}` to “login” without password check.

Below is how JWT will flow once the backend adds a proper auth service.

### JWT Flow Diagram

```
┌──────────┐                    ┌──────────────┐                    ┌─────────────┐
│ Frontend │                    │ API Gateway  │                    │ Auth/User   │
│          │                    │              │                    │ Service     │
└────┬─────┘                    └──────┬───────┘                    └──────┬──────┘
     │                                 │                                  │
     │  POST /api/auth/login           │                                  │
     │  { email, password }            │                                  │
     │────────────────────────────────▶│                                  │
     │                                 │  Forward to Auth Service         │
     │                                 │─────────────────────────────────▶│
     │                                 │                                  │
     │                                 │  Validate credentials,           │
     │                                 │  generate JWT                    │
     │                                 │◀─────────────────────────────────│
     │                                 │  { token, user }                 │
     │  { token, user }                │                                  │
     │◀────────────────────────────────│                                  │
     │                                 │                                  │
     │  Store: localStorage            │                                  │
     │  - token                        │                                  │
     │  - user                         │                                  │
     │                                 │                                  │
     │  GET /api/orders                │                                  │
     │  Header: Authorization: Bearer <JWT>                              │
     │────────────────────────────────▶│                                  │
     │                                 │  Validate JWT                    │
     │                                 │  (or forward to Auth)            │
     │                                 │  → Route to Order Service        │
     │                                 │                                  │
```

### Step-by-step JWT flow

| Step | Who | What |
|------|-----|------|
| 1 | User | Enters email/password on Login page |
| 2 | Frontend | `POST /api/auth/login` with `{ email, password }` |
| 3 | API Gateway | Forwards to Auth Service (when it exists) |
| 4 | Auth Service | Validates credentials, signs JWT with `JWT_SECRET` |
| 5 | Auth Service | Returns `{ token: "eyJhbG...", user: { id, email, name } }` |
| 6 | Frontend | Saves `token` and `user` in `localStorage` |
| 7 | Frontend | Axios interceptor adds `Authorization: Bearer <token>` to all requests |
| 8 | API Gateway | (When implemented) Validates JWT before routing to services |
| 9 | Backend | Uses claims (e.g. `userId`) from token instead of trusting request body |

### Frontend code (already in place)

```javascript
// axiosInstance.ts - Request interceptor
axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// On 401 - clear token and redirect to login
axiosInstance.interceptors.response.use(..., (error) => {
  if (error.response?.status === 401) {
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
});
```

### Backend (to add)

- Auth Service that issues JWT on successful login
- Gateway filter or dedicated Auth service to validate JWT and extract claims
- `JWT_SECRET` passed via env (never hardcoded)

---

## 3. Kafka Event Flow for Order Processing

### Topic and consumers

```
                    ┌─────────────────────┐
                    │  order-placed       │
                    │  (Kafka topic)      │
                    └──────────┬──────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Inventory       │  │ Payment         │  │ Notification    │
│ Service         │  │ Service         │  │ Service (future)│
│                 │  │                 │  │                 │
│ Reserve stock   │  │ Create payment  │  │ Send email/SMS  │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Event payload

```json
{
  "orderId": 1,
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2, "price": 99.99 }
  ],
  "totalAmount": 199.98
}
```

### Step-by-step event flow

| Step | Service | Action |
|------|---------|--------|
| 1 | Order Service | Saves order in DB, status = PENDING |
| 2 | Order Service | Serializes `OrderPlacedEvent` to JSON |
| 3 | Order Service | Publishes to Kafka topic `order-placed` |
| 4 | Kafka | Stores event, delivers to consumer groups |
| 5 | Inventory Service | Consumes event, reserves stock per item |
| 6 | Payment Service | Consumes event, creates payment (status COMPLETED) |
| 7 | (Future) Notification Service | Consumes event, sends order confirmation |

### Why Kafka?

- Decouples Order Service from Inventory, Payment, Notification
- Each consumer can scale independently
- If a consumer is down, Kafka buffers messages until it recovers
- Multiple consumers can react to the same event

---

## 4. Postman Collection Structure

### Suggested folders

```
E-commerce API
├── 1. Auth (future)
│   ├── Register
│   ├── Login
│   └── Refresh Token
├── 2. Users
│   ├── Create User (POST)
│   ├── Get User by ID (GET)
│   ├── Get User by Email (GET)
│   ├── Get All Users (GET)
│   ├── Update User (PUT)
│   └── Delete User (DELETE)
├── 3. Products
│   ├── List Products (GET)
│   ├── List by Category (GET)
│   ├── Get Product (GET)
│   ├── Create Product (POST) [Admin]
│   ├── Update Product (PUT) [Admin]
│   └── Delete Product (DELETE) [Admin]
├── 4. Inventory
│   ├── Add/Update Stock (POST)
│   ├── Get by Product (GET)
│   └── Check Stock (GET)
├── 5. Orders
│   ├── Create Order (POST)
│   ├── Get Order (GET)
│   └── Get Orders by User (GET)
└── 6. Payments
    ├── Get Payment (GET)
    ├── Get by Order (GET)
    └── Get by User (GET)
```

### Variables

| Variable | Example |
|----------|---------|
| `baseUrl` | `http://localhost:8080/api` |
| `token` | (set from Login response) |

### Example: Create Order

```
POST {{baseUrl}}/orders
Content-Type: application/json

{
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

### Pre-request script (JWT)

```javascript
// In collection Pre-request Script
if (pm.collectionVariables.get("token")) {
  pm.request.headers.add({
    key: "Authorization",
    value: "Bearer " + pm.collectionVariables.get("token")
  });
}
```

---

## 5. Testing Strategy

### Unit tests (per service)

| Layer | What to test |
|-------|--------------|
| Service | Business logic, mocks for Repository/Feign |
| Controller | Request/response mapping, validation |
| Repository | JPA queries (with `@DataJpaTest`) |

### Example: User Service unit test

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    @Test
    void createUser_duplicateEmail_throws() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () ->
            userService.createUser(UserRequest.builder().email("a@b.com").build()));
    }
}
```

### Integration tests

| Scope | Tools | Purpose |
|-------|-------|---------|
| API + DB | `@SpringBootTest`, Testcontainers | Test full HTTP → DB flow |
| Feign clients | WireMock or real service | Test service-to-service calls |
| Kafka | Embedded Kafka / Testcontainers | Test event publishing and consuming |

### Example: Order Service integration

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class OrderServiceIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    @Container static KafkaContainer kafka = new KafkaContainer();

    @Test
    void createOrder_publishesToKafka() {
        // 1. Seed user, product, inventory
        // 2. POST /orders
        // 3. Assert order saved
        // 4. Assert message in Kafka topic
    }
}
```

### Frontend tests

| Type | Tool | What |
|------|------|------|
| Unit | Vitest | Utils, hooks, simple components |
| Component | React Testing Library | Components with user actions |
| E2E | Playwright / Cypress | Full flows (login → add to cart → checkout) |

### Suggested test pyramid

```
         ┌───────┐
         │  E2E  │   Few, critical paths
         ├───────┤
         │ Integ │   API + DB, Feign, Kafka
         ├───────┤
         │ Unit  │   Many, fast
         └───────┘
```

---

## 6. Monitoring

### Components

| Component | Port | Purpose |
|-----------|------|---------|
| **Zipkin** | 9411 | Distributed tracing across services |
| **Actuator** | (per service) | Health, metrics, info |
| **Eureka** | 8761 | Service registry and status |

### Trace flow (Zipkin)

```
Request: POST /api/orders
   │
   ├── api-gateway (trace-id: abc123)
   │      │
   │      └── order-service (same trace-id)
   │             ├── user-service (Feign)
   │             ├── product-service (Feign)
   │             ├── inventory-service (Feign)
   │             └── kafka producer
   │
   └── Zipkin UI: see full path and latency
```

### Health checks

- All services: `GET /actuator/health`
- Docker: `HEALTHCHECK` uses these endpoints
- Kubernetes: liveness/readiness probes

### Metrics (Actuator + Prometheus)

- HTTP request counts and latencies
- JVM memory, threads
- DB pool usage
- Kafka consumer lag (when exposed)

### Logging

- Logs include `traceId` and `spanId`
- Central log store (e.g. ELK, Loki) can search by trace ID

---

## 7. Scaling Strategy

### Horizontal scaling

```
                    ┌─────────────────┐
                    │   Load Balancer │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ API Gateway 1 │  │ API Gateway 2 │  │ API Gateway 3 │
└───────────────┘  └───────────────┘  └───────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                    ┌────────┴────────┐
                    │     Eureka      │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        ▼                    ▼                    ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│ Order Svc 1   │  │ Order Svc 2   │  │ Order Svc 3   │
└───────────────┘  └───────────────┘  └───────────────┘
```

### What to scale and when

| Service | Scale when | Suggested replicas |
|---------|------------|--------------------|
| API Gateway | High external traffic | 2–4 |
| Order Service | High order volume | 2–4 |
| Product Service | High catalog traffic | 2–4 |
| User Service | High auth/profile traffic | 2 |
| Inventory Service | High stock checks | 2 |
| Payment Service | High payment volume | 2 |

### Docker Compose scaling

```bash
docker compose up -d --scale order-service=3 --scale product-service=2
```

### Stateless design

- No session stored in service memory
- JWT carries user identity
- Cart can be in Redis or DB for multi-instance consistency

### Database scaling

- Read replicas for Product, User (read-heavy)
- Sharding by tenant or domain when needed

---

## 8. CI/CD Pipeline Structure

### Stages

```
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│  Build   │──▶│   Test   │──▶│   Build  │──▶│   Push   │──▶│  Deploy  │
│          │   │          │   │  Image   │   │   to     │   │          │
│  mvn pkg │   │ Unit+Int │   │  Docker  │   │ Registry │   │ K8s/EC2  │
└──────────┘   └──────────┘   └──────────┘   └──────────┘   └──────────┘
```

### Example: GitHub Actions

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build-test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [user-service, product-service, order-service, ...]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Build
        run: mvn -f ${{ matrix.service }}/pom.xml package -DskipTests
      - name: Unit Tests
        run: mvn -f ${{ matrix.service }}/pom.xml test
```

### Example: Build and push images

```yaml
# .github/workflows/deploy.yml
- name: Build Docker image
  run: docker build -t myregistry/order-service:${{ github.sha }} ./order-service
- name: Push
  run: docker push myregistry/order-service:${{ github.sha }}
- name: Deploy to K8s
  run: kubectl set image deployment/order-service order-service=myregistry/order-service:${{ github.sha }}
```

### Deployment order

1. Eureka Server  
2. Config Server  
3. Infrastructure (DB, Kafka, Redis)  
4. Domain services (User, Product, Inventory, Payment, Order)  
5. API Gateway  
6. Frontend  

### Blue-green / canary (optional)

- Two environments (blue/green) or gradual traffic shift (canary)
- New version deployed to one set of instances, traffic switched after validation

---

## Quick Reference

| Concern | Where to look |
|---------|----------------|
| Request flow | Section 1 |
| JWT flow | Section 2 |
| Kafka order flow | Section 3 |
| API testing | Section 4 |
| Testing approach | Section 5 |
| Tracing and health | Section 6 |
| Scaling | Section 7 |
| CI/CD | Section 8 |
