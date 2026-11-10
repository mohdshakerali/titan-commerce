# TITAN Commerce — M2 Engineering Acceptance and Operational Handover

- **Milestone:** M2 — Docker Engineering
- **Document status:** Prepared for final acceptance; operator sign-off pending
- **Prepared:** 10 November 2026
- **Repository:** `mohdshakerali/titan-commerce`
- **Target environment:** Local Docker Compose deployment on Windows 11, Docker Desktop and WSL2
- **Cloud cost:** No paid cloud services are required for M2

---

## 1. Purpose and acceptance boundary

This document consolidates the Docker engineering baseline delivered across M2 and identifies the evidence, operating procedures, known limitations and follow-up work to hand over to the next milestone.

M2 covers the local containerized TITAN Commerce application:

- React frontend served by Nginx
- Spring Boot backend API
- PostgreSQL 17 database
- Docker Compose orchestration, networking and persistent storage
- Image build optimization, CI validation, security hardening, registry distribution and operational recovery documentation

This document does not claim that the final M2 acceptance issue is closed. Issue #33 remains open until the final integrated review is completed and the updated documentation is committed, merged and checked.

## 2. Delivered runtime baseline

### 2.1 Services and exposure

| Service | Runtime | Container port | Host exposure |
|---|---|---:|---|
| Frontend | React application served by Nginx | 80 | `80:80` |
| Backend | Spring Boot API | 8080 | Not published; exposed to the Compose network |
| Database | PostgreSQL 17 | 5432 | Not published; internal container communication |

The normal customer request path is:

1. The browser connects to `http://localhost`.
2. Nginx serves the frontend.
3. Nginx forwards API requests to the backend service.
4. The backend connects to PostgreSQL.
5. The backend returns the API response to the frontend.

### 2.2 Network segmentation

The Compose configuration uses two bridge networks:

| Network | Connected services | Purpose |
|---|---|---|
| `frontend` | Frontend and backend | Frontend-to-API communication |
| `backend_db` | Backend and PostgreSQL | Private database communication; configured as an internal network |

The backend is attached to both networks. PostgreSQL is not directly published to the host. The database connection uses the Compose service name `postgres`, not a fixed container IP.

### 2.3 Persistence

PostgreSQL data is stored in the external named Docker volume:

`titan-postgres-data`

Use the actual volume name `titan-postgres-data` when running Docker commands. The volume is external to the Compose project definition so that ordinary container recreation does not remove the database volume.

**Data-protection warning:** Do not use `docker compose down -v`, `docker volume rm` on the production-data volume, or broad Docker prune commands as routine cleanup. These actions can destroy persistent data.

### 2.4 Startup health checks

| Service | Compose health check | Configuration |
|---|---|---|
| PostgreSQL | `pg_isready` | 10-second interval, 5-second timeout, 5 retries |
| Backend | Spring Boot Actuator `/actuator/health` using `wget` | 15-second interval, 5-second timeout, 3 retries, 30-second start period |
| Frontend | No independent Compose health check | Not configured |

Startup dependencies are configured as follows:

- Backend waits for PostgreSQL with `condition: service_healthy`.
- Frontend waits for the backend with `condition: service_healthy`.

These conditions gate initial startup. They do not continuously monitor dependencies or automatically restart a dependent service when a dependency becomes unhealthy after startup.

## 3. M2 implementation register

The implementation issues and pull requests below were reviewed in the repository. All listed pull requests are merged.

| M2 issue | Workstream | Merged pull request(s) | Status |
|---|---|---|---|
| #23 | Runtime architecture | #34 | Merged |
| #24 | Image layers and build-context optimization | #35, #36, #37 | Merged |
| #25 | Build pipelines and CI | #38, #39, #40 | Merged |
| #26 | Container operations runbook | #41 | Merged |
| #27 | Container network segmentation | #42 | Merged |
| #28 | PostgreSQL persistence, backup and restore | #43 | Merged |
| #29 | Container security hardening | #44 | Merged |
| #30 | Docker Compose deployment configuration | #45 | Merged |
| #31 | Container image publishing and retrieval | #46 | Merged |
| #32 | Container reliability incidents | #47 | Merged |
| #33 | Final engineering acceptance and handover | — | Open; final sign-off pending |

Pull request range #34–#47 contains 14 merged PRs. The final acceptance issue is #33.

## 4. Operational documentation

The following repository documents form the M2 operating reference:

