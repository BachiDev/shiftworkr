# ShiftWorkr

Intelligent, enterprise-grade hospital ward scheduling and compliance management —
built with **Java 21, Spring Boot 4, and Vaadin 25 Flow**. Portfolio
project by [Fabian Bachmayer](https://bachi.dev): complex business logic, real-time
collaboration, role-based workflows, production-grade engineering.

> Status: Phase 2 (interactive schedule board) in progress. Full build plan, domain model, and roadmap:
> [`PLAN.md`](./PLAN.md) — updated as the project evolves.

## Stack

| Layer | Technology |
| :--- | :--- |
| Language / Runtime | Java 21 LTS |
| Backend | Spring Boot 4.1.x, Spring Data JPA, Spring Security 7 |
| UI | Vaadin 25.3 (Flow, server-driven, `@Push` WebSockets) — free components only |
| DB (dev / test) | PostgreSQL 16, local Docker container + Testcontainers |
| DB (live site only) | Neon Postgres (pooled `:6543` runtime, direct `:5432` Flyway) |
| Migrations | Flyway versioned (`src/main/resources/db/migration`) |
| Tests | JUnit 5 + Testcontainers + Karibu (headless Vaadin UI tests) |
| Prod hosting | OCI Always-Free ARM VM, Caddy (auto-TLS), multi-stage Docker build |

## Quickstart

Prerequisites: Java 21, Docker + Compose, Maven 3.9+ (or install via `winget`/package manager).

```bash
# 1. Start local PostgreSQL 16
docker compose -f docker-compose.dev.yml up -d

# 2. Run in dev mode (Vite hot reload, http://localhost:8080)
mvn spring-boot:run

# 2b. Run with demo data (16 staff, current + next month rosters)
mvn spring-boot:run -Dspring-boot.run.profiles=dev,demo
#    Sign in as sarah.miller@shiftworkr.local / demo1234 (ward manager)

# 3. Verify: unit + Testcontainers + Karibu tests
mvn clean verify

# 4. Run pending Flyway migrations against local DB
mvn flyway:migrate
```

Health check: `http://localhost:8080/actuator/health`

## Profiles & configuration

| Profile | Datasource | Purpose |
| :--- | :--- | :--- |
| `dev` (default) | local PG container | daily development |
| `prod` | Neon via `JDBC_URL` (pooler) + `FLYWAY_URL` (direct) | live site only |
| `demo` | inherits dev/prod | seed data + one-click demo logins (never in production) |

Copy `.env.example` to `.env` for local DB credentials. Production secrets
(`JDBC_URL`, `FLYWAY_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_PASSWORD`)
are injected as environment variables — see `docker-compose.prod.yml`.

Key settings: Hikari pool tuned for Neon serverless, `prepareThreshold=0` for
PgBouncer transaction mode, `server.forward-headers-strategy: framework` so Vaadin
`@Push` WebSockets survive the Caddy reverse proxy, UTC (`timestamptz`) storage with
`Europe/Berlin` wall-clock at the edges.

## Production build & deploy

```bash
mvn clean package -Pproduction -DskipTests   # Vaadin production frontend bundle
docker build -t shiftworkr .                  # jammy builder + alpine runtime
docker compose -f docker-compose.prod.yml up -d  # app + Caddy (needs secrets, see above)
```

CI (`.github/workflows/ci.yml`): stylelint CSS-selector check + `mvn verify` + Flyway
migration check against an ephemeral PG service, Docker build smoke test on `main`.
Qodana runs the IntelliJ inspection engine on PRs and `main`
(`.github/workflows/qodana.yml`).

## Engineering policies

- **Zero-warning policy:** `maven-compiler-plugin` runs `-Xlint:all` with `failOnWarning`
  (minus `options`/`processing`/`serial`/`this-escape`, each documented in `pom.xml`
  as incompatible with Vaadin UI idioms). Any new javac warning fails the build.
- **Boot gate:** `ApplicationContextTest` boots the full Spring context on real
  PostgreSQL, so dependency mismatches fail CI instead of production.
- **IDE-grade static analysis:** Qodana runs the IntelliJ inspections that javac cannot
  see (unused declarations, nullness/data flow, lambda refactors). Run it locally with
  `powershell -File scripts/qodana.ps1`; the script bootstraps the pinned Qodana CLI on
  first use and runs natively (no Docker pull), writing results to `.qodana/`. The
  Community linter needs no Qodana Cloud token. CSS type selectors (IntelliJ's
  `CssInvalidHtmlTagReference`) are checked by stylelint:
  `npx --yes stylelint@16.26.1 src/main/frontend/styles.css --config stylelint.config.mjs`.
- Conventional Commits (`feat:`, `fix:`, `chore(deps):`, `docs:`, `refactor:`).

## Project structure

```
src/main/java/dev/bachi/shiftworkr/
  ShiftworkrApplication.java   # Boot entry + @ConfigurationPropertiesScan
  config/                      # DemoProperties, JpaConfig (UTC mandate)
  ui/                          # MainLayout, AppShell, views
src/main/resources/
  application{,-dev,-prod,-demo}.yml
  db/migration/                # Flyway versioned migrations
  META-INF/additional-spring-configuration-metadata.json
src/main/frontend/             # Vaadin 25 frontend dir (shiftworkr-styles.css)
docker-compose.dev.yml         # local PG16
docker-compose.prod.yml        # app + Caddy (Neon DB external)
Dockerfile / Caddyfile / .env.example
```

---

© 2026 Fabian Bachmayer. All rights reserved.
