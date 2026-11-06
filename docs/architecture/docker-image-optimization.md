# TITAN Container Image Optimization

## 1. Objective

Reduce TITAN backend and frontend container image sizes while
preserving application functionality, non-root execution and
the existing multi-stage build architecture.

## 2. Baseline and Final Measurements

Image sizes were measured using Docker image inspection
(`.Size`), in bytes.

| Metric | Backend Before | Backend After | Frontend Before | Frontend After |
|---|---:|---:|---:|---:|
| Image size | 571.24 MB | 400.94 MB | 93.98 MB | 20.91 MB |
| Image layers | 9 | 8 | 10 | 9 |

### Optimization Results

- Backend: 170.30 MB saved (29.81% reduction).
- Frontend: 73.07 MB saved (77.75% reduction).
- Combined: 243.37 MB saved (36.58% reduction).

These are image-size measurements, not compressed registry
download sizes.

## 3. Backend Optimization

### Previous runtime

`eclipse-temurin:21-jre`

### Updated runtime

`eclipse-temurin:21-jre-alpine-3.24`

Changes:
- Adopted an Alpine-based Java 21 JRE runtime.
- Replaced Debian-compatible `useradd` with Alpine-compatible
  `adduser`.
- Preserved non-root execution using UID 10001.
- Preserved the existing JDK/Maven build stage.
- Continued copying only the packaged application JAR
  into the runtime stage.

Result:
- Image reduced from 571.24 MB to 400.94 MB.
- One fewer filesystem layer.

## 4. Frontend Optimization

### Previous runtime

`nginx:alpine`

### Updated runtime

`nginx:1.30.6-alpine3.24-slim`

Changes:
- Adopted a versioned Alpine Slim Nginx runtime image.
- Preserved the Node.js build stage.
- Continued copying only the generated Vite `dist` directory
  into the Nginx runtime.
- Preserved the existing Nginx configuration.

Result:
- Image reduced from 93.98 MB to 20.91 MB.
- One fewer filesystem layer.

## 5. Build Context and .dockerignore

Existing backend exclusions:

- `target/`
- `.git/`
- `*.log`
- `.env`

Existing frontend exclusions:

- `node_modules/`
- `dist/`
- `.git/`
- `*.log`
- `.env`

These exclusions prevent generated build output, local
dependencies, repository metadata, logs and local environment
files from entering the respective build contexts.

### Final Build Context Measurements

Measured through Docker BuildKit output after applying
the existing ignore rules:

| Service | Transferred Context |
|---|---:|
| Backend | 2.81 KB |
| Frontend | 828 bytes |

Historical pre-optimization build-context measurements were
not captured. Therefore, no before/after context reduction
is claimed.

The existing ignore rules were retained because they already
exclude the relevant generated and local files without
excluding required application inputs.

## 6. Docker Build Cache Review

The existing Dockerfiles already follow dependency-first
copy ordering.

### Backend

- Copies Maven wrapper and Maven configuration.
- Copies `pom.xml`.
- Resolves Maven dependencies.
- Copies application source.
- Packages the application.

This allows dependency resolution to remain cached when
only application source files change.

### Frontend

- Copies `package.json` and the lockfile.
- Runs `npm ci`.
- Copies application source.
- Runs the Vite production build.

This allows the dependency installation layer to remain
cached when application source changes without dependency
manifest changes.

No additional Dockerfile changes were necessary for cache
ordering.

## 7. Functional Verification

### Backend

- Backend image rebuilt successfully.
- Backend container recreated using the optimized image.
- Spring Boot started successfully on Java 21.
- PostgreSQL 17.11 connection established.
- Health endpoint returned `{"status":"UP"}` when requested
  from the frontend container over Docker networking.

### Frontend

- Frontend image rebuilt successfully.
- Frontend container recreated using the optimized image.
- TITAN frontend successfully loaded in the browser.

PostgreSQL and its persistent data volume were not modified
during these image optimization deployments.

## 8. Final Outcome

Both application images were successfully optimized and
deployed.

- Backend image reduction: 29.81%.
- Frontend image reduction: 77.75%.
- Combined application image reduction: 36.58%.
- One fewer filesystem layer in each final image.
- Existing multi-stage builds retained.
- Existing non-root backend execution retained.
- Application smoke checks passed.

## 9. Engineering Decisions

No unnecessary changes were made to application code or
existing .dockerignore rules.

The work focused on runtime base-image selection,
build-context hygiene, dependency caching and preserving
application functionality.