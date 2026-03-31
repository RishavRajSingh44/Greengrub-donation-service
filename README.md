# GreenGrub — Donation Service

Part of the **GreenGrub** platform — a purpose-driven solution to prevent food wastage by connecting people who have excess food with those who truly need it.

The Donation Service handles the full lifecycle of food donation listings: creation, discovery, claiming, and cancellation.

---

## Table of Contents

- [About GreenGrub](#about-greengrub)
- [Service Responsibilities](#service-responsibilities)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Donation Status Lifecycle](#donation-status-lifecycle)
- [API Endpoints](#api-endpoints)
- [Request & Response Structure](#request--response-structure)
- [Error Handling](#error-handling)
- [Getting Started](#getting-started)
- [Environment Configuration](#environment-configuration)
- [Swagger UI](#swagger-ui)

---

## Service Responsibilities

- Create and manage food donation listings
- Track donor details and contact information
- Maintain donation status throughout its lifecycle
- Expose a clean REST API consumed by other GreenGrub services and clients

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Validation | Jakarta Bean Validation |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |
| Utilities | Lombok |

---

## Project Structure

```
src/main/java/com/greengrub/donationService/
├── config/
│   └── SwaggerConfig.java          # OpenAPI configuration
├── controller/
│   └── DonationController.java     # REST endpoints
├── dto/
│   ├── DonationDTO.java            # Main request/response DTO
│   ├── UserDetailDTO.java          # Donor personal details
│   └── EmailDTO.java               # Donor email
├── entity/
│   ├── Donation.java               # JPA entity
│   ├── UserDetail.java             # Embeddable donor details
│   ├── Email.java                  # Embeddable email
│   └── DonationStatus.java         # Enum: ACTIVE, CLAIMED, CANCELLED
├── exception/
│   ├── DonationNotFoundException.java
│   ├── ErrorResponse.java
│   ├── ValidationErrorResponse.java
│   └── GlobalExceptionHandler.java
├── repository/
│   └── DonationRepository.java
└── service/
    ├── DonationService.java
    └── Impl/
        └── DonationServiceImpl.java
```

---

## Donation Status Lifecycle

GreenGrub is a connector — not a delivery service. Once food is claimed, the handoff happens directly between donor and recipient.

```
ACTIVE ──► CLAIMED
       └─► CANCELLED
```

| Status | Meaning |
|---|---|
| `ACTIVE` | Listing is live and available to be claimed |
| `CLAIMED` | A recipient has taken the donation |
| `CANCELLED` | The donor withdrew the listing |

---

## API Endpoints

Base URL: `http://localhost:8083/api/donation`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/donation` | Create a new donation listing |
| `GET` | `/api/donation` | Get all donation listings |
| `GET` | `/api/donation/{id}` | Get a donation by ID |
| `PUT` | `/api/donation/{id}` | Update a donation |
| `DELETE` | `/api/donation/{id}` | Delete a donation |

---

## Request & Response Structure

### Create / Update — Request Body

```json
{
  "donationName": "Wedding Buffet Leftovers",
  "userDetail": {
    "firstName": "Jane",
    "lastName": "Doe",
    "phoneNumber": "9876543210"
  },
  "email": {
    "emailAddress": "jane.doe@example.com"
  },
  "status": "ACTIVE"
}
```

### Response Body

```json
{
  "id": 1,
  "donationName": "Wedding Buffet Leftovers",
  "userDetail": {
    "firstName": "Jane",
    "lastName": "Doe",
    "phoneNumber": "9876543210"
  },
  "email": {
    "emailAddress": "jane.doe@example.com"
  },
  "creationDate": "2026-03-31T10:00:00",
  "updateDate": "2026-03-31T10:00:00",
  "status": "ACTIVE"
}
```

> `id`, `creationDate`, and `updateDate` are read-only — managed automatically by the server.

---

## Error Handling

All errors follow a consistent response structure.

### Standard Error

```json
{
  "timestamp": "2026-03-31T10:05:00",
  "status": 404,
  "error": "Donation Not Found",
  "message": "No donation found with ID 99. It may have been deleted or never existed.",
  "path": "/api/donation/99"
}
```

### Validation Error

```json
{
  "timestamp": "2026-03-31T10:05:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields failed validation. Please review and correct the request.",
  "path": "/api/donation",
  "fieldErrors": [
    { "field": "donationName", "message": "Donation name is required" },
    { "field": "email.emailAddress", "message": "Email address must be a valid format (e.g. user@example.com)" }
  ]
}
```

### Invalid Status Value

```json
{
  "timestamp": "2026-03-31T10:05:00",
  "status": 400,
  "error": "Malformed Request Body",
  "message": "Invalid status value 'PENDING'. Accepted values are: ACTIVE, CLAIMED, CANCELLED.",
  "path": "/api/donation"
}
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- PostgreSQL running on port `5432`

### 1. Clone the repository

```bash
git clone https://github.com/your-org/GreenGrubDonationService.git
cd GreenGrubDonationService
```

### 2. Set up the database

Create a PostgreSQL database named `greengrub`:

```sql
CREATE DATABASE greengrub;
```

### 3. Configure environment

Copy the local properties template and update credentials if needed:

```bash
cp src/main/resources/application-local.properties src/main/resources/application-local.properties
```

Default local config (see [Environment Configuration](#environment-configuration)):
- Host: `localhost:5432`
- Database: `greengrub`
- Username: `postgres`
- Password: `postgres`

### 4. Run the application

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The service starts on **port 8083**.

---

## Environment Configuration

| Property | Default (local) | Description |
|---|---|---|
| `server.port` | `8083` | Port the service runs on |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/greengrub` | PostgreSQL connection URL |
| `spring.datasource.username` | `postgres` | Database username |
| `spring.datasource.password` | `postgres` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management strategy |

---

## Swagger UI

Interactive API documentation is available once the service is running:

| | URL |
|---|---|
| Swagger UI | `http://localhost:8083/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8083/v3/api-docs` |
