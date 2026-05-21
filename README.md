# Cozy Notebooks — Backend

A minimal but production-oriented Java 21 / Spring Boot 3 backend for a cozy
notebook application: `User -> Notebook -> Page -> Block`, plus reusable
`PageTemplate`s.

The frontend client is hosted at <https://cozy-notebooks.vercel.app/> and
consumes this API via the contract documented below.

## Tech

- Java 21, Spring Boot 3.3
- Spring Web, Spring Security, Spring Data JPA, Spring Validation, Actuator
- **MySQL 8.4** — local development matches **MySQL HeatWave on OCI** in production
- Flyway (with `flyway-mysql`)
- Jackson `JsonNode` mapped to MySQL `JSON` columns via `@JdbcTypeCode(SqlTypes.JSON)`
- springdoc-openapi (Swagger UI)
- JUnit 5, MockMvc, Testcontainers (MySQL)
- Gradle (Kotlin DSL), Docker, GitHub Actions

## Architecture

```
controller (api)        →  thin, validation only, no entities returned
   service              →  business rules + tenant scoping
   repository (jpa)     →  database access only
domain (entities)       →  JPA entities, never leak to API
api/dto                 →  request/response records (Jakarta Validation)
service/mapper          →  entity ↔ DTO
security                →  CurrentUserProvider + pluggable auth filter
api/GlobalExceptionHandler  →  unified ErrorResponse
```

### Tenant isolation

Every service operation goes through `CurrentUserProvider.requireId()` and
every repository query is scoped by `userId`. Loading a resource that belongs
to another user returns `404 not_found` (we do not leak existence).

### Soft delete on MySQL

Every main entity has a `deleted_at TIMESTAMP(6)` column. MySQL doesn't
support partial indexes, so we use composite indexes that put `deleted_at`
right after the tenant/parent column:

| Table             | Index                                            |
|-------------------|--------------------------------------------------|
| `notebooks`       | `(user_id, deleted_at)`                          |
| `pages`           | `(notebook_id, deleted_at, position)`            |
| `pages`           | `(user_id, deleted_at)`                          |
| `blocks`          | `(page_id, deleted_at, position)`                |
| `blocks`          | `(user_id, deleted_at)`                          |
| `page_templates`  | `(user_id, deleted_at)`                          |
| `assets`/`tags`/`devices` | `(user_id, deleted_at)`                  |

The repository methods always end in `...AndDeletedAtIsNull`, so the optimizer
can use these composite indexes for both the active-row lookup and the sort
on `position`.

### JSON storage

`blocks.content`, `blocks.settings`, and `page_templates.blocks` are MySQL
`JSON` columns. Hibernate 6 + Jackson handle `JsonNode` natively via
`@JdbcTypeCode(SqlTypes.JSON)` — no third-party type library is needed.

### Type safety in SQL

CHECK constraints are kept at the DB level for typed string columns:

- `blocks.type` — restricts to the 17 supported block types.
- `devices.platform` — `ios | android | web | macos | windows | linux`.
- `sync_changes.entity_type` — `notebook | page | block | template | asset | tag`.
- `sync_changes.operation` — `create | update | delete | restore | reorder`.

### JWT-ready security

`SecurityConfig` runs Spring Security in stateless mode and installs an
authentication filter before `UsernamePasswordAuthenticationFilter`. For
the MVP that filter is `MockAuthenticationFilter`, which puts a fixed
`CurrentUser` (UUID + email) on the security context. To switch to JWT:

1. Replace `MockAuthenticationFilter` with a JWT-decoding filter.
2. Make the filter set the principal to `new CurrentUser(uuid, email)`.
3. Set `cozy.security.mock-user-enabled=false`.

Nothing in services, controllers, or DTOs needs to change.

## Page document storage

A page is a self-contained JSON document. `pages.content_json` holds the
**entire** page — including its internal block array — and the backend
treats that JSON as opaque. The frontend owns block-level structure,
ordering, and per-type schemas. There is no separate `blocks` table and no
block-level API.

Each page row also carries:

- `content_hash` — SHA-256 hex digest of `ObjectMapper.writeValueAsBytes(content_json)`,
  recomputed on every successful write.
- `version` — starts at `1`, incremented by one on every successful PUT.

Page templates (`page_templates`) follow the same shape: a template stores a
full page document; instantiating a template copies its `content_json` into
a brand-new page.

### Conflict detection

