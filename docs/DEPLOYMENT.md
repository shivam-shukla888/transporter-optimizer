# Production Deployment Guide

## 1. Deployment Architectures Overview

The FreightFox Transporter Assignment application supports two primary deployment topologies:
1. **Containerized Deployment via Docker Compose** (Local and Self-Hosted Linux VMs)
2. **Cloud PaaS Deployment on Render** (Managed Container Web Service + Managed PostgreSQL Database)

---

## 2. Docker Compose Deployment (Local & Self-Hosted)

### Prerequisites
- Docker Engine 24+
- Docker Compose v2+

### Running the Services
To build and start both the application and PostgreSQL 17 in detached mode:

```bash
docker compose up --build -d
```

### Checking Status & Health
```bash
docker compose ps
```
Expected output:
```
NAME                  STATUS                    PORTS
freightfox-app        Up (healthy)              0.0.0.0:8080->8080/tcp
freightfox-postgres   Up (healthy)              0.0.0.0:5434->5432/tcp
```

### Inspecting Logs
```bash
docker compose logs -f app
```

### Shutting Down
```bash
# Stop containers preserving database volume
docker compose down

# Stop and wipe database volume
docker compose down -v
```

---

## 3. Render Deployment Guide (Cloud PaaS)

The repository provides an automated Infrastructure-as-Code blueprint via `render.yaml`.

### Step-by-Step Render Setup

1. **Push Repository to GitHub**:
   Ensure your code is pushed to your GitHub repository (e.g., `main` branch).

2. **Log into Render Dashboard**:
   Navigate to [https://dashboard.render.com](https://dashboard.render.com).

3. **Deploy via Blueprint**:
   - In the Render dashboard, click **New +** $\rightarrow$ **Blueprint**.
   - Connect your GitHub repository (`freightfox-transporter-optimizer`).
   - Render detects `render.yaml` and provisions:
     - **Web Service**: `freightfox-transporter-optimizer` (Docker runtime, Free plan)
     - **Database**: `freightfox-postgres-db` (PostgreSQL 16/17, Free plan)
   - Click **Apply**.

4. **Automatic Environment Wiring**:
   Render automatically wires the database credentials into the web service environment:
   - `DB_HOST` $\leftarrow$ `fromDatabase.host`
   - `DB_PORT` $\leftarrow$ `fromDatabase.port`
   - `DB_NAME` $\leftarrow$ `fromDatabase.database`
   - `DB_USERNAME` $\leftarrow$ `fromDatabase.user`
   - `DB_PASSWORD` $\leftarrow$ `fromDatabase.password`
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `SERVER_PORT` = `8080`

5. **Liveness & Health Verification**:
   Render monitors `/actuator/health`. When the Spring Boot application finishes Flyway database migrations and starts Tomcat, Render marks the service healthy.

### Accessing the Deployed Application
- **Base URL**: `https://<your-render-app-name>.onrender.com`
- **Swagger UI**: `https://<your-render-app-name>.onrender.com/swagger-ui.html`
- **Health Check**: `https://<your-render-app-name>.onrender.com/actuator/health`

---

## 4. Environment Variables Reference

| Variable | Default Value | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port for Spring Boot Tomcat |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile (`local`, `prod`, `test`) |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5434` (local) / `5432` (prod) | PostgreSQL port |
| `DB_NAME` | `freightfox` | Database name |
| `DB_USERNAME` | `freightfox` | Database user |
| `DB_PASSWORD` | `freightfox` | Database password |
| `SPRING_DATASOURCE_URL` | *(Derived)* | Full JDBC URL override if needed |

---

## 5. Security & Operational Runbook

1. **Non-Root Execution**: The Docker container executes under user `appuser` (Alpine unprivileged user).
2. **Database Migrations**: Flyway runs automatically on application startup. Schema versions are tracked in the `flyway_schema_history` table.
3. **Graceful Shutdown**: Tomcat supports graceful shutdown, waiting for active optimization computations to complete before terminating.
