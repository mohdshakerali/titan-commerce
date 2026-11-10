# TITAN Container Operations Runbook

## Purpose and scope

This runbook covers routine operation and first-line diagnosis of the local TITAN Docker Compose stack:

- **PostgreSQL** — persistent application database
- **Backend** — Spring Boot API
- **Frontend** — Nginx serving the React application and proxying API requests

Commands are written for PowerShell from the repository root. They assume the Compose project has been configured and the required values are available in the local `.env` file.

This is a local Compose operations guide. It is not a production disaster-recovery plan, an off-host backup policy, or an orchestration-platform procedure.

## 1. Service status and first response

View service state, health and published ports:

```powershell
docker compose ps
```

View recent logs for one service:

```powershell
docker compose logs --tail 100 --timestamps --no-color backend
docker compose logs --tail 100 --timestamps --no-color frontend
docker compose logs --tail 100 --timestamps --no-color postgres
```

Follow logs while reproducing an issue (press Ctrl+C to stop following logs; this does not stop the container):

```powershell
docker compose logs --follow --tail 50 backend
```

Start diagnosis by identifying the affected service, capturing its recent logs and inspecting its state before restarting it. A restart can clear transient evidence, so collect logs first.

## 2. Service-specific checks

### PostgreSQL

Check the Compose health state:

```powershell
docker compose ps postgres
```

Check whether PostgreSQL accepts connections:

```powershell
docker compose exec postgres pg_isready -U titan_user -d titan_commerce
```

Expected output includes `accepting connections`.

Review database startup, shutdown, checkpoint and recovery messages:

```powershell
docker compose logs --tail 150 --timestamps --no-color postgres
```

Messages such as `database system is ready to accept connections` indicate readiness. During a controlled fast shutdown, PostgreSQL may log that it is terminating active sessions; that is expected for this shutdown mode. Investigate unexpected repeated crash-recovery messages, failed checkpoints, or failure to become ready.

### Spring Boot backend

Review application and database-connection logs:

```powershell
docker compose logs --tail 150 --timestamps --no-color backend
```

Check the actuator health endpoint from the frontend container over the internal Compose network:

```powershell
docker exec titan-commerce-frontend-1 wget -qO- http://backend:8080/actuator/health
```

Expected response has `"status":"UP"`; the current response also includes the `liveness` and `readiness` health groups.

The backend is not published directly to the host. Use the Compose service DNS name `backend` for internal checks rather than assuming that port 8080 is available on the host.

### Nginx frontend

Review Nginx startup and access/error logs:

```powershell
docker compose logs --tail 150 --timestamps --no-color frontend
```

Check the published frontend from the host:

```powershell
Invoke-WebRequest -Uri http://localhost/ -Method Get
```

A successful HTTP response confirms that the host can reach Nginx. Application API functionality should also be checked through the UI or backend health endpoint; a successful page response alone does not prove the backend or database is healthy.

## 3. Safe use of docker exec

Use `docker exec` or `docker compose exec` for a specific diagnostic command, and prefer a direct executable over opening an interactive shell. Examples:

```powershell
docker compose exec postgres pg_isready -U titan_user -d titan_commerce
docker exec titan-commerce-frontend-1 wget -qO- http://backend:8080/actuator/health
docker compose exec frontend nginx -t
```

Use `-T` for commands that should not allocate a pseudo-terminal, particularly when scripting. Do not print environment variables or dump process environments into tickets or logs: they can contain database credentials and the JWT signing secret. Do not edit files inside a running container as a permanent configuration change; update the tracked source/configuration and recreate the service.

## 4. Container state, exit codes and restart policies

Inspect state and lifecycle metadata for the current containers:

```powershell
docker inspect titan-commerce-backend-1 titan-commerce-frontend-1 titan-commerce-postgres-1 --format '{{.Name}} | status={{.State.Status}} | exit={{.State.ExitCode}} | oom={{.State.OOMKilled}} | error={{.State.Error}} | restarts={{.RestartCount}} | restart-policy={{.HostConfig.RestartPolicy.Name}} | health={{if .State.Health}}{{.State.Health.Status}}{{else}}not-configured{{end}}'
```

Useful fields:

- `State.Status`: whether the container is running, exited, restarting, or in another lifecycle state.
- `State.ExitCode`: the last process exit code when available. Zero normally indicates a successful process exit; a non-zero value needs log/context review.
- `State.OOMKilled`: whether Docker recorded an out-of-memory kill. Do not diagnose OOM from exit code alone.
- `State.Error`: runtime error detail, when present.
- `RestartCount`: number of restarts for this container instance.
- `HostConfig.RestartPolicy.Name`: configured restart policy.
- `State.Health.Status`: health-check state, when a health check is configured.