| Document | Purpose |
|---|---|
| `README.md` | Project overview, current project status and entry points |
| `docs/architecture/docker-runtime.md` | Observed Docker runtime architecture, service topology, networks and health checks |
| `docs/operations/container-operations-runbook.md` | Routine container inspection and operational troubleshooting |
| `docs/operations/compose-deployment-configuration.md` | Compose configuration, environment setup, startup dependencies and deployment considerations |
| `docs/operations/postgres-backup-restore.md` | PostgreSQL persistence, local backup and tested restore procedure |
| `docs/security/container-security-review.md` | Container security controls, scan findings and deferred hardening decisions |
| `docs/operations/container-image-publishing.md` | GHCR tagging, authentication, publishing and retrieval workflow |
| `docs/operations/container-reliability-runbook.md` | Simulated container incidents, observed symptoms and recovery procedures |

The CI architecture documentation delivered under M2-03 should also be retained as the reference for the repository's automated build and validation workflow.

## 5. Build, CI and image distribution

### 5.1 Build and CI

M2 delivered Docker image build improvements and a GitHub Actions validation pipeline.

The implementation includes:

- Backend and frontend runtime image optimization.
- BuildKit dependency cache mounts.
- Docker build and validation workflow automation.
- Documentation describing the CI pipeline and image optimization decisions.

The final acceptance operator should confirm that the latest branch changes pass the repository's required CI checks after the documentation PR is opened.

### 5.2 Published images

The M2-09 publishing exercise used the following image names and tags:

- `ghcr.io/mohdshakerali/titan-commerce-backend:48ebf84`
- `ghcr.io/mohdshakerali/titan-commerce-backend:latest`
- `ghcr.io/mohdshakerali/titan-commerce-frontend:48ebf84`
- `ghcr.io/mohdshakerali/titan-commerce-frontend:latest`

The tagged images were pushed to GHCR and pulled back successfully during the publishing exercise.

Recorded image digests from that exercise:

| Image | Digest |
|---|---|
| Backend | `sha256:7e1cf4e082012f745170f58357971c338130b050cc83f75e19959f701ea7b01d` |
| Frontend | `sha256:9cd42b6521b7cfe3b4b2d6e663f01f5bd4ae2ced679475fe82922b01de37e3b7` |

These digests are historical evidence from the M2-09 publishing exercise. They should not be treated as proof that a future rebuild produces the same digest.

## 6. Validation and operational evidence

The following evidence was recorded during the M2 implementation work. The final acceptance pass should confirm that the integrated stack still behaves as documented after the current documentation changes are committed.

| Area | Recorded evidence |
|---|---|
| Compose configuration | `docker compose config -q` completed successfully during M2-08 |
| Stack startup | PostgreSQL and backend reached healthy status; frontend started after the backend health condition was satisfied |
| Frontend access | Storefront was exercised through the published host HTTP port |
| API smoke test | Postman request `GET http://localhost/api/products` returned `200 OK` |
| Network segmentation | Frontend, backend and database network paths were configured and exercised |
| Database persistence | Persistence and recovery procedures were delivered and tested under M2-06 |
| Image distribution | Backend and frontend images were pushed to GHCR and pulled back successfully |
| Incident recovery | Five container incident scenarios were simulated and documented |
| Post-incident API | Postman `GET http://localhost/api/products` returned `200 OK` after the incident simulations |

### 6.1 Incident scenarios exercised

The M2-10 reliability runbook records these five scenarios:

1. Backend startup failure caused by an invalid application JAR path.
2. Database connectivity failure caused by an invalid PostgreSQL hostname in a temporary backend run.
3. Reverse-proxy failure while the backend container was stopped, producing an HTTP 502 response; service recovery restored API access.
4. Host port conflict when a second frontend attempted to bind port 80; a temporary alternate port and the correct Compose network were used for the recovery test.
5. Restart loop caused by an invalid application JAR, with the configured restart policy reaching its retry limit.

The scenarios were run as controlled tests. Temporary test containers were removed after use.

## 7. Container security posture

M2 introduced container hardening and documented the remaining image findings.

### 7.1 Implemented controls

- `no-new-privileges:true` is configured for all three Compose services.
- The backend drops all Linux capabilities with `cap_drop: [ALL]`.
- The observed backend process runs as UID `10001`.
- PostgreSQL runs as UID `999`.
- Nginx worker processes run as UID `101`; the Nginx master process runs as UID `0`.
- Backend and frontend image findings related to zlib CVE-2026-4738 were addressed by upgrading zlib in the runtime image builds; subsequent scans of those images returned zero findings in the recorded exercise.

### 7.2 Outstanding findings and deferred controls

