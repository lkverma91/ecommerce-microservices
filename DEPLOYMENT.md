# Production Deployment Guide

## 1. Environment Variable Based Configuration

All services use environment variables for production. Key variables:

| Variable | Service | Description | Example |
|----------|---------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | All | Profile: dev, prod | prod |
| `EUREKA_URI` | All (clients) | Eureka server URL | http://eureka:8761/eureka/ |
| `DATABASE_URL` | user, product, order, inventory, payment | PostgreSQL JDBC URL | jdbc:postgresql://host:5432/db |
| `DATABASE_USERNAME` | All DB services | DB user | postgres |
| `DATABASE_PASSWORD` | All DB services | DB password | (from secret) |
| `KAFKA_BOOTSTRAP_SERVERS` | order, inventory, payment | Kafka brokers | host:9092 |
| `ZIPKIN_URL` | All | Zipkin endpoint | http://zipkin:9411/api/v2/spans |
| `JWT_SECRET` | api-gateway (future) | JWT signing secret | (from secret manager) |
| `REDIS_HOST` | api-gateway | Redis for rate limiting | redis |
| `CORS_ALLOWED_ORIGINS` | api-gateway | Allowed origins | https://app.example.com |
| `RATE_LIMIT_REPLENISH` | api-gateway | Requests per second | 10 |
| `RATE_LIMIT_BURST` | api-gateway | Burst capacity | 20 |

### Example `.env` for production

```bash
SPRING_PROFILES_ACTIVE=prod
EUREKA_URI=http://eureka-server:8761/eureka/

# Databases - use RDS/Cloud SQL endpoints
DATABASE_URL=jdbc:postgresql://your-db-host:5432/user_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-secure-password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Zipkin
ZIPKIN_URL=http://zipkin:9411/api/v2/spans

# Security - NEVER commit
JWT_SECRET=your-256-bit-secret-from-vault

# API Gateway
CORS_ALLOWED_ORIGINS=https://app.example.com
RATE_LIMIT_REPLENISH=20
RATE_LIMIT_BURST=40
REDIS_HOST=redis
```

---

## 2. Deploy with Docker Compose

### Prerequisites

- Docker 20.10+
- Docker Compose v2+
- 4GB+ RAM

### Local / Dev

```bash
# Start all services
docker-compose up -d

# Start with build
docker-compose up -d --build

# View logs
docker-compose logs -f api-gateway

# Stop all
docker-compose down
```

### Production with env file

```bash
# Create .env with production values
cp .env.example .env
# Edit .env - set JWT_SECRET, DATABASE_*, etc.

# Start
docker-compose --env-file .env up -d
```

### Service URLs (Docker Compose)

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8080 |
| Eureka Dashboard | http://localhost:8761 |
| Zipkin | http://localhost:9411 |
| Config Server | http://localhost:8888 |

---

## 3. Deploy on AWS EC2

### Step 1: Launch EC2 Instance

- AMI: Amazon Linux 2 or Ubuntu 22.04
- Instance type: t3.medium (min) for full stack
- Security group: Allow 22 (SSH), 80, 443, 8080, 8761
- Attach EBS volume (20GB+)

### Step 2: Install Docker

```bash
# Amazon Linux 2
sudo yum update -y
sudo yum install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Ubuntu
sudo apt update && sudo apt install docker.io docker-compose-plugin -y
sudo systemctl enable docker
sudo systemctl start docker
```

### Step 3: Clone and Configure

```bash
git clone <your-repo>
cd ecommerce-microservices

# Create production env
cat > .env << EOF
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=$(openssl rand -base64 32)
DATABASE_PASSWORD=your-secure-db-password
EOF
```

### Step 4: Use External Services (Recommended)

For production, use managed services:

- **RDS PostgreSQL**: Create 5 DB instances (or one with multiple DBs)
- **MSK (Managed Kafka)**: Or self-host Kafka on EC2
- **ElastiCache Redis**: For API Gateway rate limiting

Update `docker-compose.override.yml` or env to point to RDS/ElastiCache.

### Step 5: Run

```bash
docker compose up -d --build
```

### Step 6: Nginx Reverse Proxy (Optional)

