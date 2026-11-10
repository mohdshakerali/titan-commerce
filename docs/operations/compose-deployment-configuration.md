
# TITAN Docker Compose Deployment Configuration

## 1. Purpose

This document describes the Docker Compose deployment configuration for TITAN Commerce.

It covers the local deployment architecture, environment configuration, service startup dependencies, persistent storage, operational commands, and considerations for production-oriented deployment.

The current Compose configuration is intended for local development and testing. It is not, by itself, a production deployment configuration.

## 2. Current Deployment Architecture

TITAN runs three primary services:

| Service | Responsibility | Network |
|---|---|---|
| frontend | Serves the React application through Nginx | frontend |
| backend | Runs the Spring Boot REST API | frontend, backend_db |
| postgres | Stores application data | backend_db |

### Network design

Two Docker bridge networks are configured:

- `frontend`: Connects the Nginx frontend to the backend API.
- `backend_db`: Connects the backend to PostgreSQL. This network is configured as internal.

Only the frontend publishes a host port. The backend exposes port 8080 to other containers, while PostgreSQL is not published to the host.

This limits direct external access to the backend and database containers.

## 3. Environment Configuration

Environment-specific values are separated from the Compose file using a local `.env` file.

The tracked `.env.example` file provides a template for required configuration.

| Variable | Purpose | Default / requirement |
|---|---|---|
| POSTGRES_DB | PostgreSQL database name | `titan_commerce` |
| POSTGRES_USER | PostgreSQL application user | `titan_user` |
| POSTGRES_PASSWORD | PostgreSQL password | Required |
| TITAN_JWT_SECRET | JWT signing secret | Required |
| FRONTEND_PORT | Host port for the frontend | `80` |

### Secret handling

- Actual credentials must remain in `.env`.
- `.env` is excluded from Git through `.gitignore`.
- `.env.example` must contain placeholders only.
- Never commit real passwords, tokens, or JWT secrets.

The database name and user variables are used when PostgreSQL initializes its data directory. Changing these values on an already initialized database does not automatically rename the existing database or user.

## 4. Startup Dependencies and Health Checks

The Compose configuration uses health checks to represent service readiness.

### PostgreSQL

The `pg_isready` command checks whether PostgreSQL is accepting connections.

The backend depends on PostgreSQL with:

`condition: service_healthy`

This prevents the backend from starting before PostgreSQL reports healthy.

### Backend

The backend health check calls:

`http://127.0.0.1:8080/actuator/health`

It uses the existing `wget` utility inside the backend container.

### Frontend

The frontend depends on the backend with:

`condition: service_healthy`

This prevents the frontend container from starting until the backend health check succeeds.

These dependency conditions control startup ordering. They do not provide automatic application-level recovery whenever a dependency becomes unhealthy later.

## 5. Persistent Storage

PostgreSQL stores its data in the external Docker volume:

`titan-postgres-data`

The volume is declared as external in Compose:

```yaml
volumes:
  postgres_data:
    external: true
    name: titan-postgres-data
```

This keeps database storage separate from the lifecycle of the Compose containers.

Removing and recreating the containers does not remove the external volume.

**Operational warning:** Never use volume-removal commands as routine deployment cleanup. Protect the database volume and maintain verified backups.

## 6. Supported Operational Commands

Run these commands from the project root.

| Command | Purpose |
|---|---|
| `docker compose config -q` | Validate Compose configuration |
| `docker compose up -d` | Start services in the background |
| `docker compose up -d --build` | Build images and start services |
| `docker compose ps` | Inspect service states and health |
| `docker compose logs -f` | Follow service logs |
| `docker compose restart backend` | Restart the backend |
| `docker compose stop` | Stop services without removing containers |
| `docker compose start` | Start previously stopped services |
| `docker compose down` | Remove Compose containers and networks |

The external PostgreSQL volume is retained when `docker compose down` is used without volume-removal options.

## 7. Local Development vs Production-Oriented Deployment

The current configuration is designed for local development. The following changes should be evaluated before production use.

| Area | Current local configuration | Production-oriented consideration |
|---|---|---|
| Application images | Built locally from Dockerfiles | Build and publish controlled, immutable images to a registry |
| PostgreSQL | Local PostgreSQL container | Consider managed or separately operated database infrastructure |
| Secrets | Local `.env` file | Use an approved secrets manager or secure deployment secret mechanism |
| Network exposure | Frontend publishes host port 80 | Use controlled ingress, TLS, and appropriate network restrictions |
| Image versions | PostgreSQL uses `postgres:17` | Pin and manage tested image versions or digests |
| Backups | Local backup and restore procedure | Implement scheduled backups and regularly tested recovery |
| Logging | Docker Engine default logging configuration | Define and monitor explicit log retention and collection |
| Resources | No arbitrary resource limits | Establish limits from workload measurements and capacity planning |
| Deployment | Docker Compose on a local machine | Use a controlled deployment environment and release process |

These are production considerations, not features currently implemented by this Compose file.

## 8. Resource and Logging Decisions

No explicit CPU or memory limits have been added.

Resource limits should be based on realistic workload measurements rather than arbitrary values. This avoids introducing unnecessary constraints into the current development environment.

The Compose file also does not override the Docker Engine logging driver. Production deployment should define an appropriate logging and retention policy.

## 9. Profiles and Multiple Compose Files

No additional Compose profiles or override files have been introduced.

The current deployment has three services and does not require optional service groups or separate Compose configurations to support its present scope.

Additional deployment configurations should only be introduced when a concrete operational requirement exists.

## 10. Validation and Recovery Evidence

The following checks were performed during M2-08:

- Compose configuration validation completed successfully.
- PostgreSQL reported healthy.
- Backend reported healthy.
- Frontend started after the backend became healthy.
- Postman `GET /api/products` returned HTTP 200 OK.
- The complete Compose stack was shut down using `docker compose down`.
- The stack was recreated using `docker compose up -d --no-build`.
- PostgreSQL and backend returned to healthy status.
- The Postman API request succeeded again after recovery.

The recovery test demonstrated that application data remained available after container recreation through the external PostgreSQL volume.

## 11. Operational Conclusion

The current Docker Compose configuration provides a consistent local deployment with:

- Explicit service startup dependencies.
- PostgreSQL and backend health checks.
- Separate frontend and database networks.
- External persistent database storage.
- Environment-based configuration.
- Container security settings inherited from previous hardening work.

Production deployment requires additional infrastructure, security, backup, observability, and release-management decisions. These are intentionally documented as future considerations rather than added without a concrete requirement.
