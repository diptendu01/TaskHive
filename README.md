# TaskHive

[![Version](https://img.shields.io/badge/version-v0.2.0-14b8a6?style=flat-square)](https://github.com/diptendu01/TaskHive)
[![Java](https://img.shields.io/badge/Java-21%20LTS-007396?style=flat-square)](https://adoptium.net/temurin/releases)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?style=flat-square)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=flat-square)](https://vite.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?style=flat-square)](https://tailwindcss.com/)
[![GitHub Stars](https://img.shields.io/github/stars/diptendu01/TaskHive?style=flat-square)](https://github.com/diptendu01/TaskHive/stargazers)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](https://github.com/diptendu01/TaskHive/blob/main/LICENSE)

> A real-time collaborative Kanban board built with Spring Boot 4, Java 21, React 19, PostgreSQL, Redis Pub/Sub, fractional indexing, and JWT-authenticated STOMP WebSockets.

[Report an Issue](https://github.com/diptendu01/TaskHive/issues) · [Request a Feature](https://github.com/diptendu01/TaskHive/issues/new)

## Features

- **Real-Time Multi-User Collaboration** — Invite registered teammates by email via Workspace Members (`POST /api/v1/workspaces/{id}/members`) and sync board updates live over STOMP 1.2 WebSockets.
- **After-Commit Redis Pub/Sub Fan-Out** — Board events are dispatched strictly after JPA transaction commit (`TransactionSynchronization.afterCommit`) through Redis Pub/Sub (`taskhive:board-events`) with automatic local STOMP fallback for dev environments.
- **True Fractional Indexing & Auto-Rebalance** — $O(1)$ midpoint calculation (`FractionalIndexService`) for within-column and cross-column card reordering plus list reordering, with automatic column rebalancing when gaps shrink below `1e-4` and `@Version` optimistic locking for concurrent drops.
- **WebSocket Error Rollback** — Rejected WebSocket moves send targeted `/user/queue/errors` frames so the sender's optimistic UI reverts cleanly.
- **Hardened Security** — Stateless JWT auth on REST and STOMP `CONNECT`/`SUBSCRIBE`/`SEND` (including per-frame token expiration checks), board-ownership authorization, and Redis-backed login rate limiting (`429 Too Many Requests` after 10 failures in 15 minutes).
- **Full CRUD & OLED Black UI** — Editable card detail modal, inline list rename/reorder/delete, optimistic board creation/rename/delete, drag-to-delete drop zone, and single-click accessibility alternatives.
- **Unit & Testcontainers Integration Tests** — JUnit 5 tests for `FractionalIndexService` plus Testcontainers 2.x PostgreSQL integration tests covering auth, rate limiting, IDOR protection, workspace invites, fractional moves, and CRUD.

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21 LTS, Spring Boot 4.1.2, Spring Security 7, Spring WebSocket (STOMP), Spring Data JPA, Spring Data Redis (Lettuce), SpringDoc OpenAPI 3.2 |
| **Frontend** | React 19, Vite 8, Tailwind CSS 4 (`@tailwindcss/vite`), React Router, `@dnd-kit/core` |
| **Data & Scaling** | PostgreSQL 16 (Neon), Redis 7 Pub/Sub & Rate Limiting (Upstash) |
| **Testing** | JUnit 5, AssertJ, Testcontainers 2.x (`testcontainers-postgresql`) |
| **Deployment** | Cloudflare Pages (Frontend), Render Docker (Backend), Docker Compose (Local) |

## Getting Started

### Prerequisites

- **Java 21 JDK** & **Apache Maven 3.9+**
- **Node.js 22+ LTS**
- **Docker** (for Testcontainers integration tests or Docker Compose)

### Run Locally

**1. Backend (dev profile uses in-memory H2 in PostgreSQL mode by default):**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**2. Run Unit & Testcontainers Integration Tests:**

```bash
cd backend
mvn test
```

**3. Frontend:**

```bash
cd frontend
npm install
npm run dev
```

## License

This project is licensed under the MIT License.