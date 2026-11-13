# Production Deployment Guide

## 1. Deployment Architectures Overview

The FreightFox Transporter Assignment application supports two primary deployment topologies:
1. **Containerized Deployment via Docker Compose** (Local workstations and self-hosted Linux VMs)
2. **Cloud PaaS Deployment on Render** (Managed Container Web Service + Managed PostgreSQL Database)

---

## 2. Live Cloud Deployment (Render PaaS)

The application is deployed on Render using the automated blueprint specification in `render.yaml`.

### Verified Production Endpoints
- **Base Application URL**: [https://freightfox-transporter-optimizer.onrender.com](https://freightfox-transporter-optimizer.onrender.com)
- **Interactive Swagger UI**: [https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html](https://freightfox-transporter-optimizer.onrender.com/swagger-ui/index.html)
- **OpenAPI 3 JSON Specification**: [https://freightfox-transporter-optimizer.onrender.com/v3/api-docs](https://freightfox-transporter-optimizer.onrender.com/v3/api-docs)
- **Actuator Health Check**: [https://freightfox-transporter-optimizer.onrender.com/actuator/health](https://freightfox-transporter-optimizer.onrender.com/actuator/health)

> [!IMPORTANT]
> **Active Dataset Snapshot Semantics**:
> Ingesting a new payload via `POST /api/v1/transporters/input` atomically replaces the currently active dataset snapshot in the database. Any previous lanes and quotes are archived, and the new dataset becomes active for all subsequent optimization requests. When testing the live deployment, be aware that posting input alters the dataset used for assignments.

### Render Free-Tier Operational Characteristics
- **Instance Spin-Down (Cold Starts)**: Free web services spin down after 15 minutes without incoming HTTP traffic. The first incoming request after idle triggers a container boot, which may take ~50 seconds before responding.
- **Resource Limits**: The free web service runs with 512 MB RAM and 0.1 vCPU.
- **Database Retention & Expiry Policies**: Render free-tier PostgreSQL databases are subject to Render's lifecycle policies (including storage caps and potential 30-day expiration on unlinked or trial accounts). Free tier availability is subject to change at Render's discretion and should not be treated as a permanent production SLA.

---

## 3. Render Blueprint Provisioning (`render.yaml`)

The repository includes a declarative `render.yaml` blueprint defining the complete topology:

```yaml
services:
  - type: web
    name: freightfox-transporter-optimizer
    runtime: docker
    plan: free
    region: oregon
    dockerfilePath: ./Dockerfile
    healthCheckPath: /actuator/health
    envVars:
      - key: SPRING_PROFILES_ACTIVE
        value: prod
      - key: SERVER_PORT
        value: "8080"
      - key: DB_HOST
        fromDatabase:
          name: freightfox-postgres-db
          property: host
      - key: DB_PORT
        fromDatabase:
          name: freightfox-postgres-db
          property: port
      - key: DB_NAME
        fromDatabase:
          name: freightfox-postgres-db
          property: database
      - key: DB_USERNAME
        fromDatabase:
          name: freightfox-postgres-db
          property: user
      - key: DB_PASSWORD
        fromDatabase:
          name: freightfox-postgres-db
          property: password

databases:
  - name: freightfox-postgres-db
    plan: free
    region: oregon
    databaseName: freightfox
    user: freightfox
```

### Setup Steps on Render:
1. Push repository commits to GitHub `main` branch.
2. In the [Render Dashboard](https://dashboard.render.com), click **New +** $\rightarrow$ **Blueprint**.
3. Select your GitHub repository (`freightfox-transporter-optimizer`).
4. Render parses `render.yaml` and displays the web service and database to be provisioned.
5. Click **Apply**. Database credentials are automatically injected into the web service environment without committing credentials into version control.

---

## 4. Docker Compose Deployment (Local & Self-Hosted)

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

### Accessing Local Endpoints
- **Local Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Local OpenAPI Docs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Local Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Inspecting Logs
```bash
docker compose logs -f app
```

### Stopping Services
```bash
# Stop containers preserving PostgreSQL data volume
docker compose down

# Stop and wipe database volume
docker compose down -v
```

---

## 5. Environment Variables Reference

| Variable | Default Value | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port for Spring Boot Tomcat |
| `SPRING_PROFILES_ACTIVE` | `local` | Active Spring profile (`local`, `prod`, `postgres-test`, `h2-test`) |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5434` (local) / `5432` (prod) | PostgreSQL port |
| `DB_NAME` | `freightfox` | Database name |
| `DB_USERNAME` | `freightfox` | Database user |
| `DB_PASSWORD` | `freightfox` | Database password |
| `SPRING_DATASOURCE_URL` | *(Optional)* | Full JDBC URL override (if specified, takes precedence) |

---

## 6. Operational Security & Maintenance

1. **Non-Root Container User**: The application executes under `appuser` (unprivileged Alpine user, UID 1000) created in the Dockerfile.
2. **Flyway Migrations**: Flyway executes on application startup. Migration scripts (`src/main/resources/db/migration/`) apply deterministically and record version history in `flyway_schema_history`.
3. **Snapshot Immutability**: Submitted datasets are stored as immutable snapshots, preventing in-place mutations from conflicting with concurrent assignment requests.
4. **Credential Isolation**: No production passwords or tokens are stored in configuration files or documentation. All secrets are injected dynamically via environment variables.