- The official PostgreSQL `postgres:17` image retained upstream Debian-package and `gosu` vulnerability findings in the recorded Trivy scan, including critical-severity findings. These were documented rather than hidden or patched with an arbitrary in-container package upgrade.
- Read-only root filesystems were evaluated but deferred because application write paths had not been fully identified and tested.
- The frontend does not have an independent Compose health check.
- Compose health-based dependency conditions apply to startup ordering; they are not a continuous health orchestration mechanism.

Consult `docs/security/container-security-review.md` for the recorded scan evidence and the rationale for deferred controls.

## 8. Known limitations and follow-up work

M2 establishes a local Docker engineering baseline. It is not a production high-availability deployment.

| Area | Current limitation | Follow-up direction |
|---|---|---|
| PostgreSQL image findings | Residual upstream package findings remain in the official `postgres:17` image scan | Reassess when upstream image/package updates are available; rescan before release |
| Secrets | Local Compose uses a `.env` file | Use an approved secrets manager or secure deployment secret mechanism in a production environment |
| Backups | Local backup and restore procedure is documented | Add scheduled backups, retention and regular recovery testing for a production deployment |
| Transport security | Local storefront is exposed over HTTP | Introduce TLS and controlled ingress before external production exposure |
| Resource limits | No arbitrary container resource limits were imposed | Set limits using workload measurements and capacity planning |
| Frontend health | No independent frontend health check is configured | Consider an application-level or proxy-level health check if operational monitoring requires it |
| Dependency recovery | Compose health conditions gate startup but do not provide continuous recovery | Add suitable monitoring and recovery orchestration for the target deployment environment |
| Root filesystem | Read-only root filesystem was deferred | Identify and test all writable paths before enabling it |
| Deployment platform | M2 uses local Docker Compose | Evaluate the next infrastructure platform and deployment model in later milestones |

No paid cloud infrastructure is needed to complete or accept M2.

## 9. Final M2-11 acceptance checklist

The implementation work below has been delivered through M2-01 to M2-10. Complete the final integrated sign-off after the current documentation updates are included in the feature branch.

### 9.1 Repository and documentation

- [x] Review M2 implementation PRs #34–#47; all are merged.
- [ ] Confirm the updated README accurately describes the current project.
- [ ] Confirm `docs/architecture/docker-runtime.md` reflects the two-network architecture and actual health checks.
- [ ] Confirm `docs/operations/container-operations-runbook.md` no longer states that the backend has no Compose health check.
- [ ] Add this acceptance and handover document to the repository.
- [ ] Confirm known limitations and future work are recorded.

### 9.2 Integrated local acceptance

- [ ] Build the backend and frontend images from the current repository state.
- [ ] Confirm the Compose configuration is valid.
- [ ] Start the complete stack using the documented procedure.
- [ ] Confirm PostgreSQL and backend health status and service dependencies.
- [ ] Open `http://localhost` and confirm storefront access.
- [ ] Run Postman `GET http://localhost/api/products` and confirm `200 OK`.
- [ ] Confirm the frontend-to-backend and backend-to-database paths work.
- [ ] Confirm PostgreSQL data survives ordinary container recreation.
- [ ] Confirm the documented backup and restore procedure is available and its evidence is retained.
- [ ] Review the security findings and documented residual risks.
- [ ] Confirm the incident runbook and image-publishing documentation are present.

**Important:** Do not use `docker compose down -v` during acceptance. Preserve the external `titan-postgres-data` volume.

### 9.3 Pull request and closure

- [ ] Commit the README, architecture, operations runbook and handover document updates on `feature/m2-engineering-handover`.
- [ ] Push the branch and open a pull request against `main`.
- [ ] Confirm required CI checks pass.
- [ ] Review and merge the pull request using the repository's established merge workflow.
- [ ] Pull the merged `main` branch locally.
- [ ] Close Issue #33 only after the evidence and documentation have been reviewed.

## 10. Handover to M3

M2 provides the container engineering baseline for subsequent infrastructure and automation work.

The next milestone can build on:

- Docker runtime and container lifecycle knowledge.
- Container networking and service discovery.
- Image build optimization and registry distribution.
- CI validation and operational troubleshooting.
- Persistent data handling, backup and recovery.
- Container security controls and vulnerability review.

M3 can progress into Linux administration, Bash scripting and Ansible automation while continuing to use the local TITAN environment. This keeps the learning and project workflow cost-conscious and avoids requiring paid cloud infrastructure for the next stage's initial work.

---

## 11. Acceptance status

**M2 implementation:** Issues #23–#32 and PRs #34–#47 are complete and merged.

**M2 final acceptance:** Pending completion of the integrated checklist in Section 9, merge of the final handover documentation PR, and closure of Issue #33.

**Operational handover:** This document records the delivered baseline, supporting runbooks, validation evidence, known limitations and follow-up direction for M3.
