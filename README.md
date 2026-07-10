# GitBounty

GitBounty is a GitHub-style platform for hosting Git repositories, with an
escrow-based bounty system layered on top: repository owners can attach a
cash bounty to an issue, contributors claim it and open a pull request to
resolve it, and funds are held in escrow until the work is merged and
approved.

## Tech stack

**Backend**
- Java 17, Spring Boot 4.0.6 (Web, Security, Data JPA, OAuth2 Resource Server)
- MySQL, with schema migrations managed by Flyway
- JGit + JGit HTTP Server, embedded in the backend to serve Git repositories
  over smart HTTP (clone/fetch/push)
- springdoc-openapi (Swagger UI) for API documentation
- Lombok

**Frontend**
- React 19 + TypeScript, built with Vite
- React Router, Axios
- keycloak-js for authentication
- react-markdown + shiki for Markdown rendering and syntax highlighting

**Infrastructure**
- Keycloak (OIDC) as the identity provider
- Docker Compose for local development and deployment
- Caddy as reverse proxy / TLS termination in front of the frontend, backend,
  and Keycloak

## Architecture

Caddy sits at the edge and routes by hostname to three backing services:
- `{DOMAIN}` → the built frontend (static SPA, served as files)
- `api.{DOMAIN}` → the Spring Boot backend
- `auth.{DOMAIN}` → Keycloak

The backend is a single Spring Boot application that exposes a REST API
under `/api/**` and also acts as a Git server: repositories are stored on
disk (a Docker volume, `GIT_REPOSITORIES_ROOT`) and served directly via
JGit's smart HTTP implementation, so the backend itself is the origin for
`git clone`/`push`/`pull` operations against hosted repositories. Application
data (users, bounties, transactions, issues, pull requests, etc.) is
persisted to MySQL through Spring Data JPA, with Flyway managing schema
migrations. Authentication is delegated to Keycloak: the frontend
authenticates via Keycloak's OIDC flow, and the backend validates the
resulting JWTs as an OAuth2 resource server.

## Project structure

```
Backend/    Spring Boot API + embedded Git server (Maven project)
Frontend/   React + TypeScript SPA (gitbounty/ is the Vite app root)
keycloak/   Realm import config for the Keycloak identity provider
db/         Database init scripts
Caddyfile   Reverse proxy / routing config used by the caddy service
compose.yaml  Docker Compose definition for all services
```

## Features implemented

- **Repository management** — create, list, update, and delete repositories;
  manage collaborators/members; browse files and folders and view file
  contents; branch listing
- **Git operations** — push/pull/clone over smart HTTP via JGit; commit
  history
- **Issues** — create, list, view, assign, and change issue state
- **Pull requests** — create, list, view, diff, and merge
- **Bounty / escrow system** — post a bounty on an issue, claim/unclaim,
  cancel, and complete a bounty; funds are held in escrow and are released
  to the contributor when the bounty is completed, or refunded to the
  repository owner if the bounty is cancelled, its issue is closed without
  payment, or the repository is deleted
- **Payments & transactions** — credit top-ups, balance lookups, and
  transaction approval/rejection/dispute flows
- **User profiles** — view and update user profile data

Authentication and authorization (login, JWT validation, per-endpoint
permission checks) run through Keycloak, as described above in Architecture.

## Setup / running locally

1. Copy/create an `.env` file at the repo root. The variables actually read
   by the compose services and the Spring Boot app are:

   | Purpose | Variable |
   |---|---|
   | Domain / routing | `DOMAIN` |
   | Database (external MySQL — not a compose service) | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
   | Flyway | `FLYWAY_USER`, `FLYWAY_PASSWORD`, `FLYWAY_DEFAULT_SCHEMA` |
   | Keycloak | `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_HOSTNAME` |
   | Swagger / app | `SWAGGER_ENABLED`, `SWAGGER_OAUTH_CLIENT_ID` |
   | Build/deploy | `BRANCH_NAME` |

2. Add local DNS entries for the domain and its subdomains, e.g. in
   `/etc/hosts`:
   ```
   127.0.0.1 gitbounty.foo api.gitbounty.foo auth.gitbounty.foo
   ```
   (replace `gitbounty.foo` with whatever you set `DOMAIN` to). Without this,
   the hostnames Caddy routes on won't resolve locally.
3. Start everything:
   ```bash
   docker compose up --build
   ```
4. Visit `https://{DOMAIN}/` for the app, and
   `http://api.{DOMAIN}/swagger-ui/index.html` for the API docs. Caddy
   obtains a trusted certificate automatically, so HTTPS works with no
   browser warning.
5. Register an account through Keycloak at
   `http://auth.{DOMAIN}/realms/gitbounty/account/`.

## Testing

Backend tests run with Maven:

```bash
cd Backend
./mvnw test
```

The backend test suite combines plain unit tests (JUnit + Mockito),
Spring slice tests (`@WebMvcTest`, `@DataJpaTest`), full-context integration
tests (`@SpringBootTest`), and H2-backed persistence tests for
service-layer behavior. Controller, service, and permission logic each have
dedicated test classes (e.g. `BountyControllerIntegrationTest`,
`CodebaseControllerDeletionIntegrationTest`).

The frontend currently has no automated test suite; `npm run build` runs
TypeScript type-checking as part of the Vite build, and `npm run lint` runs
ESLint.

CI (`.github/workflows/ci.yml`) runs the backend test suite and verifies the
frontend build on every push to `main`/`develop` and on every pull request.
