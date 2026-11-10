
# Project TITAN — Cloud-Native E-Commerce Platform

A hands-on DevOps and cloud engineering project focused on building, deploying, securing, and operating a real-world-inspired e-commerce platform.

**Repository:** `mohdshakerali/titan-commerce`  
**Project type:** DevOps / Cloud Engineering Portfolio  
**Current development phase:** Container Engineering Completed — Infrastructure Automation Next  
**Development approach:** Local-first, cost-conscious engineering

---

## 1. Project Overview

Project TITAN is a fictional B2C e-commerce platform developed to demonstrate practical software delivery and DevOps engineering.

The project explores how an engineering team builds and operates an application platform, including:

- Application development and integration
- Containerization and image optimization
- CI/CD automation
- Container networking and persistent storage
- Container security hardening
- Image distribution through a container registry
- Reliability troubleshooting and recovery
- Infrastructure automation and cloud deployment planning

The focus is on practical implementation, troubleshooting, operational evidence, and engineering documentation rather than installing tools without a clear purpose.

## 2. Current Application Architecture

TITAN currently consists of three primary services:

| Component | Technology | Responsibility |
|---|---|---|
| Frontend | React, TypeScript, Vite, Nginx | Customer-facing storefront |
| Backend | Java 21, Spring Boot | REST API and application logic |
| Database | PostgreSQL 17 | Persistent application data |

### Request flow

```text
Customer Browser / Postman
          |
          v
   Nginx Frontend
      Port 80
          |
          v
   Spring Boot API
      Port 8080
          |
          v
      PostgreSQL
      Port 5432
```

Docker Compose manages the three services.

Two Docker networks provide service segmentation:

- `frontend`: Nginx and the backend.
- `backend_db`: Backend and PostgreSQL; configured as an internal network.

Only the frontend publishes a host port. The backend and database are not directly published to the host.

PostgreSQL uses the external persistent Docker volume `titan-postgres-data`.

## 3. Technology Stack

| Area | Technologies |
|---|---|
| Application frontend | React, TypeScript, Vite |
| Application backend | Java 21, Spring Boot |
| Database | PostgreSQL |
| Containerization | Docker, Docker Compose |
| Container registry | GitHub Container Registry |
| CI | GitHub Actions |
| Image analysis | Trivy |
| Source control | Git, GitHub |
| Operating environment | Windows 11, Docker Desktop, WSL2 |
| Cloud platform planned | AWS |
| Infrastructure as Code planned | Terraform |
| Configuration management planned | Ansible |
| Orchestration planned | Kubernetes |
| Monitoring planned | Prometheus, Grafana |

Technologies listed as planned are not represented as completed implementations.

## 4. Engineering Progress

### M0 — Project Foundation
Project definition, business requirements, architecture and repository foundation.

### M1 — Application Engineering
Spring Boot backend, React frontend, PostgreSQL integration, customer authentication and product catalogue.

### M2 — Docker Engineering
The container engineering implementation and acceptance work covers:

- Docker runtime architecture
- Image optimization and build-context management
- BuildKit caching and CI image builds
- Container operations
- Network segmentation
- PostgreSQL persistence and backup/restore
- Container security hardening
- Docker Compose deployment configuration
- GHCR image publishing and retrieval
- Reliability incident simulations
- Engineering acceptance and operational handover

M2 implementation evidence is maintained in GitHub issues, pull requests and the documentation directory.

## 5. Running TITAN Locally

### Prerequisites

- Git
- Docker Desktop
- Docker Compose
- Windows 11 with Docker's Linux container environment

### Configuration

Clone the repository and enter its directory.

For a fresh local setup, copy the example environment file:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and provide unique local values for:

- `POSTGRES_PASSWORD`
- `TITAN_JWT_SECRET`

Do not commit the actual `.env` file or share its values.

Existing installations should retain their existing `.env` file rather than overwrite it.

Create the external PostgreSQL volume if it does not already exist:

```powershell
docker volume create titan-postgres-data
```

### Start the application

