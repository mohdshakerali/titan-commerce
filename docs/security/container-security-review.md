# TITAN Container Security Review and Remediation

## Scope

This review covers the local TITAN PostgreSQL, Spring Boot backend, and Nginx frontend containers and their built images. It records the runtime controls applied, Trivy results observed on 2026-11-10, remediation decisions, and remaining limitations.

This is a project-level hardening exercise, not a security certification or a claim that all images are vulnerability-free.

## 1. Runtime identity and privilege review

The running processes were inspected rather than relying only on the image defaults.

| Service/process | Observed identity | Finding |
|---|---|---|
| Spring Boot Java process | UID 10001 (`appuser`) | Dedicated non-root runtime user |
| Nginx master | UID 0 (`root`) | Root master retained for the official image startup/bind model |
| Nginx workers | UID 101 (`nginx`) | Workers run unprivileged |
| PostgreSQL processes | UID 999 (`postgres`) | Database processes run as non-root |

All three containers were confirmed to have `Privileged=false`.

The empty Docker `Config.User` for Nginx and PostgreSQL does not mean their application processes run as root. The Nginx and PostgreSQL process identities were checked inside the containers and via process listings.

## 2. Runtime hardening applied

The Compose configuration applies:

- `security_opt: [no-new-privileges:true]` to all three services.
- `cap_drop: [ALL]` to the backend only.

The backend already runs as UID 10001 and listens on container port 8080, so it has no identified need for Linux capabilities in this application configuration.

The backend process `/proc/1/status` was inspected after applying the controls:

| Field | Observed value |
|---|---|
| `CapInh` | `0000000000000000` |
| `CapPrm` | `0000000000000000` |
| `CapEff` | `0000000000000000` |
| `CapBnd` | `0000000000000000` |
| `NoNewPrivs` | `1` |

Docker inspection confirmed `cap_drop=["ALL"]` and `security-opt=["no-new-privileges:true"]` for the backend, and `no-new-privileges:true` for Nginx and PostgreSQL.

Nginx and PostgreSQL retain Docker's default capability set at this stage. The official Nginx master/worker startup model and PostgreSQL entrypoint initialization/privilege-switching behaviour have not been altered. Further capability drops should be introduced only with a service-specific compatibility test.

## 3. Read-only root filesystem assessment

A read-only root filesystem was evaluated but **not enabled** in this change.

Reasons:
- PostgreSQL must write database files to the persistent volume mounted at `/var/lib/postgresql/data`; it also uses runtime paths during startup and operation.
- Nginx requires writable runtime/cache/temp paths depending on its configuration and image entrypoint.
- Spring Boot/Tomcat may use temporary paths such as `/tmp` for runtime and request-processing operations.

A safe read-only deployment requires explicitly mapping and testing each required writable path (for example, dedicated volumes or tmpfs mounts) and then exercising initialization, normal traffic, restart and recovery. That compatibility work was not performed in this M2-07 pass. Leaving the root filesystem writable is a documented limitation, not an assertion that it is the preferred final production setting.

## 4. Secret and configuration handling

The Compose configuration injects `POSTGRES_PASSWORD` and `TITAN_JWT_SECRET` at runtime from the local `.env` file. The Dockerfiles do not define these values as build arguments or copy the `.env` file into an image. The repository `.gitignore` excludes `.env`.

The earlier M2-03 image review found that the backend image configuration did not contain `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD`, or `TITAN_JWT_SECRET`. CI uses test-only values, not production credentials.

Do not print container environments or include actual secret values in logs, issues, screenshots, or documentation. Runtime environment variables are still visible to principals with sufficient container inspection/exec access; a production deployment should use an approved secret-management mechanism.

## 5. Image vulnerability scan and remediation

Scanner: Trivy `0.76.2`, vulnerability scanner, run against local Docker images on 2026-11-10. The scan database reported OS package and application/binary findings separately. A package can be associated with multiple CVEs, and the same advisory can affect multiple packages; counts below are Trivy finding counts, not counts of unique exploitable attack paths.

### Backend image

Initial scan of `titan-commerce-backend`:
- Alpine 3.24.4 OS layer: 1 MEDIUM finding, `zlib` package `1.3.2-r1`, CVE-2026-4738; Trivy listed `1.4-r0` as fixed.
- `app/app.jar`: 0 findings.
- No HIGH or CRITICAL findings in this scan.

Remediation:
- Added `RUN apk upgrade --no-cache zlib` to the backend runtime stage.
- Rebuilt `titan-commerce-backend`.
- Rescanned the rebuilt image: 0 Alpine findings and 0 JAR findings.
- Deployed the rebuilt backend image without rebuilding a second time.
- Postman `GET http://localhost/api/products` returned `200 OK` after deployment.

### Frontend image

Initial scan of `titan-commerce-frontend`:
- Alpine 3.24.4 OS layer: 1 MEDIUM finding, `zlib` package `1.3.2-r1`, CVE-2026-4738; Trivy listed `1.4-r0` as fixed.
- No language-specific files were detected by this scan.