```nginx
# /etc/nginx/conf.d/ecommerce.conf
server {
    listen 80;
    server_name api.yourdomain.com;
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 4. Scaling Services

### Horizontal Scaling with Docker Compose

```yaml
# docker-compose scale
docker compose up -d --scale user-service=2 --scale product-service=2
```

### Per-service scaling

```yaml
# In docker-compose.yml - add deploy replicas (Docker Swarm)
user-service:
  deploy:
    replicas: 3
```

### Load balancing

- Eureka + Ribbon/Spring Cloud LoadBalancer: Multiple instances of a service register with Eureka; the gateway and Feign clients automatically load balance.
- Run multiple containers: `docker compose up -d user-service user-service` (with different container names) or use `--scale`.

### Scaling strategy

| Service | Scale when | Suggested replicas |
|---------|------------|--------------------|
| api-gateway | High traffic | 2–4 |
| user-service | Many auth/profile requests | 2–3 |
| product-service | Catalog traffic | 2–4 |
| order-service | Order volume | 2–4 |
| inventory-service | Stock checks | 2 |
| payment-service | Payment volume | 2 |

---

## 5. Secure Production Environment

### 5.1 Secrets Management

**Never** commit secrets. Use:

- **AWS Secrets Manager**: Fetch at runtime
- **HashiCorp Vault**: Centralized secrets
- **Environment variables**: Set in CI/CD or orchestration
- **Docker secrets** (Swarm): `docker secret create db_password -`

```bash
# Example: Fetch from AWS Secrets Manager and export
export DATABASE_PASSWORD=$(aws secretsmanager get-secret-value --secret-id prod/db --query SecretString --output text)
```

### 5.2 JWT Secret Handling

```bash
# Generate secure secret
JWT_SECRET=$(openssl rand -base64 32)

# Set in environment (never in code)
export JWT_SECRET=$JWT_SECRET
```

### 5.3 Database Security

- Use strong passwords
- Restrict DB access to application subnets only
- Enable SSL for JDBC: `?ssl=true&sslmode=require`
- Use IAM auth for RDS when possible

### 5.4 Network Security

- Run services in private subnets
- Expose only API Gateway (and optionally Zipkin) to public
- Use VPC security groups to restrict traffic
- Enable VPC Flow Logs for auditing

### 5.5 API Gateway Security

- **HTTPS**: Terminate SSL at load balancer or Nginx
- **Rate limiting**: Configured via `RATE_LIMIT_REPLENISH` and `RATE_LIMIT_BURST`
- **CORS**: Restrict `CORS_ALLOWED_ORIGINS` to your frontend domains

### 5.6 Logging and Tracing

- Avoid logging sensitive data (passwords, tokens)
- Use structured logging (JSON) for aggregation
- Zipkin: Restrict access or disable in high-security environments

---

## 6. Configuration Examples

### Config Server bootstrap (if using Config Server)

```yaml
# bootstrap.yml in each service
spring:
  application:
    name: user-service
  cloud:
    config:
      uri: ${CONFIG_SERVER_URI:http://localhost:8888}
      fail-fast: false
```

### Kubernetes ConfigMap example

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ecommerce-config
data:
  EUREKA_URI: "http://eureka:8761/eureka/"
  SPRING_PROFILES_ACTIVE: "prod"
  ZIPKIN_URL: "http://zipkin:9411/api/v2/spans"
```

### Kubernetes Secret example

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ecommerce-secrets
type: Opaque
stringData:
  DATABASE_PASSWORD: "your-password"
  JWT_SECRET: "your-jwt-secret"
```

---

## 7. Health Checks

All services expose:

- **Liveness**: `GET /actuator/health/liveness`
- **Readiness**: `GET /actuator/health/readiness`

Docker HEALTHCHECK uses `/actuator/health`. For Kubernetes:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 9002
  initialDelaySeconds: 60
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 9002
  initialDelaySeconds: 30
  periodSeconds: 5
```

---

## 8. Error Response Format

All services return a consistent structure:

```json
{
  "timestamp": "2025-02-22T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: 123",
  "path": "/api/users/123",
  "traceId": "abc123",
  "validationErrors": {
    "email": "Invalid email format"
  }
}
```

`traceId` links to Zipkin for distributed tracing.
