# E-commerce Frontend

React (Vite) + TypeScript + TailwindCSS frontend for the e-commerce microservices backend.

## Folder Structure

```
frontend/
├── public/
├── src/
│   ├── api/              # API client & endpoints
│   │   ├── axiosInstance.ts   # Axios + JWT interceptor
│   │   ├── authApi.ts
│   │   ├── productApi.ts
│   │   ├── inventoryApi.ts
│   │   └── orderApi.ts
│   ├── components/
│   │   ├── Layout.tsx
│   │   ├── ProtectedRoute.tsx
│   │   └── ErrorBoundary.tsx
│   ├── context/
│   │   ├── AuthContext.tsx
│   │   └── CartContext.tsx
│   ├── pages/
│   │   ├── HomePage.tsx
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   ├── ProductListPage.tsx
│   │   ├── ProductDetailPage.tsx
│   │   ├── CartPage.tsx
│   │   ├── CheckoutPage.tsx
│   │   ├── OrdersPage.tsx
│   │   └── AdminProductsPage.tsx
│   ├── types/
│   │   └── index.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── index.html
├── vite.config.ts
├── tailwind.config.js
├── Dockerfile
└── nginx.conf
```

## Setup

```bash
cd frontend
npm install
```

## Environment

| Variable | Description | Dev | Prod |
|----------|-------------|-----|------|
| `VITE_API_BASE_URL` | API base (no trailing slash) | `/api` (proxy) | `https://api.example.com/api` |

- **Dev**: Vite proxies `/api` → `http://localhost:8080`
- **Prod**: Set full URL at build time

## Run

```bash
# Development (API via proxy)
npm run dev

# Production build
npm run build

# Preview build
npm run preview
```

## Docker

```bash
# Build (default API=/api)
docker build -t ecommerce-frontend .

# Build with prod API URL
docker build --build-arg VITE_API_BASE_URL=https://api.example.com/api -t ecommerce-frontend .
```

## Features

- **Auth**: Login, Register, JWT in header
- **Axios interceptor**: Adds Bearer token, handles 401 → redirect to login
- **Protected routes**: Cart, Checkout, Orders, Admin
- **Product listing**: With category filter
- **Product details**: Add to cart
- **Cart**: Update quantity, remove, persist in localStorage
- **Checkout**: Place order via Order Service
- **Orders**: List user orders
- **Admin**: CRUD products (requires auth)
