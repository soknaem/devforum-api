# DevForum API

A decoupled RESTful backend for a developer resource-sharing and community forum platform, built with **Spring Boot 3**, **Java 21**, **PostgreSQL**, and **JWT** stateless authentication.

## Tech Stack

- Java 21, Spring Boot 3.2.5
- Gradle (Groovy DSL)
- Spring Data JPA + Hibernate (Criteria API / `Specification`)
- Spring Security + JWT (`jjwt` 0.12.5)
- MapStruct 1.5.5.Final for entity <-> DTO mapping
- Lombok
- PostgreSQL

## Getting Started

### 1. Create the database

```sql
CREATE DATABASE forum_db;
```

### 2. Configure connection

Edit `src/main/resources/application.yml` if your PostgreSQL credentials differ from the defaults (`postgres` / `postgres` on `localhost:5432`).

### 3. Run the application

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

On first boot, `DataInitializer` seeds:
- 5 categories (Books, Code Snippets, Videos, Articles, Tools)
- Two users: `admin` / `Admin@123` (ROLE_ADMIN) and `demo` / `Demo@123` (ROLE_USER)

## Authentication

| Method | Endpoint                  | Access | Description               |
|--------|----------------------------|--------|----------------------------|
| POST   | `/api/v1/auth/register`    | Public | Register a new user       |
| POST   | `/api/v1/auth/login`       | Public | Log in, returns a JWT     |

Include the token on protected requests:

```
Authorization: Bearer <token>
```

## Resources

| Method | Endpoint                | Access              | Description                         |
|--------|--------------------------|---------------------|--------------------------------------|
| GET    | `/api/v1/resources`      | Public              | Paginated + filterable list          |
| GET    | `/api/v1/resources/{id}` | Public              | Get single resource                  |
| POST   | `/api/v1/resources`      | Authenticated       | Create a resource                    |
| PUT    | `/api/v1/resources/{id}` | Owner or Admin      | Update a resource                    |
| DELETE | `/api/v1/resources/{id}` | Owner or Admin      | Delete a resource                    |

### Query parameters for `GET /api/v1/resources`

- `search` – case-insensitive match on title/description
- `categoryId` – filter by category
- `type` – `BOOK` | `CODE` | `LINK` | `VIDEO` | `ARTICLE`
- `status` – `DRAFT` | `PUBLISHED` | `ARCHIVED`
- `tag` – filter by tag name
- `page`, `size`, `sortBy`, `sortDir` – pagination & sorting

### Example: create a resource

```bash
curl -X POST http://localhost:8080/api/v1/resources \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Effective Java",
        "description": "Best practices for the Java platform",
        "url": "https://example.com/effective-java",
        "type": "BOOK",
        "status": "PUBLISHED",
        "categoryId": 1,
        "tags": ["java", "best-practices"]
      }'
```

Tags are auto-linked: any tag name that already exists (case-insensitive) is reused, otherwise a new `Tag` row is created.

## Response Shape

All endpoints return a consistent envelope:

```json
{
  "success": true,
  "message": "Request processed successfully",
  "data": { },
  "timestamp": "2026-09-05T10:00:00"
}
```

Paginated list `data` looks like:

```json
{
  "content": [ ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 42,
  "totalPages": 5,
  "isLast": false
}
```
