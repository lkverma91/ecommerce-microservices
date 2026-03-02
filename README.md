# E-commerce Microservices

Spring Boot microservices with Eureka, API Gateway, Kafka, PostgreSQL, OpenFeign, and JWT-ready structure.

## Services

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 8761 | Service discovery |
| Config Server | 8888 | Centralized configuration |
| API Gateway | 8080 | Route `/api/*` to services, JWT validation |
| **Auth Service** | 9001 (local) | Login, register, OAuth2 (Google/GitHub/Facebook/Twitter), JWT issuance |
| User Service | 9002 | User management, BCrypt, OAuth linking |
| Product Service | 9003 | Product catalog (admin write protected by role) |
| Order Service | 9004 | Order creation, Feign + Kafka |
| Inventory Service | 9005 | Stock, Kafka consumer |
| Payment Service | 9006 | Payments, Kafka consumer |
| Frontend | 3000 / 5173 | React (Vite) - dev: 5173, Docker: 3000 |

## Quick Start

See **[RUNNING_GUIDE.md](RUNNING_GUIDE.md)** for step-by-step instructions from scratch.

## Run with Docker Compose

```bash
# From project root - start all services (Postgres, Redis, Kafka, Zipkin, Eureka, Config, Auth, User, Product, Order, Inventory, Payment, API Gateway, Frontend)
docker compose up -d --build

# Optional: use a .env file for secrets (copy from .env.example)
cp .env.example .env
# Edit .env: set JWT_SECRET (min 32 chars for JWT), and OAuth client ids if using social login
docker compose up -d --build
```

Start infrastructure only (single Postgres with all DBs, Redis, Zookeeper, Kafka):
```bash
docker compose up -d postgres redis zookeeper kafka
```

## Run Locally (Maven)

1. Start PostgreSQL (one DB per service) and Kafka.
2. Start Eureka: `cd eureka-server && mvn spring-boot:run -Dspring-boot.run.profiles=local`
3. Start services in order: config-server, user-service, product-service, inventory-service, payment-service, order-service, api-gateway.

## Sample APIs

All requests go through **API Gateway** at `http://localhost:8080`.

### Authentication (Auth Service)
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "john@example.com",
  "name": "John Doe",
  "phone": "+1234567890",
  "password": "secret123"
}

Response 201: { "token": "<JWT>", "user": { "id", "email", "name", "roles", ... } }

POST /api/auth/login
Content-Type: application/json

{ "email": "john@example.com", "password": "secret123" }

Response 200: { "token": "<JWT>", "user": { ... } }

GET /api/auth/me
Authorization: Bearer <JWT>
Response 200: { "id", "email", "name", "roles", ... }
```

**Social login:** Redirect the browser to `GET /api/auth/oauth2/authorization/{google|github|facebook|twitter}`. After provider login, backend redirects to frontend `/auth/callback?token=<JWT>`.

**Protected routes:** Send `Authorization: Bearer <token>` for `/api/users/**`, `/api/orders/**`, `/api/products/**` (write), etc. Gateway validates JWT and forwards `X-User-Id` and `X-User-Roles`.

### User Service (requires JWT for most endpoints)
```http
POST /api/auth/register   # use this to create account (returns token)
GET /api/users/1          # requires Authorization: Bearer <token>
GET /api/users
GET /api/users/email/john@example.com
PUT /api/users/1
DELETE /api/users/1
```

### Product Service
```http
POST /api/products   # requires JWT with role ADMIN
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "Laptop",
  "description": "High performance laptop",
  "price": 999.99,
  "category": "Electronics"
}

Response 201:
{
  "id": 1,
  "name": "Laptop",
  "description": "High performance laptop",
  "price": 999.99,
  "category": "Electronics",
  "active": true,
  "createdAt": "2025-02-22T10:00:00"
}

GET /api/products
GET /api/products?category=Electronics
GET /api/products/1
PUT /api/products/1
DELETE /api/products/1
```

### Inventory Service
```http
POST /api/inventory
Content-Type: application/json

{
  "productId": 1,
  "quantity": 100
}

Response 201:
{
  "id": 1,
  "productId": 1,
  "quantity": 100,
  "reserved": 0,
  "available": 100
}

GET /api/inventory
GET /api/inventory/product/1
GET /api/inventory/check?productId=1&quantity=5
```

### Order Service (OpenFeign + Kafka)
```http
POST /api/orders
Content-Type: application/json

{
  "userId": 1,
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}

Response 201:
{
  "id": 1,
  "userId": 1,
  "status": "PENDING",
  "totalAmount": 1999.98,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "unitPrice": 999.99,
      "subtotal": 1999.98
    }
  ],
  "createdAt": "2025-02-22T10:00:00"
}

GET /api/orders/1
GET /api/orders/user/1
```

### Payment Service
```http
GET /api/payments/1
GET /api/payments/order/1
GET /api/payments/user/1
```

Payments are created asynchronously when an order is placed (Kafka consumer).

## Flow

1. **Create order**: Client → API Gateway → Order Service.
2. Order Service uses **OpenFeign** to call User, Product, Inventory.
3. Order Service publishes `OrderPlacedEvent` to **Kafka** topic `order-placed`.
4. **Inventory Service** consumes event → reserves stock.
5. **Payment Service** consumes event → creates payment record.

## Tech Stack

- Java 17, Spring Boot 3.2
- PostgreSQL, JPA/Hibernate
- Eureka, Spring Cloud Gateway
- **Auth:** JWT (jjwt), BCrypt, Spring Security OAuth2 Client (Google, GitHub, Facebook, Twitter)
- OpenFeign, Kafka
- Docker, Docker Compose
- Zipkin (tracing), Redis (rate limiting)

## Frontend

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

See [frontend/README.md](frontend/README.md) for details.

## Integration & Testing

- **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)** — Request flow, JWT, Kafka, Postman, testing, monitoring, scaling, CI/CD
- **Postman** — Import `postman/E-commerce-API.postman_collection.json`

## How to run (summary)

| Goal | Command |
|------|---------|
| **Full stack (Docker)** | `docker compose up -d --build` → open http://localhost:3000 |
| **Optional .env** | `cp .env.example .env` then edit (set `JWT_SECRET` for production; add OAuth client ids for social login) |
| **Frontend only (dev)** | `cd frontend && npm install && npm run dev` → http://localhost:5173 (start gateway + backend separately for API) |
| **Backend only (Maven)** | Start Eureka, then config-server, auth-service, user-service, product-service, order-service, inventory-service, payment-service, api-gateway (see [RUNNING_GUIDE.md](RUNNING_GUIDE.md)) |

After `docker compose up`, use the app at **http://localhost:3000**: register or log in (email/password or social if OAuth is configured). Product create/update/delete require a user with role `ADMIN` (assign in DB if needed).

## Production

See [DEPLOYMENT.md](DEPLOYMENT.md) for:
- AWS EC2 deployment
- Environment variable configuration (including JWT and OAuth)
- Scaling and security
