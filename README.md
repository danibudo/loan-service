> Note: this README.md file has been generated with Claude Code.
# loan-service

Loan lifecycle microservice for the **Library Management System** — a portfolio project demonstrating a production-style microservice architecture across multiple languages and frameworks.

> The full system includes an auth-service (TypeScript/Node.js), user-service (Kotlin/Spring Boot), catalog-service (Java/Spring Boot), loan-service (Kotlin/Spring Boot), notification-service (TypeScript/Node.js), and an API gateway (Spring Cloud Gateway). Each service owns its own database and communicates asynchronously via RabbitMQ or synchronously over REST.

## Overview

The loan-service manages the full lifecycle of a book loan — from a member's initial request through librarian approval, copy pickup, and return. It coordinates copy reservation and release with the catalog-service via a choreography-based saga: when a librarian approves a loan, this service publishes a reservation request and waits for the catalog-service to confirm or deny it asynchronously.

Role-based access is enforced at the service layer. The service trusts caller identity from headers injected by the API gateway (`X-User-Id`, `X-User-Role`) rather than validating JWTs directly. All loan endpoints require authentication — members can only view and manage their own loans, while librarians and super-admins have full access.

## Features

- **Loan request submission** — members submit loan requests for a title with a desired pickup window; the title's existence is validated synchronously against the catalog-service
- **Full status lifecycle** — `pending → awaiting_copy → approved → started → ended`, with `rejected` and `cancelled` as terminal branches
- **Copy reservation saga** — on librarian approval, publishes `copy_reservation_requested`; handles `copy_reserved` (transitions to `approved`, assigns `copy_id`) and `copy_reservation_failed` (returns to `pending` for retry)
- **Copy release saga** — on loan end or cancellation of an approved loan, publishes `copy_release_requested` and consumes the `copy_released` confirmation
- **Member ownership enforcement** — members can only view and cancel their own loans; attempting to access another member's loan returns `403`
- **Title validation with circuit breaker** — `POST /loans` calls the catalog-service synchronously via WebClient wrapped in a Resilience4j circuit breaker; returns `503` if the circuit is open
- **Due date reminder scheduler** — a daily job publishes `loan_due_reminder` events for all active loans due within a configurable lookahead window
- **Dead-letter queues** — every consumer queue has a corresponding DLQ; unprocessable messages are routed there without blocking the queue

## Tech Stack

- **Runtime:** Kotlin / JVM 21
- **Framework:** Spring Boot
- **Database:** PostgreSQL (Spring Data JPA, Flyway migrations)
- **Messaging:** RabbitMQ (Spring AMQP)
- **REST client:** Spring WebFlux `WebClient`
- **Circuit breaker:** Resilience4j (`spring-cloud-starter-circuitbreaker-resilience4j`)
- **Validation:** Jakarta Bean Validation
- **Scheduler:** Spring `@Scheduled`
- **Containerisation:** Docker (multi-stage Maven build, non-root user)

## API

All endpoints require `X-User-Id` (UUID) and `X-User-Role` (role string), injected by the API gateway after JWT validation.

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/loans` | `member` | Submit a loan request for a title |
| `GET` | `/loans` | all authenticated | Members see their own loans only; librarians/super-admins see all with optional filters |
| `GET` | `/loans/{id}` | all authenticated | Members can only fetch their own loan |
| `POST` | `/loans/{id}/approve` | `librarian`, `super-admin` | Approve a pending loan; triggers the copy reservation saga |
| `POST` | `/loans/{id}/reject` | `librarian`, `super-admin` | Reject a pending loan with a mandatory reason |
| `POST` | `/loans/{id}/start` | `librarian`, `super-admin` | Mark loan as started (member picked up the book); sets due date |
| `POST` | `/loans/{id}/end` | `librarian`, `super-admin` | Mark loan as ended (member returned the book); triggers copy release |
| `POST` | `/loans/{id}/cancel` | `member` | Cancel own pending or approved loan; triggers copy release if approved |
| `GET` | `/health` | — | Actuator health check |

`GET /loans` supports optional query parameters for librarians: `status`, `member_id`, `title_id`.

**Submit loan request**
```
POST /loans
Content-Type: application/json
X-User-Id: <uuid>
X-User-Role: member