Remediation:
- Added `RUN apk upgrade --no-cache zlib` to the frontend runtime stage.
- Rebuilt `titan-commerce-frontend`.
- Rescanned the rebuilt image: 0 findings.
- Deployed the rebuilt frontend image.
- Postman `GET http://localhost/api/products` returned `200 OK` after deployment.

### PostgreSQL image

Initial scan of `postgres:17` (Debian 13.7):
- Debian OS target: 357 findings in the full report.
- Bundled `/usr/local/bin/gosu` Go binary target: 61 findings.
- High/Critical filtered report: Debian 84 HIGH and 1 CRITICAL; gosu 25 HIGH and 1 CRITICAL.

A newer local `postgres:17` image was pulled. Its reported PostgreSQL version remained 17.11 (Debian 17.11-1.pgdg13+2), matching the running image's PostgreSQL version. The refreshed image's High/Critical scan reported:
- Debian OS target: 82 HIGH and 1 CRITICAL (83 total in that filtered target).
- `gosu` binary target: 25 HIGH and 1 CRITICAL (26 total in that filtered target).

The refreshed image therefore reduced two Debian HIGH findings in this scan, but the reported CRITICAL and gosu findings remained. The refreshed PostgreSQL image was then applied to the Compose PostgreSQL service. Postman `GET http://localhost/api/products` returned `200 OK` after the service update.

Key items from the filtered report included:
- `libxml2`, CVE-2026-6653, reported CRITICAL by Trivy; the report did not list a fixed package version.
- Bundled `gosu` binary, Go standard library reported as `v1.24.6`, CVE-2025-68121, reported CRITICAL; Trivy listed fixed Go versions including `1.24.13`.
- Other findings were marked `affected` without a fixed version or `fix_deferred` (including Perl CVE-2026-9538).

Trivy emitted a warning that it selected severities from other vendors for some vulnerabilities. Severity can therefore differ from the distribution/vendor assessment. Findings must be reviewed against the relevant upstream and distribution advisories before assigning exploitability or remediation priority.

Remediation decision:
- Do not run an unreviewed broad `apt upgrade` inside the running database container or maintain a private derivative database image solely to suppress scanner counts.
- Do not replace the official PostgreSQL image with an unrelated base distribution as a shortcut.
- Refresh the official `postgres:17` image and record remaining findings with their package, advisory, status and fix availability.
- Treat the remaining PostgreSQL findings as unresolved/upstream findings requiring periodic reassessment, not as fixed or clean. A later upstream image refresh should trigger another scan.

## 6. Functional verification after hardening

After applying the Compose security settings and deploying the rebuilt/updated images:
- PostgreSQL container reported healthy.
- `pg_isready` returned `accepting connections`.
- Backend actuator endpoint returned `status: UP` with liveness/readiness groups.
- Postman `GET http://localhost/api/products` returned `200 OK` after backend remediation, after frontend remediation, and after the PostgreSQL image refresh.

The existing Compose network segmentation and external PostgreSQL data volume were retained.

## 7. Accepted limitations and next priorities

| Area | Current decision | Follow-up priority |
|---|---|---|
| Backend user/capabilities | Non-root UID 10001; all capabilities dropped and verified zero | Keep controls in CI/review |
| Nginx user model | Root master, non-root workers | Evaluate official unprivileged Nginx image/configuration separately |
| PostgreSQL process user | UID 999 non-root | Keep official entrypoint model; assess further hardening with compatibility tests |
| Privilege escalation | Disabled for all Compose services | Keep enabled |
| Read-only root filesystems | Not enabled; writable paths not fully mapped/tested | Test service-by-service with explicit tmpfs/volume mounts |
| Backend/frontend image vulnerabilities | CVE-2026-4738 remediated; final scan clean | Rescan on rebuild/base-image refresh |
| PostgreSQL image vulnerabilities | Remaining HIGH/CRITICAL findings in Debian packages and gosu | Monitor upstream fixes and periodically rescan |
| Secrets | Runtime injection; `.env` ignored; no secrets in Dockerfile build args/copies | Move to approved secret manager for production |
| Image provenance | OCI source/revision labels already added in CI | Consider digest pinning and signing in a later supply-chain milestone |

A clean Trivy report is not a guarantee of security; it means that the scanner's current vulnerability database did not report findings for the scanned image. Conversely, a finding is not proof of exploitability in the deployed TITAN configuration. Record severity, affected component, fix status, exposure and compensating controls.

## 8. Commands used for repeat scanning

The local scan used Trivy in a temporary Docker container with the Docker socket and a persistent cache volume:

```powershell
docker run --rm `
  -v /var/run/docker.sock:/var/run/docker.sock `
  -v trivy-cache:/root/.cache/ `
  aquasec/trivy:0.76.2 image --scanners vuln titan-commerce-backend
```

For PostgreSQL, use the same command with `--severity CRITICAL,HIGH postgres:17` to focus triage on the highest reported severities. Review complete reports before making remediation decisions.

## Scope boundary

This is local Compose container hardening. Kubernetes `securityContext`, Pod Security Standards, admission policies, image signing/verification, automated vulnerability gates, and production secret management are intentionally reserved for later deployment/supply-chain work.