All three current services use `restart: unless-stopped`. Docker can restart a service after an unexpected process exit; a container explicitly stopped by an operator is not automatically brought back solely by this policy. A restart policy is not a substitute for application health checks or incident diagnosis.

Current health-check coverage is uneven: PostgreSQL has a Compose health check; backend and frontend do not. For those services, use the service-specific probes in this runbook. A container being `running` only means its main process is running; it does not establish application readiness.

## 5. Routine lifecycle operations

Restart one service after capturing relevant logs:

```powershell
docker compose restart backend
docker compose restart frontend
docker compose restart postgres
```

Stop and start the stack without removing its containers or volumes:

```powershell
docker compose stop
docker compose start
```

Apply a changed image/configuration to one service after its source/configuration has been updated:

```powershell
docker compose build backend
docker compose up -d --no-deps backend
```

Replace `backend` with the service being changed. Use `docker compose ps` and the service-specific checks above after the operation.

The current tests confirmed:
- Spring Boot received the termination signal, completed Tomcat graceful shutdown, closed JPA and Hikari, then restarted and reconnected to PostgreSQL.
- Nginx received `SIGQUIT`; its worker processes exited with code 0 and the master exited before Nginx started again.
- PostgreSQL received a fast shutdown request, completed a shutdown checkpoint, shut down cleanly, reused its existing data directory, and became ready again.
- After the database restart, `pg_isready` reported that PostgreSQL was accepting connections and the backend actuator endpoint reported `UP`.

The configured stop signals observed in the current containers are Nginx `SIGQUIT` and PostgreSQL `SIGINT`. The backend has no explicit stop signal configured and uses Docker's default `SIGTERM`. The observed logs show these signals were handled gracefully in the tested restarts.

## 6. Resource usage and limits

Take a point-in-time resource snapshot:

```powershell
docker stats --no-stream
```

The initial idle snapshot during this operational review showed:

| Service | CPU | Memory |
|---|---:|---:|
| Frontend | 0.00% | 7.312 MiB |
| Backend | 0.38% | 288.4 MiB |
| PostgreSQL | 0.00% | 59.21 MiB |

These are observations from one low-load snapshot, not capacity or load-test results. The displayed memory limit was Docker Desktop's shared 7.625 GiB limit, not a per-service reservation.

**No per-container CPU or memory constraints were added.** The available evidence does not establish safe service-specific limits, and the laptop has constrained local capacity. Revisit limits only after representative load testing, with headroom for JVM startup/heap behaviour and PostgreSQL operations. Do not copy these idle values into production limits.

## 7. Data-safe recovery

The PostgreSQL data directory is stored in the external named Docker volume:

```text
titan-postgres-data
```

The Compose file declares this volume as external. Treat it as persistent application data.

### If a service is unhealthy

1. Capture `docker compose ps`, recent service logs, and inspect metadata before restarting.
2. Check PostgreSQL readiness first with `docker compose exec postgres pg_isready -U titan_user -d titan_commerce`.
3. If PostgreSQL is ready but the backend is unhealthy, review backend startup/connection logs, then restart only the backend if appropriate.
4. Check the backend actuator endpoint from the frontend container.
5. Check frontend logs and host HTTP access.
6. Escalate to restoring data only when there is evidence of data damage and a known-good backup is available. A container restart is not a database restore.

### If containers need to be recreated

A normal Compose teardown and recreation preserves the external named volume:

```powershell
docker compose down
docker compose up -d
```

This removes the Compose containers and network, then recreates the services. Because the database volume is external, it is not removed by this normal `down` operation.

**Never use `docker compose down -v` for routine recovery. Never run `docker volume rm titan-postgres-data` or indiscriminate `docker volume prune` as a troubleshooting shortcut.** These actions can delete persistent database data or other volumes. Do not manually remove the PostgreSQL data directory.

After recreation, wait for PostgreSQL health, confirm `pg_isready`, then confirm backend actuator health and frontend HTTP access.

### Recovery boundary

This runbook documents container-level restart/recreation while preserving the current local volume. It does not yet provide an automated, off-host PostgreSQL backup, point-in-time recovery, or a tested disaster-recovery procedure. Before relying on TITAN data, create and verify an independent backup and test restoration to a separate target.

## 8. Known operational limitations

- Only PostgreSQL has a Docker health check in the current Compose configuration.
- The frontend depends on backend `service_started`, not an application-level readiness condition.
- Resource measurements were collected under normal idle/local usage only; no representative load test was performed.
- The local external PostgreSQL volume is persistent but is not itself a backup.
- A historical PostgreSQL log entry showed automatic recovery after an earlier interruption; recovery completed and PostgreSQL became ready. The controlled restart performed during this review shut down cleanly.

Review these limitations when the Compose architecture evolves and before using this setup as a production deployment template.
