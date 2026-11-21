# TaskHive

[![Version](https://img.shields.io/badge/version-v0.1.0-14b8a6?style=flat-square)](https://github.com/diptendu01/TaskHive)
[![Java](https://img.shields.io/badge/Java-21%20LTS-007396?style=flat-square)](https://adoptium.net/temurin/releases)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=flat-square)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=flat-square)](https://vite.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?style=flat-square)](https://tailwindcss.com/)
[![GitHub Stars](https://img.shields.io/github/stars/diptendu01/TaskHive?style=flat-square)](https://github.com/diptendu01/TaskHive/stargazers)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](https://github.com/diptendu01/TaskHive/blob/main/LICENSE)

> A production-grade, real-time Kanban board inspired by Trello—built with fractional indexing, JWT-authenticated STOMP WebSockets, Spring Boot 4, and a high-contrast OLED black React UI.

[Report an Issue](https://github.com/diptendu01/TaskHive/issues) · [Request a Feature](https://github.com/diptendu01/TaskHive/issues/new)

## Features

- **Real-Time Collaboration** — Native browser WebSockets using STOMP 1.2 (`@EnableWebSocketMessageBroker`) with automatic exponential-backoff reconnection so card creations, moves, and deletions sync instantly across open clients.
- **Fractional Indexing** — O(1) drag-and-drop reordering for lists and cards without full-column database rewrites.
- **Hardened Security** — Stateless JWT authentication (REST + STOMP `CONNECT`/`SUBSCRIBE`), BCrypt password hashing, board-ownership and same-board authorization checks, and locked-down production CORS/error handling.
- **OLED Black UI & Accessibility** — Pure-black (`#000000`) theme with `#14B8A6` teal accents, cursor spotlight hover effects, smooth `@dnd-kit` drag-and-drop, drag-to-delete drop zone, and single-click delete/keyboard alternatives.
- **Cloud-Ready Persistence** — PostgreSQL 16 (via Hibernate/JPA) and TLS-capable Redis 7 (ready for Upstash or local Docker).
- **Automated Integration Tests** — JUnit 5, Mockito, and Testcontainers 2.x for isolated PostgreSQL integration testing.

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21 LTS, Spring Boot 4.1.2, Spring Security 7, Spring WebSocket (STOMP), Spring Data JPA, Spring Data Redis (Lettuce), SpringDoc OpenAPI 3.2 |
| **Frontend** | React 19, Vite 8, Tailwind CSS 4 (`@tailwindcss/vite`), React Router, `@dnd-kit` |
| **Data** | PostgreSQL 16, Redis 7 |
| **Deployment** | Cloudflare Pages (Frontend), Render Docker (Backend), Neon (Postgres), Upstash (Redis), Docker Compose (Local / VPS) |

## Project Structure

```text
TaskHive/
├── backend/                 # Spring Boot 4.1.2 REST + STOMP WebSocket API (Maven, Dockerfile)
├── frontend/                # React 19 + Vite 8 + Tailwind CSS 4 SPA
└── docker-compose.local.yml # Local PostgreSQL + Redis + full-stack Compose setup
```

## Getting Started

### Prerequisites

- **Java 21 JDK** & **Apache Maven 3.9+**
- **Node.js 22+ LTS** (or Node.js 26 LTS)
- **Docker & Docker Compose** (optional, for containerized local run)

### Quick Start (Docker Compose)

```bash
git clone https://github.com/diptendu01/TaskHive.git
cd TaskHive
docker compose -f docker-compose.local.yml up -d
```

- **Frontend:** `http://localhost:5173`
- **Backend API:** `http://localhost:8080`
- **Health Check:** `http://localhost:8080/actuator/health`

### Run Locally Without Docker

**1. Backend (Dev profile uses in-memory H2 in PostgreSQL mode by default):**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**2. Frontend:**

```bash
cd frontend
npm install
npm run dev
```

## Environment Variables (Production)

### Backend (`SPRING_PROFILES_ACTIVE=prod`)

| Variable | Description |
| :--- | :--- |
| `SPRING_DATASOURCE_URL` | JDBC PostgreSQL URL (e.g., `jdbc:postgresql://<host>/neondb?sslmode=require`) |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `SPRING_REDIS_HOST` | Redis hostname (plain host, without `https://` or `rediss://`) |
| `SPRING_REDIS_PORT` | Redis port (default `6379`) |
| `SPRING_REDIS_PASSWORD` | Redis password |
| `SPRING_DATA_REDIS_SSL_ENABLED` | Set to `true` for TLS providers such as Upstash |
| `JWT_SECRET` | Random secret key (minimum 32 characters; 48+ recommended) |
| `FRONTEND_URL` | Allowed frontend origin for CORS & WebSockets (no trailing slash) |
| `PORT` | HTTP port (default `8080`; auto-injected on Render) |

### Frontend (Vite Build)

| Variable | Description |
| :--- | :--- |
| `VITE_API_URL` | Base backend URL (e.g., `https://your-backend.onrender.com`) |
| `VITE_WS_URL` | WebSocket endpoint URL (e.g., `wss://your-backend.onrender.com/ws`) |

## License

This project is licensed under the MIT License.