`PUT /pages/{pageId}` accepts an optional `baseHash` field. When present,
the server compares it (string equality) to the page's stored
`content_hash`. A mismatch returns HTTP 409 with `code: "conflict"` and the
DB row is left unchanged. When `baseHash` is omitted, the update is
applied unconditionally.

## Endpoints

Base path: `/api/v1`

```
GET    /health

GET    /notebooks
POST   /notebooks
GET    /notebooks/{notebookId}
PATCH  /notebooks/{notebookId}
DELETE /notebooks/{notebookId}

GET    /notebooks/{notebookId}/pages
POST   /notebooks/{notebookId}/pages
GET    /pages/{pageId}
PUT    /pages/{pageId}
DELETE /pages/{pageId}

GET    /templates
POST   /templates
GET    /templates/{templateId}
PATCH  /templates/{templateId}
DELETE /templates/{templateId}
POST   /templates/{templateId}/create-page
```

OpenAPI / Swagger UI: <http://localhost:8080/swagger-ui.html>
Raw spec: <http://localhost:8080/v3/api-docs>

## Running locally

### With Docker Compose (recommended)

```bash
docker compose up --build
```

This starts MySQL 8.4 on `:3306` and the API on `:8080`. Flyway runs the
migrations on startup, including a dev user seeded as
`00000000-0000-0000-0000-000000000001`.

Try it:

```bash
curl http://localhost:8080/api/v1/health

curl -X POST http://localhost:8080/api/v1/notebooks \
  -H 'Content-Type: application/json' \
  -d '{"title":"My first notebook"}'
```

### With a local MySQL 8.4

```bash
# in one shell
docker run --rm \
  -e MYSQL_DATABASE=cozy_notebooks \
  -e MYSQL_USER=cozy \
  -e MYSQL_PASSWORD=cozy \
  -e MYSQL_ROOT_PASSWORD=root \
  -p 3306:3306 mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_0900_ai_ci \
  --default-time-zone=+00:00

# in another
./gradlew bootRun
```

### Connecting to MySQL HeatWave on OCI

The schema and Flyway scripts in this repository are designed to run
unchanged on **MySQL HeatWave** (MySQL 8.4-compatible). To connect:

```yaml
spring:
  datasource:
    url: jdbc:mysql://<heatwave-endpoint>:3306/cozy_notebooks?useSSL=true&serverTimezone=UTC
    username: <user>
    password: <password>
```

Or via env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`.

### Configuration

| Variable                  | Default                                                                                |
|---------------------------|----------------------------------------------------------------------------------------|
| `SPRING_DATASOURCE_URL`   | `jdbc:mysql://localhost:3306/cozy_notebooks?...&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | `cozy`                                                                              |
| `SPRING_DATASOURCE_PASSWORD` | `cozy`                                                                              |
| `SERVER_PORT`             | `8080`                                                                                 |
| `COZY_MOCK_USER_ENABLED`  | `true`                                                                                 |
| `COZY_MOCK_USER_ID`       | `00000000-0000-0000-0000-000000000001`                                                 |
| `COZY_MOCK_USER_EMAIL`    | `dev@cozy.local`                                                                       |

## Testing

```bash
./gradlew test
```

Tests use Testcontainers to spin up a real **MySQL 8.4** container
(`mysql:8.4`). The `AbstractIntegrationTest` base class boots the full
Spring context with MockMvc.

Test coverage includes:

- Health endpoint.
- Notebook CRUD + validation errors with the unified error format.
- Page + block creation, update, delete, and reorder.
- Template CRUD and `create-page` materialization.
- Tenant isolation: foreign user's notebook returns 404.
- Validation tests (missing required fields, invalid block type, invalid UUID path).
- Frontend smoke flow (`FrontendFlowSmokeIT`): notebook → page → paragraph + checklist
  blocks → patch content → reorder → soft-delete, asserting deleted blocks are gone.

### Default mode: Testcontainers (CI and Linux/macOS contributors)

**Simply running `./gradlew test` assumes Docker Desktop (or compatible Docker daemon) is running** so Testcontainers can start MySQL 8.4. If Docker is not available, switch to **local-MySQL mode** below.

If `USE_LOCAL_MYSQL` is not set, `AbstractIntegrationTest` boots a fresh
`mysql:8.4` Testcontainer per JVM. This is what runs in CI and is the
recommended setup whenever Testcontainers can talk to your local Docker
daemon out of the box.

