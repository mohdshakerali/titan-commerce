# Contributing to TITAN Commerce

Thank you for contributing to TITAN Commerce.

This document defines the development workflow, coding expectations and contribution standards for the project.

## 1. Development Principles

All contributions should prioritize:

- Maintainable and readable code.
- Security and reliability.
- Automated testing.
- Clear documentation.
- Small, reviewable changes.
- Cost-conscious infrastructure decisions.

## 2. Branching Strategy

The `main` branch represents the stable integration branch.

Create a separate branch for every feature, bug fix or documentation change.

Branch naming conventions:

| Type | Naming Convention | Example |
|---|---|---|
| Feature | feature/description | feature/customer-registration |
| Bug fix | fix/description | fix/order-validation |
| Documentation | docs/description | docs/update-architecture |
| Infrastructure | infra/description | infra/docker-compose |

Do not commit unfinished work directly to `main`.

## 3. Commit Message Convention

Use clear, descriptive commit messages.

Follow this format:

`type: short description`

Common types:

- `feat:` New functionality.
- `fix:` Bug fixes.
- `docs:` Documentation changes.
- `test:` Test additions or modifications.
- `refactor:` Code restructuring without changing behaviour.
- `build:` Build and dependency changes.
- `ci:` CI/CD pipeline changes.
- `infra:` Infrastructure changes.
- `chore:` Maintenance tasks.

Examples:

- `feat: add customer registration API`
- `fix: prevent duplicate order processing`
- `docs: document application architecture`
- `ci: add backend test workflow`

## 4. Issue Management

Every significant task should have a corresponding GitHub Issue.

Issues should include:

- Clear title and description.
- Appropriate labels.
- Acceptance criteria.
- Milestone assignment where applicable.

Work should be tracked using the Project TITAN engineering board.

## 5. Pull Request Workflow

1. Create a feature branch from the latest `main`.
2. Implement the change.
3. Run relevant tests and quality checks.
4. Commit and push the branch.
5. Open a Pull Request targeting `main`.
6. Link the related GitHub Issue.
7. Provide a summary and testing evidence.
8. Resolve review feedback.
9. Merge only after required checks pass.

## 6. Code Quality and Testing

Contributions should:

- Follow the established coding conventions.
- Include tests for new business logic.
- Avoid unnecessary dependencies.
- Handle errors appropriately.
- Avoid exposing sensitive information.
- Pass the applicable automated quality checks.

Testing and CI requirements will be expanded in later milestones.

## 7. Security Requirements

Never commit:

- Passwords or access tokens.
- AWS access keys or secret keys.
- Database credentials.
- Private keys or certificates.
- Real customer personal information.

Use environment variables and secure secret-management mechanisms.

All infrastructure changes must be reviewed for security and cost implications.

## 8. Documentation Requirements

Update relevant documentation when changes affect:

- Application architecture.
- Technology decisions.
- APIs and configuration.
- Infrastructure.
- Deployment procedures.
- Operational troubleshooting.

Significant architectural decisions must be recorded as ADRs.

## 9. Definition of Done

A contribution is considered complete when:

- Acceptance criteria are satisfied.
- Relevant tests pass.
- Documentation is updated where necessary.
- No secrets are committed.
- Required reviews and quality checks are completed.
- The change is merged into `main`.
- The related issue is closed.

## 10. Continuous Improvement

These guidelines may evolve as TITAN Commerce grows.

Changes to development practices should be documented and communicated through the repository.
