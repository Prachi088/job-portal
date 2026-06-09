# SATI Alumni Portal — Backend

A **Spring Boot** REST API powering the SATI Alumni Portal — a full-stack college networking platform for **Samrat Ashok Technological Institute (SATI), Vidisha**. It handles authentication, job postings, applications, events, real-time messaging, connections, notifications, and an AI career assistant.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Configuration](#configuration)
5. [Security](#security)
   - [JWT Authentication](#jwt-authentication)
   - [JwtFilter — Presence Tracking](#jwtfilter--presence-tracking)
   - [SecurityConfig — CORS & Route Guards](#securityconfig--cors--route-guards)
6. [Data Model](#data-model)
7. [API Reference](#api-reference)
   - [Auth](#auth)
   - [Jobs](#jobs)
   - [Applications](#applications)
   - [Events](#events)
   - [Users & Profiles](#users--profiles)
   - [Connections](#connections)
   - [Messages](#messages)
   - [Notifications](#notifications)
   - [AI Chat](#ai-chat)
   - [Health](#health)
8. [AI Integration — GroqService](#ai-integration--groqservice)
9. [Running the Project](#running-the-project)
10. [Deployment](#deployment)

---

## Project Overview

Two user roles are supported:

| Role | Capabilities |
|---|---|
| **STUDENT** | Browse & apply to jobs, register for events, connect with alumni, real-time messaging, AI career assistant, resume upload |
| **RECRUITER / Alumni** | Post & manage jobs, create & manage events, review applicants, messaging with students |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT (jjwt) |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL (configurable via `application.properties`) |
| Password Hashing | BCrypt |
| AI Integration | Groq API (Llama 3.3 70B) via `RestTemplate` |
| Build Tool | Maven |
| Runtime | Java 17+ |

---

## Project Structure

```
src/main/java/com/jobportal/job_portal/
├── config/
│   ├── AppConfig.java          # RestTemplate bean
│   └── SecurityConfig.java     # Spring Security, CORS, route guards
├── controller/
│   ├── AuthController.java     # /auth/register, /auth/login
│   ├── JobController.java      # /jobs/**
│   ├── ApplicationController.java  # /api/applications/**
│   ├── EventController.java    # /api/events/**
│   ├── UserController.java     # /api/users/** (profile, resume, presence)
│   ├── ConnectionController.java   # /api/connections/**
│   ├── MessageController.java  # /api/messages/**
│   ├── NotificationController.java # /api/notifications/**
│   ├── ChatController.java     # /chat (AI assistant)
│   └── HealthController.java   # /health
├── entity/
│   ├── User.java
│   ├── Job.java
│   ├── Application.java
│   ├── Event.java
│   ├── EventApplication.java
│   ├── Connection.java
│   ├── ConnectionRequest.java
│   ├── Message.java
│   └── Notification.java
├── repository/
│   ├── UserRepository.java
│   ├── JobRepository.java
│   ├── ApplicationRepository.java
│   ├── EventRepository.java
│   ├── EventApplicationRepository.java
│   ├── ConnectionRepository.java
│   ├── ConnectionRequestRepository.java
│   ├── MessageRepository.java
│   └── NotificationRepository.java
├── security/
│   ├── JwtUtil.java            # Token generation & parsing
│   └── JwtFilter.java          # Per-request auth + presence update
└── service/
    └── GroqService.java        # Groq API client (AI chat)
```

---

## Configuration

Create `src/main/resources/application.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/job_portal
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=your_secret_key_minimum_32_characters_long
jwt.expiration=86400000

# Groq AI
groq.api.key=your_groq_api_key

# File upload limit (resumes stored as Base64 in DB)
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

> **Note:** `jwt.secret` must be at least 32 characters for HS256. If it is shorter, `JwtUtil` will throw an `IllegalStateException` on startup.

---

## Security

### JWT Authentication

`JwtUtil` generates and validates JWTs using HS256. Every token carries three claims:

| Claim | Value |
|---|---|
| `sub` | User email |
| `role` | User role (e.g. `STUDENT`, `RECRUITER`) |
| `id` | Numeric database user ID |

Including the `id` claim directly in the token is intentional — the frontend reads it to build API URLs like `/api/users/42`. Without it, the frontend would have to use the `sub` (email) as an identifier, which breaks path-variable based endpoints.

Token generation:
```java
jwtUtil.generateToken(email, role, userId)
```

Token validation reads `exp` and verifies the signature. Any `JwtException` (expired, tampered, malformed) causes `isTokenValid()` to return `false`.

### JwtFilter — Presence Tracking

`JwtFilter` runs on every request (`OncePerRequestFilter`). If a valid Bearer token is present it:

1. Extracts email, role, and userId from the token.
2. Sets a `UsernamePasswordAuthenticationToken` in the `SecurityContextHolder` so Spring Security treats the request as authenticated.
3. **Updates `lastSeenAt`** on the `User` entity — but throttled to once per 60 seconds to avoid a DB write on every poll or WebSocket heartbeat.

This passive presence tracking means the frontend never needs a dedicated "ping" endpoint — any authenticated API call keeps the user's online status current.

### SecurityConfig — CORS & Route Guards

CORS is locked to the production AWS S3 frontend origin. `allowCredentials` is `false` because auth is handled via the `Authorization` header (JWT), not cookies.

**Publicly accessible routes** (no token required):

| Method | Path |
|---|---|
| POST | `/auth/register` |
| POST | `/auth/login` |
| GET | `/health` |
| POST | `/chat` |
| GET | `/api/users` |
| GET | `/jobs` |
| GET | `/jobs/search` |
| GET | `/api/events` |
| GET | `/api/connections/users/all` |
| OPTIONS | `/**` (preflight) |

All other routes require a valid JWT. Missing or expired tokens return **401** (not 403) thanks to a custom `AuthenticationEntryPoint`.

---

## Data Model

| Entity | Table | Key Fields |
|---|---|---|
| `User` | `users` | id, name, email, password (BCrypt), role, skills, experience, education, company, batch, resumeData (Base64), lastSeenAt |
| `Job` | `jobs` | id, title, description, location, company, salary, type, recruiterId |
| `Application` | `applications` | id, userId, jobId, status (APPLIED / ACCEPTED / REJECTED) |
| `Event` | `events` | id, title, description, eventDate, location, recruiterId |
| `EventApplication` | `event_applications` | id, eventId, userId, appliedAt, status (REGISTERED / ATTENDED / CANCELLED) |
| `Connection` | `connections` | id, user1Id, user2Id, connectedAt |
| `ConnectionRequest` | `connection_requests` | id, senderId, receiverId, status (PENDING / ACCEPTED / REJECTED), createdAt |
| `Message` | `messages` | id, senderId, receiverId, content, isRead, createdAt |
| `Notification` | `notifications` | id, userId, actorId, type, message, isRead, createdAt |

**Presence** is derived, not stored separately. `isOnline = lastSeenAt > now - 2 minutes`.

**Resumes** are stored as Base64 strings in the `users.resume_data` column (TEXT). The field is annotated `@JsonIgnore` so it is never accidentally included in list responses.

---

## API Reference

All endpoints are prefixed relative to the base URL (default `http://localhost:8080`).

---

### Auth

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | No | Register a new user |
| POST | `/auth/login` | No | Login, returns JWT + user info |

**Register request body:**
```json
{
  "name": "Prachi Rajput",
  "email": "prachi@example.com",
  "password": "secret123",
  "role": "STUDENT"
}
```

**Login response:**
```json
{
  "token": "eyJ...",
  "role": "STUDENT",
  "name": "Prachi Rajput",
  "id": "42"
}
```

---

### Jobs

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/jobs` | No | Get all jobs |
| GET | `/jobs/{id}` | Yes | Get job by ID |
| GET | `/jobs/recruiter/{recruiterId}` | Yes | Get jobs posted by a recruiter |
| GET | `/jobs/search?title=` | No | Search jobs by title (case-insensitive) |
| POST | `/jobs` | Yes | Create a new job |
| DELETE | `/jobs/{id}` | Yes | Delete a job |

**Job request body:**
```json
{
  "title": "Backend Developer",
  "description": "...",
  "location": "Bangalore",
  "company": "TechCorp",
  "salary": "6-8 LPA",
  "type": "Full-time",
  "recruiterId": 1
}
```

---

### Applications

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/applications` | Yes | Apply for a job |
| GET | `/api/applications/user/{userId}` | Yes | Get applications by student |
| GET | `/api/applications/job/{jobId}` | Yes | Get applicants for a job (with user details) |
| GET | `/api/applications` | Yes | Get all applications |
| PUT | `/api/applications/{id}/status` | Yes | Update application status |

**Apply request body:**
```json
{ "userId": 42, "jobId": 7 }
```

**Update status request body:**
```json
{ "status": "ACCEPTED" }
```

Valid statuses: `APPLIED`, `ACCEPTED`, `REJECTED`.

---

### Events

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/events` | No | Get all events |
| GET | `/api/events/{id}` | Yes | Get event by ID |
| GET | `/api/events/recruiter/{recruiterId}` | Yes | Get events by recruiter |
| POST | `/api/events` | Yes | Create an event |
| DELETE | `/api/events/{id}` | Yes | Delete an event |
| POST | `/api/events/{eventId}/register` | Yes | Register for an event |
| GET | `/api/events/{eventId}/applications` | Yes | Get event registrants (with user details) |
| GET | `/api/events/user/{userId}/applications` | Yes | Get a user's event registrations |
| PUT | `/api/events/applications/{applicationId}/status` | Yes | Update event registration status |

**Event registration request body:**
```json
{ "userId": 42 }
```

Valid event application statuses: `REGISTERED`, `ATTENDED`, `CANCELLED`.

---

### Users & Profiles

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/users` | No | Get all users |
| GET | `/api/users/{id}` | Yes | Get user by ID |
| PUT | `/api/users/{id}/profile` | Yes | Update profile fields |
| GET | `/api/users/{id}/presence` | Yes | Get online status + lastSeenAt |
| POST | `/api/users/{id}/resume` | Yes | Upload resume (multipart/form-data, field: `resume`) |
| GET | `/api/users/{id}/resume` | Yes | Download resume (binary response) |

**Presence response:**
```json
{
  "isOnline": true,
  "lastSeenAt": "2026-06-09T20:55:00"
}
```

`isOnline` is `true` when `lastSeenAt` is within the last 2 minutes.

**Profile update** — all fields are optional and applied only if non-null:
`phone`, `address`, `skills`, `experience`, `education`, `company`, `currentRole`, `linkedinUrl`, `website`, `bio`, `projects`, `batch`.

---

### Connections

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/connections/users/all` | No | List all users (for discover page) |
| POST | `/api/connections/request` | Yes | Send a connection request |
| PUT | `/api/connections/request/{id}` | Yes | Accept or reject a request |
| GET | `/api/connections/requests/{userId}` | Yes | Get incoming pending requests |
| GET | `/api/connections/requests/sent/{userId}` | Yes | Get outgoing pending requests |
| GET | `/api/connections/{userId}` | Yes | Get accepted connections |
| DELETE | `/api/connections` | Yes | Remove a connection |

**Send request body:**
```json
{ "senderId": 1, "receiverId": 42 }
```

**Update request body:**
```json
{ "status": "ACCEPTED" }
```

Valid statuses: `ACCEPTED`, `REJECTED`. Only the receiver of the request may call the update endpoint (enforced server-side).

When a request is accepted, a `Connection` record is created and a `CONNECTION_ACCEPTED` notification is sent to the original sender. On rejection, a `CONNECTION_REJECTED` notification is sent instead.

**Remove connection body:**
```json
{ "userId": 1, "otherId": 42 }
```

---

### Messages

Messaging is restricted to connected users — the server checks `ConnectionRepository.existsBetweenUsers()` before allowing any send or read.

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/messages` | Yes | Send a message |
| GET | `/api/messages/conversation/{user1}/{user2}` | Yes | Get full conversation (chronological) |
| PUT | `/api/messages/read/{senderId}/{receiverId}` | Yes | Mark messages as read |
| GET | `/api/messages/unread/{userId}` | Yes | Get total unread message count |
| DELETE | `/api/messages/conversation` | Yes | Delete entire conversation |

**Send message body:**
```json
{
  "senderId": 1,
  "receiverId": 42,
  "content": "Hey, are you hiring?"
}
```

The `senderId` must match the authenticated user's ID (enforced via JWT). Messages are stored with `isRead: false` and `createdAt` set by the server.

**Delete conversation body:**
```json
{ "userId": 1, "otherId": 42 }
```

---

### Notifications

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/notifications/{userId}` | Yes | Get all notifications (newest first) |
| GET | `/api/notifications/{userId}/unread-count` | Yes | Get unread notification count |
| PUT | `/api/notifications/{userId}/mark-read` | Yes | Mark all notifications as read |
| PUT | `/api/notifications/read/{notificationId}` | Yes | Mark a single notification as read |

All endpoints verify that the authenticated user's ID matches the `userId` path variable (403 otherwise).

**Notification types:** `CONNECTION_ACCEPTED`, `CONNECTION_REJECTED`.

---

### AI Chat

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/chat` | No | Send a message to the AI career assistant |

**Request body:**
```json
{ "message": "How should I prepare for a Java interview?" }
```

**Response:**
```json
{ "reply": "Here are some key topics to focus on..." }
```

The endpoint is public (no JWT required) so unauthenticated users can still access the assistant from the landing page.

---

### Health

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/health` | No | Check if the server is running |

Returns a simple 200 response — used by the frontend to verify backend connectivity before showing the app.

---

## AI Integration — GroqService

`GroqService` wraps the [Groq API](https://groq.com) to provide fast LLM inference.

- **Model:** `llama-3.3-70b-versatile`
- **Max tokens:** 1024 per response
- **System prompt:** Instructs the model to act as a job portal career assistant focused on applications, resume tips, interview preparation, and career advice.
- **Error handling:** Rate limit responses (429) return a user-friendly "AI is busy" message rather than an error stack trace. All other exceptions return a safe error string.

The Groq API key is injected via `@Value("${groq.api.key}")` from `application.properties` — never hardcoded.

---

## Running the Project

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+ running locally (or a remote instance)

### Setup

1. Create the database:
```sql
CREATE DATABASE job_portal;
```

2. Fill in `application.properties` with your DB credentials, JWT secret, and Groq API key (see [Configuration](#configuration)).

3. Build and run:
```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`. Spring will auto-create all tables on first run (`ddl-auto=update`).

### Build JAR for deployment

```bash
mvn clean package -DskipTests
java -jar target/job-portal-0.0.1-SNAPSHOT.jar
```

---

## Deployment

The backend is deployed on **AWS EC2** (ap-south-1). The frontend S3 origin is whitelisted in `SecurityConfig.corsConfigurationSource()`. To add a new allowed origin (e.g. a local dev frontend), update the `setAllowedOrigins` list in `SecurityConfig.java`.

For production, set environment variables instead of hardcoding secrets in `application.properties`:

```bash
export JWT_SECRET=your_long_random_secret
export GROQ_API_KEY=gsk_...
```

And reference them in `application.properties`:
```properties
jwt.secret=${JWT_SECRET}
groq.api.key=${GROQ_API_KEY}
```