```powershell
docker compose config -q
docker compose up -d --build
```

Compose waits for PostgreSQL readiness before starting the backend, and for backend health before starting the frontend.

### Access the application

Storefront:

`http://localhost`

Product API:

`http://localhost/api/products`

Expected API response:

`HTTP 200 OK`

### Useful operational commands

```powershell
docker compose ps
docker compose logs -f
docker compose restart backend
docker compose stop
docker compose start
docker compose down
```

The external PostgreSQL volume is retained by normal Compose shutdown and container recreation. Never use volume-removal commands as routine cleanup.

## 6. Container Image Distribution

TITAN backend and frontend images have been published to GitHub Container Registry.

Backend:

`ghcr.io/mohdshakerali/titan-commerce-backend`

Frontend:

`ghcr.io/mohdshakerali/titan-commerce-frontend`

The M2-09 workflow demonstrated:

- Source-revision-based image tagging
- `latest` tags
- Registry authentication
- Image publishing and retrieval
- OCI manifest inspection
- Digest-based image identification

The registry images are separately documented. The current local Compose configuration continues to build from the repository's Dockerfiles.

## 7. CI Pipeline

GitHub Actions provides automated validation for:

1. Backend tests using Java 21 and PostgreSQL.
2. Frontend dependency installation, linting and production build.
3. Backend and frontend Docker image builds.

The Docker build job depends on successful backend and frontend validation.

The current workflow validates and builds images. It does not automatically publish them to GHCR or deploy the application.

## 8. Security and Reliability

Completed container engineering work includes:

- Non-root backend execution using UID 10001.
- Backend Linux capability removal.
- No-new-privileges protection.
- Separate frontend and database network paths.
- Runtime health checks and startup dependency conditions.
- External PostgreSQL persistent storage.
- Environment-based secret configuration.
- Trivy image vulnerability assessment and documented remediation decisions.
- Controlled failure simulations and troubleshooting documentation.

The PostgreSQL image still has documented upstream vulnerability findings. The project does not claim that every container image is vulnerability-free.

## 9. Engineering Documentation

Important technical documentation is maintained in `docs/`.

| Document | Location |
|---|---|
| Docker runtime architecture | `docs/architecture/docker-runtime.md` |
| Container network architecture | `docs/architecture/container-networking.md` |
| Image optimization | `docs/architecture/docker-image-optimization.md` |
| CI/CD pipeline architecture | `docs/architecture/ci-cd-pipeline.md` |
| Container operations | `docs/operations/container-operations-runbook.md` |
| PostgreSQL backup and restore | `docs/operations/postgres-backup-restore.md` |
| Compose deployment configuration | `docs/operations/compose-deployment-configuration.md` |
| Image publishing and retrieval | `docs/operations/container-image-publishing.md` |
| Reliability incident runbook | `docs/operations/container-reliability-runbook.md` |
| Container security review | `docs/security/container-security-review.md` |

## 10. Cost and Infrastructure Strategy

TITAN follows a local-first development approach.

Docker engineering, image publishing and local reliability testing are performed without requiring paid AWS resources.

Future cloud implementation will evaluate:

- AWS infrastructure requirements
- Terraform-managed resources
- Container registry integration with Amazon ECR
- IAM permissions and secrets management
- Kubernetes deployment
- Infrastructure costs and teardown procedures

Cloud resources will only be provisioned after evaluating cost and available credits.

## 11. Next Engineering Phases

The planned engineering direction includes:

- M3 — Linux, Bash and Ansible
- Terraform infrastructure automation
- AWS infrastructure implementation
- Kubernetes orchestration
- Helm and Kustomize
- GitOps deployment
- Monitoring and observability

These are future development activities, not completed M2 deliverables.

## 12. Engineering Philosophy

**Build it. Automate it. Secure it. Monitor it. Break it. Fix it. Document it.**

TITAN is intended to demonstrate how engineering technologies work together in a practical delivery environment, with a focus on operational understanding, reproducibility, reliability and cost awareness.
