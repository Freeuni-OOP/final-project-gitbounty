# GitBounty Backend

A simple Spring Boot backend for the GitBounty project.

## Requirements

- Java 17+
- Docker and Docker Compose (optional, if you want to run in a container)
- Maven Wrapper is included, so you do not need Maven installed globally

## Project overview

The app exposes:

- `GET /health` → returns `Server is running!`

Git repositories are served under `GET /git/*` and are stored in `repositories/` by default.
When running in Docker, that folder is mounted to `/app/repositories`.
You can override the location with `GIT_REPOSITORIES_ROOT`.

The backend is currently configured to run on port `8081`.

## Start the database service

From the project root, start the shared MySQL service:

```bash
docker compose up -d mysql
```

Check status:

```bash
docker compose ps
```

Stop the database service:

```bash
docker compose stop mysql
```

## Run locally

From the project root:

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8081/health
```

Or test it from the terminal:

```bash
curl http://localhost:8081/health
```

## Run with Docker

Build and start the container with Docker Compose:

```bash
docker compose up --build
```

This starts the backend, the shared MySQL service, and Keycloak. Flyway creates the `keycloak_dev` database and user inside the same MySQL instance before Keycloak connects.

Keycloak is started with the imported `gitbounty` realm from `keycloak/import/gitbounty-realm.json`, which includes a demo client and user for local testing:

- client id: `gitbounty-backend`
- client secret: `gitbounty-backend-secret`
- demo user: `gitbounty-user`
- demo password: `gitbounty123`

This publishes the app on port `8081`, so you can open:

```text
http://localhost:8081/health
```

To stop the container:

```bash
docker compose down
```

## Build a jar

If you want to package the app without running it immediately:

```bash
./mvnw clean package -DskipTests
```

The jar will be created in `target/`.

## Run tests

All tests require MySQL to be running:

```bash
docker compose up -d mysql
./mvnw test
```

This runs:
- `BackendApplicationTests` — Context and configuration verification
- `DatabaseConnectivityTests` — MySQL connectivity verification

## Troubleshooting

- If port `8081` is already in use, stop the other process or change the configured port in `src/main/resources/application.properties`.
- If `./mvnw` is not executable, run:

```bash
chmod +x mvnw
```

# Registering an account in Keycloak
http://localhost:8080/realms/gitbounty/account/

