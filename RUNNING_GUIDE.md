# E-commerce Microservices — Complete Running Guide

Step-by-step guide to run both backend and frontend from scratch.

---

## Prerequisites

| Tool | Version | Purpose | How to check |
|------|---------|---------|--------------|
| **Java** | 17+ | Backend (Spring Boot) | `java -version` |
| **Maven** | 3.8+ | Build Java projects | `mvn -version` |
| **Node.js** | 18+ | Frontend (React/Vite) | `node -version` |
| **npm** | 9+ | Install frontend deps | `npm -version` |
| **Docker** | 24+ | Containers | `docker -version` |
| **Docker Compose** | v2+ | Run all services | `docker compose version` |
| **Git** | 2.x | Clone repo | `git --version` |

---

## Option A: Run Everything with Docker Compose (Easiest)

Best for: Quick start, local development, testing.

### Step 1: Open the project folder

```bash
cd d:\SpringBootWorkSpace\cursor-ai-demo\ecommerce-microservices
```

**Explanation:** Move into the project root where `docker-compose.yml` is located.

---

### Step 2: Start all services

```bash
docker compose up -d --build
```

**Explanation:**
- `docker compose` — Use Compose to manage multiple containers
- `up` — Create and start containers
- `-d` — Run in background (detached)
- `--build` — Build images from Dockerfiles before starting (needed the first time)

**What starts:**
- 5 PostgreSQL databases (user, product, order, inventory, payment)
- Zookeeper + Kafka
- Redis
- Zipkin
- Eureka Server
- Config Server
- User, Product, Order, Inventory, Payment services
- API Gateway
- Frontend

---

### Step 3: Check status

```bash
docker compose ps
```

**Explanation:** Lists running containers and their ports.

**Expected:** All services in "Up" or "running" state.

---

### Step 4: View logs (if needed)

```bash
# All services
docker compose logs -f

# One service
docker compose logs -f api-gateway

# Stop following (Ctrl+C)
```

**Explanation:**
- `logs` — Show container output
- `-f` — Follow (stream) logs

---

### Step 5: Access the application

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:3000 | Web UI |
| **API Gateway** | http://localhost:8080/api | REST API entry point |
| **Eureka** | http://localhost:8761 | Service registry UI |
| **Zipkin** | http://localhost:9411 | Distributed tracing |

---

### Step 6: Stop everything

```bash
docker compose down
```

**Explanation:** Stops and removes containers. Volumes (data) are kept.

```bash
docker compose down -v
```

**Explanation:** `-v` removes volumes too (resets databases).

---

## Option B: Run Backend Manually (Maven)

Use when you want to run services on your machine without Docker (for debugging, profiling).

### Step 1: Start infrastructure (PostgreSQL, Kafka, Redis)

You still need:
- **PostgreSQL** (5 databases: user_db, product_db, order_db, inventory_db, payment_db)
- **Kafka** (with Zookeeper)
- **Redis**

**Option B1: Run only infra with Docker**

```bash
docker compose up -d postgres-user postgres-product postgres-order postgres-inventory postgres-payment zookeeper kafka redis zipkin
```

**Explanation:** Starts only databases, Kafka, Redis, and Zipkin.

---

### Step 2: Create databases

Each Postgres container creates one DB. Ports:

| DB | Port | Database name |
|----|------|---------------|
| user | 5432 | user_db |
| product | 5433 | product_db |
| order | 5434 | order_db |
| inventory | 5435 | inventory_db |
| payment | 5436 | payment_db |

If you run PostgreSQL locally, create these databases:

```sql
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payment_db;
```

---

### Step 3: Start Eureka Server (first)

```bash
cd eureka-server
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Explanation:**
- `mvn` — Maven
- `spring-boot:run` — Run without packaging JAR
- `-Dspring-boot.run.profiles=local` — Activate `local` profile

**Wait until:** You see "Started EurekaServerApplication" and http://localhost:8761 loads.

**New terminal for each next service.**

---

### Step 4: Start Config Server

```bash
cd config-server
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Port:** 8888

---

### Step 5: Start domain services (order can be last)

