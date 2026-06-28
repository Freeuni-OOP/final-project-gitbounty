FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
# Copy the manifest first to preserve standard layer caching rules
COPY pom.xml .

# OPTIMIZATION: Mount the native .m2 folder to host storage.
# This prevents downloading dependencies from the internet on code changes.
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY src ./src

# OPTIMIZATION: Use the cache mount here too. Maven will reuse the .m2 folder
# AND skip compiling unchanged .class files, dropping incremental build times to seconds.
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]