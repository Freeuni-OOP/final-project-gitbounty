# GitBounty Backend

A simple Spring Boot backend for the GitBounty project.

## Requirements

- Java 17+
- Maven Wrapper is included, so you do not need Maven installed globally

## Project overview

The app exposes:

- `GET /health` → returns `Server is running!`

Git repositories are served under `GET /git/*`.
When running in Docker, the storage folder is mounted to `/app/repositories`.
You can override the location with `GIT_REPOSITORIES_ROOT`.

The backend is currently configured to run on port `8081`.

## Run locally

From the project root:

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8081/health
```

