# ADR-003: Database-backed Job Queue Initially

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

TITAN Commerce needs asynchronous order processing.

The API should be able to accept an order and record the corresponding processing task without requiring the entire order workflow to finish during the customer request.

A separate background worker will retrieve and process pending jobs.

## Decision

The initial implementation will use PostgreSQL as a database-backed job queue.

Order creation and job creation must be handled transactionally to avoid creating an order without its corresponding processing job.

The job-processing implementation must support:

- Safe retries.
- Idempotent execution.
- Job status tracking.
- Failure handling.
- Protection against duplicate processing.

RabbitMQ will be evaluated as a future messaging integration.

## Alternatives Considered

1. **RabbitMQ:** Dedicated message broker with queueing and delivery mechanisms.
2. **Amazon SQS:** Managed cloud message queue.
3. **Database-backed queue:** Uses PostgreSQL to persist and coordinate background jobs.

## Consequences

### Positive

- No additional message-broker service during initial development.
- Lower local resource consumption.
- Simpler initial infrastructure.
- Transactional coordination between order and job creation.

### Negative

- Application must implement job claiming, retry and failure management.
- Polling can introduce database overhead.
- Database-backed queues may become less suitable as throughput grows.

## Future Considerations

RabbitMQ will be evaluated when the core application is stable.

The future implementation will compare message delivery, retries, dead-letter handling, operational complexity and resource consumption.

Amazon SQS may be considered in an AWS-specific extension.
