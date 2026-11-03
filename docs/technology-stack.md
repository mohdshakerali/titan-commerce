# TITAN Commerce — Technology Stack

**Document Version:** 1.0  
**Project:** Project TITAN  
**Status:** Approved — Initial Baseline

## 1. Architecture Overview

TITAN Commerce will initially use a modular monolith architecture with a separately runnable background worker.

The application will be developed as a single codebase with clearly separated business modules. The API and worker will be independently executable and deployable.

This approach provides maintainability without introducing premature microservice complexity.

## 2. Selected Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | React | Customer and admin interfaces |
| Backend | Java Spring Boot | REST APIs and business logic |
| Database | PostgreSQL | Persistent relational data |
| Background jobs | PostgreSQL-backed job queue | Asynchronous order processing |
| Backend testing | JUnit 5, Mockito | Unit and integration testing |
| Frontend testing | Vitest, React Testing Library | Component and frontend testing |
| Version control | Git and GitHub | Source control and collaboration |
| Containerization | Docker | Application packaging |
| Local orchestration | Docker Compose | Local multi-container environment |

## 3. Application Architecture

The initial application will contain the following logical modules:

- Authentication and user management
- Product catalogue
- Shopping cart
- Order management
- Background order processing
- Administration

The backend API and background worker will share the application codebase while running as separate processes.

## 4. Background Processing Strategy

The initial implementation will use PostgreSQL-backed jobs.

The job-processing design must support:

- Safe retries
- Idempotent processing
- Failure handling
- Clear job status tracking

RabbitMQ will be evaluated and introduced in a later engineering iteration to demonstrate message-driven architecture.

## 5. Database Strategy

PostgreSQL will store:

- Customer accounts
- Product information
- Shopping carts
- Orders
- Order-processing jobs

Relational transactions and constraints will protect important business data.

## 6. Testing Strategy

Backend:
- JUnit 5
- Mockito
- Spring Boot Test

Frontend:
- Vitest
- React Testing Library

Testing will cover business logic, API behaviour, validation and important user workflows.

## 7. Architecture and Technology Decisions

The initial implementation deliberately avoids:

- Premature microservices
- Unnecessary infrastructure dependencies
- Real payment integrations
- Unnecessary managed cloud services during local development

Technology choices may evolve through documented Architecture Decision Records (ADRs).

## 8. Development and Infrastructure Strategy

Development will begin locally using Windows and the available development environment.

Docker and Docker Compose will provide a reproducible local environment.

AWS infrastructure, Terraform, Kubernetes, CI/CD, security scanning and observability will be introduced progressively according to the Project TITAN roadmap.

Cloud resources must be evaluated for cost and credit eligibility before deployment.

## 9. Future Evaluation

The following technologies will be evaluated in later milestones:

- RabbitMQ
- Ansible
- Terraform
- Kubernetes
- Helm and Kustomize
- GitHub Actions
- Argo CD
- Prometheus and Grafana
- AWS managed services

These are planned engineering extensions, not dependencies required for the initial application.
