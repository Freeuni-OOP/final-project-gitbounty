# GitBounty Backend

A simple Spring Boot backend for the GitBounty project.

## Requirements

- Java 17+
- Docker and Docker Compose (optional, if you want to run in a container)
- Maven Wrapper is included, so you do not need Maven installed globally

## Project overview

The app exposes a basic health endpoint:

- `GET /health` → returns `Server is running!`

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

Or test it from the terminal:

```bash
curl http://localhost:8081/health
```

## Run with Docker

Build and start the container with Docker Compose:

```bash
docker compose up --build
```

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

## Troubleshooting

- If port `8081` is already in use, stop the other process or change the configured port in `src/main/resources/application.properties`.
- If `./mvnw` is not executable, run:

```bash
chmod +x mvnw
```