Open separate terminals. Order does not matter, but Order Service needs User, Product, and Inventory.

```bash
# Terminal 3 - User Service
cd user-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 4 - Product Service
cd product-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 5 - Inventory Service
cd inventory-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 6 - Payment Service
cd payment-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 7 - Order Service
cd order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Ports:**
- user-service: 9002
- product-service: 9003
- inventory-service: 9005
- payment-service: 9006
- order-service: 9004

---

### Step 6: Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Port:** 8080  
**Requires:** Redis on localhost:6379 (start via Docker if needed).

---

## Option C: Run Frontend Manually

### Step 1: Install dependencies

```bash
cd frontend
npm install
```

**Explanation:**
- `npm` — Node package manager
- `install` — Reads `package.json` and installs dependencies (React, Vite, Tailwind, etc.)

**Output:** `node_modules` folder created.

---

### Step 2: Start dev server

```bash
npm run dev
```

**Explanation:**
- `npm run` — Run a script from `package.json`
- `dev` — Runs `vite`, which serves the app and proxies `/api` to the backend

**Output:**
```
  VITE v5.x.x  ready in xxx ms
  ➜  Local:   http://localhost:5173/
```

Open: http://localhost:5173

---

### Step 3: Production build (optional)

```bash
npm run build
```

**Explanation:** Compiles and minifies for production into `dist/`.

```bash
npm run preview
```

**Explanation:** Serves the `dist/` folder locally (simulates production).

---

## Complete Flow: Docker Compose (Recommended)

| Step | Command | What happens |
|------|---------|--------------|
| 1 | `cd ecommerce-microservices` | Go to project root |
| 2 | `docker compose up -d --build` | Build and start all containers |
| 3 | Wait 2–3 min | Services register with Eureka and become ready |
| 4 | Open http://localhost:3000 | Use the frontend |
| 5 | Register a user | Creates user in user-service |
| 6 | Add products (Admin) | Creates products in product-service |
| 7 | Add inventory | Sets stock in inventory-service |
| 8 | Add to cart → Place order | Flow: Order → Feign calls → Kafka → Inventory & Payment |

---

## Quick Test Checklist

### Backend health

```bash
# API Gateway
curl http://localhost:8080/actuator/health

# Eureka - list registered services
# Open: http://localhost:8761
```

### Create a user

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"name\":\"Test User\",\"password\":\"secret123\"}"
```

### Create a product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Laptop\",\"description\":\"Good laptop\",\"price\":999.99,\"category\":\"Electronics\"}"
```

### Add inventory

```bash
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/json" \
  -d "{\"productId\":1,\"quantity\":100}"
```

### Place order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d "{\"userId\":1,\"items\":[{\"productId\":1,\"quantity\":2}]}"
```

---

## Troubleshooting

| Issue | Check | Fix |
|-------|-------|-----|
| Port already in use | `netstat -ano \| findstr :8080` (Windows) | Stop other process or change port |
| Eureka not loading | `docker compose logs eureka-server` | Wait longer, check health |
| API returns 502/503 | Eureka dashboard | Ensure all services are registered |
| Frontend can't reach API | CORS, proxy | Dev: use Vite proxy; Docker: nginx proxies `/api` |
| Kafka connection refused | `docker compose ps` | Ensure Kafka container is running |
| Redis connection failed | API Gateway logs | Start Redis: `docker compose up -d redis` |

---

## Port Reference

| Service | Port |
|---------|------|
| Frontend (Docker) | 3000 |
| Frontend (Vite dev) | 5173 |
| API Gateway | 8080 |
| Eureka | 8761 |
| Config Server | 8888 |
| User Service | 9002 |
| Product Service | 9003 |
| Order Service | 9004 |
| Inventory Service | 9005 |
| Payment Service | 9006 |
| Zipkin | 9411 |
| PostgreSQL (user) | 5432 |
| PostgreSQL (product) | 5433 |
| PostgreSQL (order) | 5434 |
| PostgreSQL (inventory) | 5435 |
| PostgreSQL (payment) | 5436 |
| Kafka | 9092 |
| Redis | 6379 |