{
  "title_id": "uuid",
  "desired_pickup_from": "2024-02-01",
  "desired_pickup_to": "2024-02-15"
}
```

**Reject loan**
```
POST /loans/{id}/reject
Content-Type: application/json
X-User-Id: <uuid>
X-User-Role: librarian

{ "reason": "No copies expected in the requested timeframe." }
```

All other action endpoints (`approve`, `start`, `end`, `cancel`) take no request body.

All response bodies use `snake_case` field names. The loan response includes `copy_id`, `rejection_reason`, and `due_date`, which are `null` until the relevant lifecycle stage is reached.

## Messaging

The service owns and publishes to the `loan-service.events` RabbitMQ exchange and consumes saga responses from `catalog-service.events`. Each queue has a corresponding dead-letter queue (DLQ) for messages that fail processing.

| Event published | Trigger | Description |
|---|---|---|
| `loan.loan_requested` | `POST /loans` | A member submitted a new loan request |
| `loan.loan_approved` | `catalog.copy_reserved` received | A copy was reserved and the loan is approved |
| `loan.loan_rejected` | `POST /loans/{id}/reject` | A librarian explicitly rejected the loan |
| `loan.loan_started` | `POST /loans/{id}/start` | The member picked up the book; due date is set |
| `loan.loan_ended` | `POST /loans/{id}/end` | The member returned the book |
| `loan.loan_cancelled` | `POST /loans/{id}/cancel` | The member cancelled their loan |
| `loan.loan_due_reminder` | Daily scheduler | A loan is due within the configured lookahead window |
| `loan.copy_reservation_requested` | `POST /loans/{id}/approve` | Saga trigger: asks catalog-service to reserve a copy |
| `loan.copy_release_requested` | `POST /loans/{id}/end` or `cancel` (when approved) | Saga trigger: asks catalog-service to release the copy |

| Event consumed | Action |
|---|---|
| `catalog.copy_reserved` | Verifies loan is in `awaiting_copy`; sets `copy_id`, transitions to `approved`, publishes `loan_approved` |
| `catalog.copy_reservation_failed` | Verifies loan is in `awaiting_copy`; transitions back to `pending` — no member notification; the librarian must retry or reject |
| `catalog.copy_released` | Informational confirmation; logged and acked — the loan is already in its terminal state |

The `copy_reserved` and `copy_reservation_failed` handlers are `@Transactional` — a publish failure rolls back the status change so the message is nacked and retried. Duplicate delivery is handled by checking the loan's current status before processing (idempotency guard).

## Running Locally

The included `docker-compose.yml` starts the full stack: PostgreSQL (with `user_service`, `auth_service`, `catalog_service`, and `loan_service` databases), RabbitMQ, the auth-service, user-service, catalog-service, and loan-service.

```bash
docker compose up --build
```

The auth-service, user-service, and catalog-service repositories must be present at the same directory level as this repository:

```
../auth-service
../user-service
../catalog-service
./               ← this repo
```

The loan-service will be available at `http://localhost:8082`. The RabbitMQ management UI is available at `http://localhost:15672` (guest / guest).

Database migrations run automatically on startup via Flyway.

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8082` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/loan_service` | JDBC URL for PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password |
| `SPRING_RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `SPRING_RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `SPRING_RABBITMQ_USERNAME` | `guest` | RabbitMQ user |
| `SPRING_RABBITMQ_PASSWORD` | `guest` | RabbitMQ password |
| `RABBITMQ_PREFETCH` | `10` | Per-consumer prefetch count |
| `CATALOG_SERVICE_URL` | `http://localhost:8081` | Base URL for catalog-service REST calls |
| `LOAN_DUE_DAYS` | `30` | Days from pickup to due date |
| `LOAN_REMINDER_DAYS_BEFORE` | `3` | Days before due date to send a reminder |