### Local-MySQL mode for Windows / IntelliJ users

Testcontainers' Docker auto-detection on Windows + Docker Desktop is
notoriously fragile (named-pipe issues, stale `~/.testcontainers.properties`,
etc.). To avoid wrestling with it, the test base class supports an opt-in
mode that **does not start any Testcontainers container** and instead runs
the suite against a MySQL you start yourself.

How it works: when the env var `USE_LOCAL_MYSQL=true` is set,
`AbstractIntegrationTest` skips constructing/starting `MySQLContainer`
entirely and registers the datasource as:

- `spring.datasource.url=jdbc:mysql://localhost:3306/cozy_notebooks`
- `spring.datasource.username=cozy`
- `spring.datasource.password=cozy`

Flyway still runs all migrations on Spring context startup, so the schema
is up to date for every test session.

Workflow:

```bash
# 1. Start MySQL from the repo's docker-compose (once)
docker compose up -d mysql

# 2a. Shell env (POSIX / Git Bash)
USE_LOCAL_MYSQL=true ./gradlew clean test

# 2b. Gradle property — sets USE_LOCAL_MYSQL on forked JVMs (helps on CMD/PowerShell)
./gradlew clean test -Pcozy.test.useLocalMysql=true

# 2c. Optional: Gradle task pulls MySQL up when local mode or -P flag is enabled
USE_LOCAL_MYSQL=true ./gradlew startLocalMysql clean test
```

To stop / reset the DB later: `docker compose down -v`.

`FlywayV5HrefCodeBackfillIT` also honors local mode: it creates logical database `cozy_notebooks_href_migration` on `localhost` (via `CREATE DATABASE`), using **`root` / root password `root`** — matching `docker-compose.yml` — so it does not collide with Flyway migrations on `cozy_notebooks` used by `AbstractIntegrationTest`.

#### IntelliJ "Run Backend Tests" configuration

| Setting | Value |
|---|---|
| Gradle task | `clean test` |
| Gradle arguments OR env | `-Pcozy.test.useLocalMysql=true` **or** `USE_LOCAL_MYSQL=true` (+ optional `TESTCONTAINERS_RYUK_DISABLED=true`) |

Notes:

- Machine-specific overrides (Ryuk tweaks, exotic `DOCKER_HOST`, etc.)
  belong in IntelliJ Gradle run configs — not git.
- The repo **`build.gradle.kts`** intentionally forwards `-Pcozy.test.useLocalMysql=true` → `USE_LOCAL_MYSQL=true` only for Gradle `test`; that is documented here and avoids fragile manual exports on Windows.
- The credentials/URL above match the `mysql` service in this repo's
  `docker-compose.yml`. If you point at a different MySQL, adjust the
  service password accordingly.

## Project layout

```
src/main/java/com/cozy/notebooks
├── CozyNotebooksApplication.java
├── api/                # controllers, DTOs, error response
│   ├── dto/
│   ├── ErrorResponse.java
│   └── GlobalExceptionHandler.java
├── config/             # OpenAPI / Swagger
├── domain/             # JPA entities (BaseEntity + per-aggregate)
├── exception/          # NotFound, BadRequest, Forbidden, Unauthorized
├── repository/         # Spring Data JPA repositories
├── security/           # SecurityConfig, CurrentUser*, mock filter
└── service/            # business logic
    └── mapper/         # entity ↔ DTO
src/main/resources
├── application.yml          # MySQL JDBC URL by default
├── application-test.yml
└── db/migration/            # Flyway: V1__init_schema.sql (MySQL 8.4),
                             #         V2__seed_dev_user.sql
src/test/java/com/cozy/notebooks
├── api/                # MockMvc integration tests
├── security/           # tenant isolation
└── support/AbstractIntegrationTest.java   # MySQLContainer base class
```

## Deferred entities

`Asset`, `Tag`, `PageTag`, `Device`, `SyncChange` are present in the
schema (`V1__init_schema.sql`) so future migrations are additive, but
they have no controllers/services in the MVP. Wire them up incrementally
in the same controller / service / repository / DTO / mapper pattern.

## CI

`.github/workflows/ci.yml` runs `./gradlew clean build` on every push and
pull request to `main` using JDK 21. Tests use Testcontainers (MySQL 8.4),
so the runner needs Docker — the `ubuntu-latest` runner provides it by
default.
