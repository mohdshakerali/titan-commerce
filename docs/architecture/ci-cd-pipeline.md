
# TITAN CI/CD Pipeline Engineering

## 1. Objective

Implement an automated Continuous Integration (CI) pipeline for TITAN Commerce using GitHub Actions.

The pipeline validates backend and frontend application changes and builds both Docker images before changes are accepted into the main branch.

The initial implementation focuses on CI. Container images are not published to an external registry, and no cloud infrastructure is deployed by this workflow.

## 2. CI Architecture

Workflow: `.github/workflows/titan-ci.yml`

Triggers:
- Pull requests targeting `main`.
- Pushes to `main`.
- Manual execution through `workflow_dispatch`.

Permissions:
- `contents: read`.

The workflow contains three jobs:

1. Backend Tests
2. Frontend Validation
3. Build Docker Images

The Docker build job depends on successful completion of both application validation jobs.

## 3. Backend Test Pipeline

Environment:
- Runner: GitHub-hosted Ubuntu.
- Java: 21 (Temurin).
- Build tool: Maven Wrapper.
- Database: PostgreSQL 17.

A temporary PostgreSQL service container is started for backend testing.

Database configuration:
- Database: `titan_commerce`
- Username: `titan_user`
- Password: CI-only test value.

Health check:

`pg_isready -U titan_user -d titan_commerce`

The workflow:
- Checks out the repository.
- Configures Java 21.
- Enables Maven dependency caching through `actions/setup-java`.
- Executes the Maven test lifecycle.

Command:

`./mvnw --batch-mode --no-transfer-progress test`

The backend tests validate application context startup, product persistence, customer registration, authentication, JWT handling and product catalogue endpoints.

Production credentials are not used in the CI environment.

## 4. Frontend Validation Pipeline

Environment:
- Runner: GitHub-hosted Ubuntu.
- Node.js: 22.
- Package manager: npm.
- Build tool: Vite.
- Language: TypeScript.

The workflow:
- Checks out the repository.
- Configures Node.js 22.
- Enables npm dependency caching through `actions/setup-node`.
- Installs dependencies using the lockfile.
- Runs ESLint.
- Creates a production frontend build.

Commands:

`npm ci`

`npm run lint`

`npm run build`

The production build includes TypeScript compilation and Vite bundling.

## 5. Docker Image Build Pipeline

Docker image builds execute only after backend tests and frontend validation pass.

Build tooling:
- Docker Buildx.
- Docker Build Push Action.
- GitHub Actions cache backend.

Two images are built:

### Backend

Build context: `./backend`

Dockerfile: `backend/Dockerfile`

Image name: `titan-commerce-backend`

### Frontend

Build context: `./frontend`

Dockerfile: `frontend/Dockerfile`

Image name: `titan-commerce-frontend`

The workflow uses separate BuildKit GitHub Actions cache scopes:

- `titan-backend`
- `titan-frontend`

Configuration:

`cache-from: type=gha`

`cache-to: type=gha,mode=max,ignore-error=true`

Image publishing is disabled:

`push: false`

Images are built and tagged on the hosted runner but are not published to Docker Hub or Amazon ECR.

## 6. Build Cache Strategy

TITAN uses multiple caching mechanisms.

### Maven dependency caching

The backend Dockerfile uses a BuildKit cache mount:

`/root/.m2`

This allows Maven dependency downloads to be retained across builds using the same persistent BuildKit builder.

GitHub Actions also configures Maven dependency caching through `setup-java` for the separate backend test job.

### npm dependency caching

The frontend Dockerfile uses:

`/root/.npm`

This caches npm package downloads in the BuildKit builder.

The frontend validation job separately uses npm caching through `setup-node`.

### Docker layer caching

Docker Buildx uses the GitHub Actions cache backend with separate scopes for the backend and frontend.

Important distinction:

GitHub Actions Docker layer caching and BuildKit cache mounts are separate mechanisms.

BuildKit cache-mount contents are not automatically preserved between newly created hosted builders merely because the `type=gha` layer cache is configured.

The current workflow does not implement a separate persistent cache-mount export/import mechanism.

## 7. Image Tagging and Traceability

Docker images use commit-specific tags.

Format:

`titan-commerce-backend:sha-<github.sha>`

`titan-commerce-frontend:sha-<github.sha>`

The workflow also attaches OCI metadata labels:

- `org.opencontainers.image.source`
- `org.opencontainers.image.revision`

These labels identify the source repository and Git commit associated with the image build.

This establishes traceability between source code revisions and generated container images.

Because `push: false` is configured, the images remain local to the GitHub-hosted build job and are not available in an external registry after the job finishes.

## 8. Secrets and Configuration Handling

TITAN separates build-time configuration from runtime application configuration.

The Docker images do not contain the following runtime values in their image environment configuration:

- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `TITAN_JWT_SECRET`

The local `.env` file is excluded from Git tracking and Docker build contexts.

Docker Compose injects database credentials and the JWT secret into the application containers at runtime.

GitHub Actions uses dedicated non-production test values for its temporary PostgreSQL service and backend test job.

No production credentials are embedded in Docker build arguments.

Future cloud deployment will require integration with a proper secret-management service.

## 9. CI Verification Results

The first pull-request execution completed successfully.

Verified jobs:
- Backend Tests: Passed.
- Frontend Validation: Passed.
- Build Docker Images: Passed.

A subsequent push-triggered execution also completed successfully after commit-based image tagging was introduced.

Verified commit:

`6397b57e2dc55e7d6fb8ae201f23abe582c6c536`

Verified jobs:
- Backend Tests: Passed.
- Frontend Validation: Passed.
- Build Docker Images: Passed.

The Docker build logs confirmed that both images were built with the full commit SHA and the expected OCI source and revision labels.

CI run:

`https://github.com/mohdshakerali/titan-commerce/actions/runs/42721905880`

## 10. Local BuildKit Cache Validation

Maven and npm BuildKit cache mounts were implemented in the application Dockerfiles.

The application images were successfully built and deployed locally after the changes.

A subsequent local no-cache rebuild experiment completed the backend build, but the frontend remained at the npm dependency installation step.

The repeated-build experiment was therefore inconclusive.

No quantitative claim of local build-time improvement is made.

The GitHub Actions workflow successfully validates both application builds independently.

## 11. Current Architecture and Future Integration

Current CI flow:

Source code
→ GitHub Actions
→ Backend tests
→ Frontend validation
→ Docker image builds
→ Commit-specific image identification

Future planned integration:

GitHub Actions
→ Amazon ECR
→ AWS deployment environment
→ Application monitoring and operational validation

Potential future cloud services include:
- Amazon ECR for container image storage.
- AWS IAM for deployment permissions.
- AWS ECS or EKS for container orchestration.
- AWS Secrets Manager for application secrets.
- Amazon CloudWatch for monitoring.

These services are planned future integration points and are not currently provisioned by this CI workflow.

## 12. Engineering Outcome

TITAN now has an automated CI pipeline that:

- Tests the backend against PostgreSQL.
- Validates the React frontend.
- Builds backend and frontend Docker images.
- Uses dependency and Docker layer caching mechanisms.
- Associates generated images with their source Git commits.
- Applies OCI image metadata labels.
- Keeps production credentials outside image build configuration.
- Runs automatically for pull requests and pushes to `main`.

The current implementation establishes the foundation for future container image publishing and AWS deployment.
