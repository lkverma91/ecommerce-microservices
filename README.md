# E-commerce Microservices

Spring Boot microservices with Eureka, API Gateway, Kafka, PostgreSQL, OpenFeign, and JWT-ready structure.

## Services

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 8761 | Service discovery |
| Config Server | 8888 | Centralized configuration |
| API Gateway | 8080 | Route `/api/*` to services |
| User Service | 9002 | User management |
| Product Service | 9003 | Product catalog |
| Order Service | 9004 | Order creation, Feign + Kafka |
| Inventory Service | 9005 | Stock, Kafka consumer |
| Payment Service | 9006 | Payments, Kafka consumer |
| Frontend | 3000 / 5173 | React (Vite) - dev: 5173, Docker: 3000 |

## Quick Start

See **[RUNNING_GUIDE.md](RUNNING_GUIDE.md)** for step-by-step instructions from scratch.

## Run with Docker Compose

```bash
# Start all services (includes Redis, Zipkin)
docker-compose up -d

# With custom env (e.g. JWT_SECRET)
JWT_SECRET=your-secret docker-compose up -d
```

Start infra only (DBs + Kafka):
```bash
docker-compose up -d postgres-user postgres-product postgres-order postgres-inventory postgres-payment zookeeper kafka
```

## Run Locally (Maven)

1. Start PostgreSQL (one DB per service) and Kafka.
2. Start Eureka: `cd eureka-server && mvn spring-boot:run -Dspring-boot.run.profiles=local`
3. Start services in order: config-server, user-service, product-service, inventory-service, payment-service, order-service, api-gateway.

## Sample APIs

All requests go through **API Gateway** at `http://localhost:8080`.

### User Service
```http
POST /api/users
Content-Type: application/json

{
  "email": "john@example.com",
  "name": "John Doe",
  "phone": "+1234567890",
  "password": "secret123"
}

Response 201:
{
  "id": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "phone": "+1234567890",
  "active": true,
  "createdAt": "2025-02-22T10:00:00"
}

GET /api/users/1
GET /api/users
GET /api/users/email/john@example.com
PUT /api/users/1
DELETE /api/users/1
```

### Product Service
```http
POST /api/products
Content-Type: application/json

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

## Production

See [DEPLOYMENT.md](DEPLOYMENT.md) for:
- AWS EC2 deployment
- Environment variable configuration
- Scaling and security
