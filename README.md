# GreenGrub Donation Service

The Donation Service manages the full lifecycle of food donation listings — creation, discovery, claiming, and cancellation. It is one microservice in the GreenGrub platform that connects food donors with recipients to reduce food wastage.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Local Development Setup](#local-development-setup)
3. [K8s / Cloud Deployment](#ks--cloud-deployment)
4. [Architecture Overview](#architecture-overview)
5. [Domain Model (ERD Summary)](#domain-model-erd-summary)
6. [REST API](#rest-api)
7. [gRPC API](#grpc-api)
8. [Kafka — Event Contract](#kafka--event-contract)
9. [gRPC Clients](#grpc-clients)
10. [Flyway Migrations](#flyway-migrations)
11. [Resilience Patterns](#resilience-patterns)
12. [Exception Handling](#exception-handling)
13. [CORS Configuration](#cors-configuration)
14. [Project Structure](#project-structure)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.0 |
| Build | Maven (use `settings.xml` at project root — see below) |
| Database | PostgreSQL 15 (local Docker) / GCP Cloud SQL (k8s) |
| ORM | Spring Data JPA + Hibernate |
| Schema migrations | Flyway |
| Messaging | Apache Kafka |
| Inter-service RPC | gRPC (proto-contracts shared library) |
| Resilience | Resilience4j 2.2.0 |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Observability | Spring Actuator + Micrometer Prometheus |

---

## Local Development Setup

### Prerequisites

- Java 21
- Maven 3.8+
- Docker & Docker Compose
- GitHub Personal Access Token with `read:packages` scope (for proto-contracts dependency)

### 1. Set environment variables for GitHub Packages

```bash
export GITHUB_USERNAME=your-github-username
export GITHUB_TOKEN=your-github-token
```

### 2. Start local infrastructure

```bash
# From /Users/I528797/Desktop/greengrub/services/
docker-compose -f docker-compose.local.yml up -d
```

This starts:
- PostgreSQL 15 on `localhost:5432` (database: `greengrub`, user: `postgres`, password: `postgres`)
- Kafka on `localhost:9002`
- Redis, MongoDB, Adminer, Kafka UI, Maildev

### 3. Build and run

```bash
# Always use settings.xml — your .m2 mirrors the org repo and cannot resolve proto-contracts
mvn clean install -DskipTests -s settings.xml

mvn spring-boot:run -Dspring-boot.run.profiles=local -s settings.xml
```

The service starts on:
- REST: `http://localhost:8083`
- gRPC: `localhost:9093`
- Swagger UI: `http://localhost:8083/swagger-ui/index.html`
- Actuator health: `http://localhost:8083/actuator/health`

---

## K8s / Cloud Deployment

### Required Environment Variables

| Variable | Description | Default |
|---|---|---|
| `CLOUD_SQL_INSTANCE` | GCP Cloud SQL connection name (`project:region:instance`) | — (required) |
| `DONATION_DATABASE_NAME` | Cloud SQL database name | `donations` |
| `DB_USERNAME` | Cloud SQL user | — (required) |
| `DB_PASSWORD` | Cloud SQL password | — (required) |
| `DONATION_GRPC_SERVER_PORT` | gRPC server port | `9093` |
| `FOOD_SERVICE_HOST` | food-service k8s DNS hostname | `food-service` |
| `FOOD_GRPC_SERVICE_PORT` | food-service gRPC port | `9091` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `kafka:9092` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins (empty = no CORS) | `` |

Activate the k8s profile by setting `SPRING_PROFILES_ACTIVE=k8s` in your deployment manifest.

---

## Architecture Overview

### Local (Docker Compose)

```
React Client
    │  HTTP
    ▼
API Gateway (port 8080)
    │
    ├── /foodRequest ──► Donation Service  :8083  (REST)  :9093 (gRPC server)
    │                        │
    │                        ├── gRPC client ──► food-service  :9091
    │                        └── Kafka producer ──► donation-topic ──► notification-service
    │
    ├── /customer    ──► Customer Service  :8082
    └── /foodRequest ──► Food Service      :8081
```

### Cloud (GCP / Kubernetes)

- **Ingress Controller** routes external traffic to services by path prefix
- All services run in a private Kubernetes cluster
- Databases: GCP Cloud SQL (PostgreSQL) per service
- Kafka managed cluster inside k8s namespace
- Notification Service consumes `donation-topic` and sends emails via Gmail SMTP

### Inter-service Communication

| From | To | Protocol | Purpose |
|---|---|---|---|
| donation-service | food-service | gRPC | Hydrate food item details for donation detail page |
| customer-service | donation-service | gRPC | Fetch donations by user ID |
| donation-service | Kafka (`donation-topic`) | Kafka | Notify notification-service on create/update/delete |

---

## Domain Model (ERD Summary)

### `donations` table

| Column | Type | Notes |
|---|---|---|
| `id` | VARCHAR(36) | UUID, generated via `@PrePersist` |
| `donation_name` | VARCHAR(255) | Human-readable label |
| `doner_details_user_id` | VARCHAR(36) | Embedded donor user ID |
| `doner_details_first_name` | VARCHAR(100) | |
| `doner_details_last_name` | VARCHAR(100) | |
| `doner_details_email` | VARCHAR(255) | |
| `doner_details_phone` | VARCHAR(20) | |
| `pick_up_address` | VARCHAR(500) | Physical pickup location |
| `pick_up_time` | TIMESTAMP | Scheduled pickup |
| `estimated_quantity_amount` | NUMERIC(12,2) | Quantity amount |
| `estimated_quantity_unit` | VARCHAR(32) | `KG` or `SERVINGS` |
| `status` | VARCHAR(32) | `ACTIVE`, `CLAIMED`, `CANCELLED` |
| `creation_date` | TIMESTAMP | Auto-set by Hibernate |
| `update_date` | TIMESTAMP | Auto-updated by Hibernate |

### `donation_food_items` join table

Stores the list of food request IDs associated with a donation. These IDs reference records in the `food-service` database (no FK across services — microservice boundary).

| Column | Notes |
|---|---|
| `donation_id` | FK → `donations.id` ON DELETE CASCADE |
| `food_item_id` | UUID of food request in food-service |

### Status Lifecycle

```
ACTIVE ──► CLAIMED     (a recipient claims the donation)
       └──► CANCELLED  (the donor cancels)
```

---

## REST API

Base URL: `http://localhost:8083/api/v1/donations`

| Method | Endpoint | Description | Response |
|---|---|---|---|
| GET | `/` | List all donations (no food hydration) | `200 List<DonationDTO>` |
| GET | `/{id}?page=0&size=10` | Donation detail + paginated food items | `200 DonationDetailDTO` |
| POST | `/` | Create a donation | `200 DonationDTO` |
| PUT | `/{id}` | Update a donation | `200 DonationDTO` |
| DELETE | `/{id}` | Delete a donation | `200 String` |

### `GET /{id}` — Pagination query params

| Param | Default | Description |
|---|---|---|
| `page` | `0` | 0-based page index |
| `size` | `10` | Items per page |

### `DonationDetailDTO` response shape

```json
{
  "donation": { /* full DonationDTO */ },
  "foodItems": [ /* paginated FoodDetailDTO list */ ],
  "totalFoodItems": 42,
  "currentPage": 0,
  "pageSize": 10,
  "totalPages": 5
}
```

`totalFoodItems` is always the full count across all pages — use this for the table heading (e.g., "Food Items (42)").

---

## gRPC API

**Server port:** `9093` (local) / `${DONATION_GRPC_SERVER_PORT}` (k8s)
**Proto package:** `greengrub.proto.donation`
**Shared library:** `com.greengrub:proto-contracts:0.0.1-SNAPSHOT`

| RPC Method | Request | Response | Notes |
|---|---|---|---|
| `CreateDonation` | `CreateDonationRequest` | `DonationResponse` | |
| `GetAllDonations` | `DonationListRequest` | `DonationListResponse` | Supports page/pageSize |
| `GetDonationById` | `DonationByIdRequest` | `DonationResponse` | Base donation only (no food hydration) |
| `GetDonationsByUserId` | `DonationByUserIdRequest` | `DonationListResponse` | Filtered by embedded donor userId |
| `UpdateDonation` | `UpdateDonationRequest` | `DonationResponse` | |
| `DeleteDonation` | `DonationByIdRequest` | `DeleteDonationResponse` | |
| `GetFoodItemsByDonationId` | `FoodListRequest` | `FoodListResponse` | Paginated; hydrates from food-service |
| `GetUsersByDonationId` | `DonationByIdRequest` | `ListUsersResponse` | Returns embedded donor detail |

---

## Kafka — Event Contract

### Producer

| Property | Value |
|---|---|
| Topic | `donation-topic` |
| Key | `donationId` (String) |
| Serializer | `JsonSerializer` with type mapping header |
| Type mapping | `donationEvent → DonationEventDTO` |
| Bootstrap servers | `${KAFKA_BOOTSTRAP_SERVERS}` |

Events are published on: **create**, **update**, and **delete** operations.

### Event payload — `DonationEventDTO`

```json
{
  "donationId": "uuid",
  "donorName": "Jane Doe",
  "donorEmail": "jane@example.com",
  "totalAmount": 5.50,
  "createdAt": "2026-05-25T10:00:00",
  "organizationName": "GreenGrub",
  "customer": {
    "id": "user-uuid",
    "firstname": "Jane",
    "lastname": "Doe",
    "email": "jane@example.com",
    "phone": "1234567890"
  },
  "items": [
    {
      "foodName": "Pasta Donation",
      "quantity": 5,
      "unit": "KG",
      "category": null
    }
  ]
}
```

**Important:** The type mapping header `donationEvent` must match the consumer's `spring.json.type.mapping` in notification-service (`donationEvent:com.greengrub.notification.dto.Donation`). This allows the consumer to deserialize without needing the same class package.

### Best-effort publish

Kafka publish happens **after** the DB transaction commits. If Kafka is unavailable, the donation is still saved and the user gets a success response. The failure is logged at WARN level. This is intentional — a donation should not fail because the notification system is down.

---

## gRPC Clients

### FoodServiceClient

| Property | Value |
|---|---|
| Channel | `food-service` |
| Address (local) | `localhost:9091` |
| Address (k8s) | `${FOOD_SERVICE_HOST}:${FOOD_GRPC_SERVICE_PORT}` |
| Negotiation | plaintext |
| Called method | `GetFoodsByIds` |
| Purpose | Hydrate food item details for donation detail page |

**Pagination strategy:** The full list of food IDs is stored on the donation entity. The client slices the requested page from the list and calls `GetFoodsByIds` with only the IDs on the current page. `totalFoodItems` is always computed from the full list size before slicing.

**Degradation:** If food-service is unavailable (circuit open or retries exhausted), `foodItems` returns as an empty list — the donation detail is still returned successfully. This is a best-effort enrichment.

---

## Flyway Migrations

Location: `src/main/resources/db/migration/`

| File | Description |
|---|---|
| `V1__init_donations.sql` | Creates `donations` table with all columns and indexes |
| `V2__init_donation_food_items.sql` | Creates `donation_food_items` join table |

**Hibernate DDL:** `validate` — Flyway owns the schema. If the entity and migration drift, the application will fail to start with a clear error rather than silently mutating the database.

**`baseline-on-migrate: true`** — Allows Flyway to run against an existing database that was previously managed by Hibernate `ddl-auto=update`.

To add a new migration: create `V3__your_description.sql`. Never modify existing migration files.

---

## Resilience Patterns

All instances use Resilience4j 2.2.0 with Spring AOP. The `spring-boot-starter-aop` dependency is required for `@Retry` and `@CircuitBreaker` annotations to work.

### Resilience instance summary

| Instance | Applied To | Retry | Circuit Breaker | On Open |
|---|---|---|---|---|
| `kafkaPublishRetry` + `kafkaPublishBreaker` | Kafka producer | 3 attempts, 500ms exp backoff | 50% threshold, 10-call window, 30s open | Fallback logs warning, donation still saved |
| `dbRetry` + `dbBreaker` | DB reads (findById, findAll) | 3 attempts, 200ms exp backoff | 50% threshold (k8s), 60% (local) | 503 SERVICE_UNAVAILABLE |
| `foodServiceRetry` + `foodServiceBreaker` | FoodServiceClient gRPC calls | 2 attempts, 1s exp backoff | 50% threshold (k8s), 60% (local) | Fallback returns empty list (graceful degradation) |

### DB resilience note

`@Retry` is applied only to **read** operations (`findById`, `findAll`). Write operations (`save`, `delete`) are **not retried** because they are not idempotent — a failed save that was actually partially applied would be duplicated on retry. The `@CircuitBreaker` covers both reads and writes to protect the connection pool.

### Stacking order

```java
// Reads:
@Retry(name = "dbRetry")
@CircuitBreaker(name = "dbBreaker")
public List<DonationDTO> getAllDonation()

// Writes (CB only — no retry):
@CircuitBreaker(name = "dbBreaker")
public DonationDTO createDonation(...)

// Food hydration (degrade silently):
@Retry(name = "foodServiceRetry")
@CircuitBreaker(name = "foodServiceBreaker", fallbackMethod = "getFoodsByIdsFallback")
public List<FoodDetailDTO> getFoodsPage(...)
```

Circuit breaker states are exposed at `/actuator/health`:

```json
{
  "circuitBreakers": {
    "dbBreaker": { "status": "UP", "details": { "state": "CLOSED" } },
    "foodServiceBreaker": { "status": "UP", "details": { "state": "CLOSED" } },
    "kafkaPublishBreaker": { "status": "UP", "details": { "state": "CLOSED" } }
  }
}
```

---

## Exception Handling

### REST — `GlobalExceptionHandler` (`@ControllerAdvice`)

| Exception | HTTP Status | Notes |
|---|---|---|
| `DonationNotFoundException` | 404 NOT_FOUND | Thrown when a donation ID does not exist |
| `FoodServiceException` | 503 SERVICE_UNAVAILABLE | food-service gRPC call failed (after retries exhausted) |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | Bean Validation failure; includes per-field errors |
| `ConstraintViolationException` | 400 BAD_REQUEST | Jakarta constraint violation |
| `HttpMessageNotReadableException` | 400 BAD_REQUEST | Invalid JSON or bad enum value; hints at accepted enum values |
| `MethodArgumentTypeMismatchException` | 400 BAD_REQUEST | Wrong type in path/query param |
| `HttpRequestMethodNotSupportedException` | 405 METHOD_NOT_ALLOWED | Wrong HTTP verb |
| `DataAccessException` | 503 SERVICE_UNAVAILABLE | Database error |
| `CallNotPermittedException` | 503 SERVICE_UNAVAILABLE | Circuit breaker open |
| `Exception` (catch-all) | 500 INTERNAL_SERVER_ERROR | Unexpected error; full stack logged |

### Error response shape

```json
{
  "timestamp": "2026-05-25T10:00:00",
  "status": 404,
  "error": "Donation Not Found",
  "message": "Donation with id abc-123 not found",
  "path": "/api/v1/donations/abc-123"
}
```

Validation errors include an additional `fieldErrors` array:

```json
{
  "fieldErrors": [
    { "field": "donationName", "message": "must not be blank" }
  ]
}
```

### gRPC — `GrpcExceptionInterceptor` (`@GrpcGlobalServerInterceptor`)

| Exception | gRPC Status |
|---|---|
| `DonationNotFoundException` | `NOT_FOUND` |
| `IllegalArgumentException` / `ConstraintViolationException` | `INVALID_ARGUMENT` |
| `FoodServiceException` | `UNAVAILABLE` |
| `CallNotPermittedException` | `UNAVAILABLE` |
| `DataAccessException` | `UNAVAILABLE` |
| Any other | `INTERNAL` |

---

## CORS Configuration

Handled by `CorsConfig.java`. Routes: `/api/**`.

| Profile | Allowed Origins |
|---|---|
| `local` | `http://localhost:3000`, `http://localhost:5173` (CRA and Vite React dev servers) |
| `k8s` | `${CORS_ALLOWED_ORIGINS:}` — empty default means no CORS headers (API Gateway is the sole external caller in production) |

Credentials are enabled only when origins are explicitly configured (never with a wildcard).

---

## Project Structure

```
src/main/java/com/greengrub/donationService/
├── client/
│   └── FoodServiceClient.java          # gRPC client for food-service (food hydration)
├── config/
│   ├── CorsConfig.java                 # Profile-driven CORS configuration
│   └── SwaggerConfig.java              # OpenAPI / Swagger UI configuration
├── controller/
│   └── DonationController.java         # REST endpoints
├── dto/
│   ├── DonationDTO.java                # Core donation data transfer object
│   ├── DonationDetailDTO.java          # Enriched detail response (donation + paginated food)
│   ├── FoodDetailDTO.java              # Hydrated food item from food-service
│   ├── UserDetailDTO.java              # Embedded donor details
│   └── QuantityDTO.java                # Quantity + unit
├── entity/
│   ├── Donation.java                   # JPA entity (@Table: donations)
│   ├── UserDetail.java                 # @Embeddable donor info
│   ├── Quantity.java                   # @Embeddable quantity info
│   ├── DonationStatus.java             # Enum: ACTIVE, CLAIMED, CANCELLED
│   └── Unit.java                       # Enum: KG, SERVINGS
├── exception/
│   ├── DonationNotFoundException.java  # 404 — donation not found
│   ├── FoodServiceException.java       # 503 — food-service gRPC failure
│   ├── KafkaPublishException.java      # Kafka publish failure (internal)
│   ├── GlobalExceptionHandler.java     # Centralised REST exception handler
│   ├── ErrorResponse.java              # Standard error response record
│   └── ValidationErrorResponse.java   # Validation error response record
├── grpc/
│   ├── DonationGrpcService.java        # gRPC server implementation
│   └── GrpcExceptionInterceptor.java   # Maps exceptions to gRPC status codes
├── kafka/
│   ├── DonationKafkaProducer.java      # Resilience4j-wrapped Kafka producer
│   ├── DonationEventDTO.java           # Kafka event payload record
│   └── KafkaProducerConfig.java        # Producer factory with type mapping
├── mapper/
│   └── DonationProtoMapper.java        # Proto <-> DTO bidirectional mapping
├── repository/
│   └── DonationRepository.java         # Spring Data JPA + @EntityGraph
└── service/
    ├── DonationService.java            # Service interface
    └── Impl/
        └── DonationServiceImpl.java    # Service implementation

src/main/resources/
├── application.properties              # Base config (ports, Kafka, Flyway, Resilience4j)
├── application-local.properties        # Local overrides (Postgres, CORS, lighter CB thresholds)
├── application-k8s.yml                 # K8s overrides (Cloud SQL, env vars, stricter CB thresholds)
└── db/migration/
    ├── V1__init_donations.sql          # donations table
    └── V2__init_donation_food_items.sql # donation_food_items join table
```
