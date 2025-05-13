# Instant Payment System

A Spring Boot application that handles instant money transfers between users. The system provides REST APIs for payment
processing with retry mechanisms and circuit breaker patterns.

## Features

- Instant money transfers between accounts
- Idempotency support for safe retries
- Circuit breaker pattern for fault tolerance
- Kafka integration for transaction notifications
- Database transaction management
- REST API endpoints

## Prerequisites

- Java 21 or higher
- Gradle 8+
- PostgreSQL 15+ (server not running, only the client tools are needed)
- Apache Kafka 3.x
- Docker and Docker Compose

## Setup


### Using Docker Compose

For quick setup with Docker, use the provided `docker-compose.yml`:

### Database Setup

1. Start `postgres` container

`$ docker-compose up postgres`

2. Use `psql` to login to Postgres in container

`$ psql instantpayments -U postgres -h localhost`

Password: `postgres`

3. Create a PostgreSQL tables
Run `CREATE TABLE..` scripts from `database/database.sql`

Also insert seed data as well for testing.

4. Ctrl-C (e.g. stop PostgreSQL container)


## Building the Application

`$ ./gradlew clean build`

## Running the Application

### Using Docker Compose

`$ docker-compose up -d --build`


## API Endpoints

### Send Payment
```
POST /api/payments Content-Type: application/json
{ "senderId": "sender123", "recipientId": "recipient456", "amount": 100.00, "idempotencyKey": "unique-transaction-id" }
```

### Swagger UI

http://localhost:8080/swagger-ui/index.html


## Testing

Run unit tests:

`$ ./gradlew test`

## Monitoring

The application exposes actuator endpoints for monitoring:

- Health check: http://localhost:8080/health
- Info: http://localhost:8080/nfo

## Error Handling

The system handles various error scenarios:
- Insufficient balance
- Account not found
- Duplicate transactions (idempotency)
- System failures with circuit breaker fallback

## Security

Ensure to configure appropriate security measures:
- Enable HTTPS in production
- Implement authentication
- Set up proper authorization
- Secure sensitive configuration
