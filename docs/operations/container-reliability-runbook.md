
# TITAN Container Reliability and Incident Troubleshooting Runbook

## 1. Purpose

This runbook documents the investigation, diagnosis, resolution and recovery of container reliability incidents in TITAN Commerce.

The objective is to establish a repeatable approach to diagnosing failures involving application startup, database connectivity, reverse proxy communication, host port allocation and container restart behaviour.

All incident simulations in this milestone were performed locally using Docker Desktop on Windows 11 with WSL2.

## 2. TITAN Runtime Architecture

The application consists of three primary services:

| Service | Responsibility |
|---|---|
| frontend | Nginx reverse proxy and React application |
| backend | Spring Boot REST API |
| postgres | PostgreSQL database |

The backend and frontend communicate through the `frontend` Docker network.

The backend and PostgreSQL communicate through the internal `backend_db` network.

PostgreSQL data is stored in the external Docker volume:

`titan-postgres-data`

The database volume is persistent and must not be removed during routine incident investigation or recovery.

## 3. Standard Incident Investigation Procedure

Use the following investigation sequence.

### Step 1: Identify the affected service

Determine whether the failure affects:

- Frontend
- Backend
- PostgreSQL
- Docker network
- Host port publishing
- Persistent storage

### Step 2: Inspect service state

```powershell
docker compose ps
```

Identify stopped, unhealthy, restarting or unavailable containers.

### Step 3: Review container logs

```powershell
docker logs <container-name>
```

For Compose services:

```powershell
docker compose logs <service-name>
```

Look for application startup exceptions, failed connections, permission issues and upstream errors.

### Step 4: Inspect container state

```powershell
docker inspect <container-name>
```

Useful runtime properties include:

- State
- ExitCode
- RestartCount
- OOMKilled
- NetworkSettings
- Configured startup command

### Step 5: Investigate resources and networking

Resource usage:

```powershell
docker stats
```

Network configuration:

```powershell
docker network inspect <network-name>
```

### Step 6: Apply corrective action

Correct the identified root cause rather than repeatedly restarting containers without understanding the failure.

### Step 7: Verify recovery

Use an application-level request wherever possible.

TITAN API smoke test:

`GET http://localhost/api/products`

Expected successful response:

`HTTP 200 OK`

## 4. Incident 1 — Backend Startup Failure

### Scenario

The backend container was deliberately started with an invalid application JAR path.

### Failure injection

```powershell
docker run --name titan-incident-backend --entrypoint /bin/sh ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84 -c "exec java -jar /app/missing.jar"
```

### Observed symptom

```text
Error: Unable to access jarfile /app/missing.jar
```

### Diagnostic evidence

```text
Status=exited
ExitCode=1
Command=/bin/sh
Args=["-c","exec java -jar /app/missing.jar"]
```

### Root cause

The container was instructed to execute a JAR file that did not exist at the specified path.

This was an intentionally incorrect startup command, not a defect in the actual TITAN backend image.

### Corrective action

Use the correct application JAR path and the Docker image's configured entrypoint.

The real backend image was not modified during the simulation.

### Prevention

- Verify application artifact paths during image builds.
- Keep Dockerfile startup commands consistent with the packaged artifact.
- Inspect container logs when startup fails.
- Validate container startup before deployment.

## 5. Incident 2 — Backend Cannot Connect to PostgreSQL

### Scenario

The backend was started with an incorrect PostgreSQL hostname.

### Failure injection

```powershell
docker compose run --rm --no-deps -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres.invalid:5432/titan_commerce backend
```

### Observed symptoms

The Spring Boot application started but failed during database initialization.

Important log evidence:

```text
SQLState: 08001
The connection attempt failed.
```

Root exception:

```text
java.net.UnknownHostException: postgres.invalid
```

Hibernate subsequently failed to initialize because it could not obtain database metadata.

### Root cause

The JDBC URL used `postgres.invalid`, which was not a resolvable Docker service hostname.

### Correct configuration

```text
jdbc:postgresql://postgres:5432/titan_commerce
```

The hostname `postgres` corresponds to the PostgreSQL Compose service.

### Corrective action

Restore the correct database service hostname in the application's database connection configuration.

The incorrect URL was supplied only to a temporary container. No Compose configuration or database data was changed.

### Recovery verification

The actual TITAN API returned HTTP 200 OK through Postman.

### Prevention

- Use Docker Compose service names for internal communication.
- Avoid hardcoding container IP addresses.
- Keep service configuration consistent across deployment environments.
- Verify database DNS resolution and connectivity when investigating connection failures.

## 6. Incident 3 — Frontend Reverse Proxy Cannot Reach Backend

### Scenario

The backend container was deliberately stopped while the frontend and PostgreSQL remained running.

### Failure injection

```powershell
docker stop titan-commerce-backend-1
```

### Observed symptom

Postman request:

`GET http://localhost/api/products`

Returned:

`502 Bad Gateway`

### Diagnostic evidence

Nginx logged:

```text
connect() failed (113: Host is unreachable)
while connecting to upstream
```

The affected upstream was:

```text
http://172.19.0.2:8080/api/products
```

Nginx returned HTTP 502 to the client.

### Root cause

The backend container was unavailable while the Nginx frontend continued receiving requests.

The reverse proxy could not reach its configured upstream service.

