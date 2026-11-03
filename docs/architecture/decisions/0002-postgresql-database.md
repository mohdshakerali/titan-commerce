# ADR-002: PostgreSQL as Primary Database

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

TITAN Commerce needs persistent storage for customer accounts, products, shopping carts, orders and background processing jobs.

Order processing requires reliable transactions and consistent relationships between business entities.

## Decision

PostgreSQL will be the primary relational database for TITAN Commerce.

## Alternatives Considered

1. **MySQL:** A widely used relational database.
2. **MongoDB:** A document-oriented NoSQL database.
3. **PostgreSQL:** An open-source relational database with strong transactional capabilities.

## Consequences

### Positive

- ACID transaction support.
- Foreign keys and relational constraints.
- Suitable for order and customer data.
- Supports complex queries and indexing.
- Available for local development without managed cloud infrastructure.

### Negative

- Requires database schema design and migrations.
- Relational data modelling requires careful planning.
- Database availability becomes important for application and worker operations.

## Future Considerations

Database performance, indexing, backup, recovery and migration strategies will be addressed in later milestones.
