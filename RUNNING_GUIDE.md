# E-commerce Microservices — Complete Running Guide

Step-by-step guide to run both backend and frontend from scratch.

---

## Local/Dev vs production (summary)

| Goal | How to run | When to use |
|------|------------|-------------|
| **Local / dev (on your machine)** | **Option A (Docker):** `docker compose up -d --build` → http://localhost:3000. **Option B (Maven):** Start infra (Postgres, Redis, Kafka, Eureka) with Docker, then run each service with `mvn spring-boot:run -Dspring-boot.run.profiles=local`. Frontend: `cd frontend && npm run dev` → http://localhost:5173. | Day-to-day development, debugging. |
| **Production-like (Docker)** | `docker compose up -d --build` (optionally with `.env` and `SPRING_PROFILES_ACTIVE=prod`). | Staging, production, or “run everything in one go” locally. |

- **Local Maven:** Use profile `local` so services use localhost URLs and fixed ports (e.g. gateway 8080, Eureka 8761). You must start **Redis** (e.g. `docker compose up -d redis`) for the API Gateway. All DBs use the same Postgres instance on **port 5432** (user_db, product_db, order_db, inventory_db, payment_db).
- **Docker Compose (dev/prod-like):** Uses profile `prod` for app services; all services talk via container names. No `.env` required for a quick run (defaults for JWT, DB, etc. are set).
- **Production:** Use `cp .env.example .env`, set a strong `JWT_SECRET` (e.g. `openssl rand -base64 32`), then `docker compose up -d --build`. For full production checklist see [DEPLOYMENT.md](DEPLOYMENT.md).

---

## How to Run the Frontend

| Mode | How to run | URL |
|------|------------|-----|
| **With Docker (full stack)** | `docker compose up -d --build` then open in browser | **http://localhost:3000** |
| **Frontend only (local dev)** | `cd frontend` → `npm install` → `npm run dev` | **http://localhost:5173** |

- **Docker:** Frontend is built (Vite) and served by nginx. API calls go to `/api` and are proxied to the API Gateway. Ensure API Gateway is running (e.g. full `docker compose up`).
- **Local dev:** Vite dev server proxies `/api` to `http://localhost:8080`. Start the API Gateway (and backend services) first if you need real API calls.

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
- PostgreSQL (single instance with DBs: user_db, product_db, order_db, inventory_db, payment_db)
- Zookeeper + Kafka
- Redis
- Zipkin
- Eureka Server
- Config Server
- Auth Service (login, register, OAuth2, JWT)
- User, Product, Order, Inventory, Payment services
- API Gateway (JWT validation, routes to services)
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

### Step 1: Start infrastructure (PostgreSQL, Redis, Kafka)

You need:
- **PostgreSQL** — single instance with DBs: user_db, product_db, order_db, inventory_db, payment_db (created by init script on first start)
- **Redis** — required by API Gateway for rate limiting
- **Kafka** (with Zookeeper) — required for order, inventory, payment services

**Option B1: Run only infra with Docker (recommended)**

```bash
docker compose up -d postgres redis zookeeper kafka
```

**Optional:** Add Zipkin (tracing) and Eureka (so you can start gateway and microservices and they register):

```bash
docker compose up -d postgres redis zookeeper kafka zipkin eureka-server
```

**Explanation:** One Postgres container on port 5432; init script `init-db/01-create-databases.sh` creates all five databases on first run.

---

### Step 2: Databases

**If you used Docker for Postgres (Step 1):** The init script already created `user_db`, `product_db`, `order_db`, `inventory_db`, `payment_db`. Nothing to do.

**If you run PostgreSQL locally** (e.g. installed on your machine), create the databases:

```sql
CREATE DATABASE user_db;
CREATE DATABASE product_db;
CREATE DATABASE order_db;
CREATE DATABASE inventory_db;
CREATE DATABASE payment_db;
```

Postgres is on **port 5432** (single instance).

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

Open separate terminals. Start **user-service** before **auth-service** (auth calls user-service). Order Service needs User, Product, and Inventory.

```bash
# Terminal 3 - User Service (start first; auth-service depends on it)
cd user-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 4 - Auth Service (login, register, JWT, OAuth)
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 5 - Product Service
cd product-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 6 - Inventory Service
cd inventory-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 7 - Payment Service
cd payment-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Terminal 8 - Order Service
cd order-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Ports:**
- auth-service: 9001
- user-service: 9002
- product-service: 9003
- order-service: 9004
- inventory-service: 9005
- payment-service: 9006

---

### Step 6: Start API Gateway

```bash
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Port:** 8080 (same as frontend proxy in `vite.config.ts`)  
**Requires:** Redis on localhost:6379. If you didn’t start Redis in Step 1, run: `docker compose up -d redis`

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

### Register and login

```bash
# Register (returns JWT and user)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"name\":\"Test User\",\"password\":\"secret123\"}"

# Login (returns JWT and user)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"test@example.com\",\"password\":\"secret123\"}"
```

Use the `token` from the response in the `Authorization: Bearer <token>` header for protected endpoints.

### Create a product (requires JWT with role ADMIN)

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d "{\"name\":\"Laptop\",\"description\":\"Good laptop\",\"price\":999.99,\"category\":\"Electronics\"}"
```

### Add inventory

```bash
curl -X POST http://localhost:8080/api/inventory \
  -H "Content-Type: application/json" \
  -d "{\"productId\":1,\"quantity\":100}"
```

### Place order (requires JWT)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
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
| Auth Service | 9001 |
| User Service | 9002 |
| Product Service | 9003 |
| Order Service | 9004 |
| Inventory Service | 9005 |
| Payment Service | 9006 |
| Zipkin | 9411 |
| PostgreSQL (single instance, all DBs) | 5432 |
| Kafka | 9092 |
| Redis | 6379 |