### Corrective action

Restart the existing backend service:

```powershell
docker compose start backend
```

The backend and PostgreSQL returned to service.

### Recovery verification

Postman returned:

`HTTP 200 OK`

The recovery request completed successfully in approximately 26 ms.

### Prevention

- Configure appropriate service health checks.
- Use Docker Compose dependency conditions.
- Monitor backend readiness.
- Investigate Nginx upstream errors using container logs.
- Ensure the backend and frontend share the required Docker network.

## 7. Incident 4 — Host Port Conflict

### Scenario

A second frontend container attempted to publish host port 80 while the actual TITAN frontend already occupied it.

### Failure injection

```powershell
docker run --rm -d --name titan-incident-port-conflict -p 80:80 ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
```

### Observed error

```text
Bind for 0.0.0.0:80 failed:
port is already allocated
```

### Root cause

The host port was already allocated by the existing TITAN frontend container.

Two independent containers cannot simultaneously bind the same host IP and port.

### Initial recovery attempt

The container was started using host port 8081:

```powershell
docker run --rm -d --name titan-incident-port-recovery -p 8081:80 ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
```

However, the container was attached to Docker's default bridge network.

Nginx logged:

```text
host not found in upstream "backend"
```

This exposed a second issue: the standalone container could not resolve the backend service name because it was not connected to TITAN's frontend network.

### Corrective action

The failed temporary container was removed and recreated using the existing TITAN frontend network.

```powershell
docker run -d --name titan-incident-port-recovery --network titan-commerce_frontend -p 8081:80 ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84
```

This provided:

- Host port 8081.
- Container port 80.
- Access to the existing TITAN frontend network.
- Docker DNS resolution of the backend service.

### Recovery verification

Postman request:

`GET http://localhost:8081/api/products`

Returned:

`HTTP 200 OK`

### Prevention

- Check existing host port allocations before deployment.
- Use alternate host ports when required.
- Attach standalone service containers to the appropriate user-defined Docker network.
- Ensure reverse proxy upstream hostnames are resolvable from the container.

## 8. Incident 5 — Container Restart Loop

### Scenario

A deliberately incorrect startup command was combined with Docker's `on-failure` restart policy.

### Failure injection

```powershell
docker run -d --name titan-incident-restart-loop --restart on-failure:3 --entrypoint /bin/sh ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84 -c "exec java -jar /app/missing.jar"
```

### Observed symptoms

The container repeatedly exited because Java could not access the specified JAR file.

### Diagnostic evidence

```text
Status=exited
ExitCode=1
RestartCount=3
OOMKilled=false
```

### Root cause

The startup command referenced a nonexistent application artifact.

Docker's restart policy retried the failed container three times and stopped retrying after reaching its configured retry limit.

This was not a memory exhaustion incident.

### Corrective action

Correct the application startup command or image configuration.

A restart policy cannot repair a persistent configuration error.

### Prevention

- Validate image entrypoints and artifact paths.
- Review restart counts when investigating repeated container failures.
- Distinguish application crashes from OOM termination.
- Use bounded restart policies where appropriate.
- Investigate the original failure before repeatedly restarting services.

## 9. Incident Summary

| Incident | Injected failure | Root cause | Recovery evidence |
|---|---|---|---|
| Backend startup | Missing JAR | Invalid startup command | Actual TITAN API remained operational |
| Database connection | Invalid hostname | Docker DNS hostname resolution failure | Postman HTTP 200 OK |
| Reverse proxy | Backend stopped | Upstream service unavailable | Postman HTTP 200 OK |
| Host port conflict | Duplicate port 80 | Host port already allocated | Postman HTTP 200 OK via port 8081 |
| Restart loop | Missing JAR with restart policy | Persistent startup configuration error | Actual TITAN API HTTP 200 OK |

## 10. Data Protection and Safety

The incident simulations were performed using disposable containers or controlled service operations.

The following safeguards were maintained:

- The external PostgreSQL volume `titan-postgres-data` was not removed.
- No volume-removal command was used.
- No database files were manually modified.
- The invalid database URL was applied only to a temporary container.
- The actual database and its persistent storage were not deliberately corrupted.
- Temporary incident containers were separated from the production-like Compose configuration.
- The real TITAN API was tested after incident recovery.

## 11. Operational Lessons

The simulations demonstrated several important container engineering principles:

1. A running container does not necessarily mean its application is ready.
2. Application logs and container state must be examined together.
3. Docker service names provide stable internal service discovery.
4. Nginx upstream errors can reveal backend availability and network problems.
5. Host port publishing conflicts are different from container-to-container networking failures.
6. Containers on different Docker networks may not resolve the same service names.
7. Restart policies handle some transient failures but cannot correct persistent application configuration errors.
8. Recovery must be verified through application-level requests.
9. Persistent database storage must be protected during troubleshooting.

## 12. M2-10 Conclusion

TITAN successfully completed five controlled container reliability incident simulations.

The investigation process covered application startup, database connectivity, reverse proxy communication, host port allocation, Docker networking and container restart behaviour.

Each failure was investigated using runtime or application evidence, its root cause was identified, and a corrective action was demonstrated.

The real TITAN API continued responding successfully after the simulations.

This runbook establishes a repeatable troubleshooting foundation for future TITAN deployment and operations work.