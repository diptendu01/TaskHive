# 🚀 TaskHive

<div align="center">
  <img src="docs/images/taskhive-hero.png" alt="TaskHive Dashboard" width="800">
</div>

[![Version](https://img.shields.io/badge/version-v0.1.0-blue?style=flat-square)]
[![Java](https://img.shields.io/badge/java-21-orange?style=flat-square)]
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.2-brightgreen?style=flat-square)]
[![React](https://img.shields.io/badge/react-18-61DAFB?style=flat-square)]
[![License](https://img.shields.io/badge/license-MIT-yellow?style=flat-square)]
[![GitHub Stars](https://img.shields.io/github/stars/yourusername/taskhive?style=social)]

**A production-grade real-time Kanban board inspired by Trello with fractional indexing, enterprise security, and WebSockets. Built for developers who care about architecture.**

[Live Demo →](https://taskhive.vercel.app) | [Backend API →](https://taskhive-backend.up.railway.app) | [Report Issue →](https://github.com/yourusername/taskhive/issues/new/choose)
</div>

## ✨ Features

- **⚡ Real-time Collaboration** — WebSockets with STOMP protocol. See card movements instantly across all connected users.
- **🎯 Fractional Indexing** — O(1) drag-and-drop positioning. No database table scans when reordering lists or cards.
- **🔒 Enterprise Security** — JWT authentication, role-based access control (RBAC), BCrypt password hashing, method-level security.
- **📊 Built for Scale** — Includes caching with Redis, dynamic WebSocket scaling with Redis Pub/Sub.
- **🧪 Production Testing** — JUnit 5 + Mockito + Testcontainers for isolated PostgreSQL integration tests.
- **📦 Docker Ready** — One-command deploy using Docker Compose — run the entire stack in `docker-compose up`.
- **🎨 Modern UI** — React + TypeScript + Tailwind CSS with `@dnd-kit` for smooth drag-and-drop.

## 🛠 Tech Stack

<div align="center">

| Backend | Frontend | Database | Infrastructure |
|---------|----------|----------|----------------|
| Java 21 | React 18 | PostgreSQL 16 | Vercel / Railway / Supabase |
| Spring Boot 3.2 | TypeScript | Redis 7 | Docker Compose |
| JWT Auth | Tailwind CSS | | |

</div>

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 JDK
- Node.js 20 LTS

### Quick Start (Docker)

Run the entire stack locally in one command:

```bash
# Clone the repository
git clone https://github.com/yourusername/taskhive.git
cd taskhive

# Start all services
docker-compose -f docker-compose.local.yml up -d

# Your services are now running
# Frontend:  http://localhost:5173
# Backend:   http://localhost:8080
# API Docs:  http://localhost:8080/api-docs
```

### Or Use Local Tools

**Backend**

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Frontend**

```bash
cd frontend
npm install
npm run dev
```