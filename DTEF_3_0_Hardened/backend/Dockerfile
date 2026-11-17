FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# JUnit/Testcontainers run in the dedicated CI job with the host Docker daemon.
# Compile tests here, but do not start containers from inside a Docker build.
RUN mvn -B -q -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 dtef \
    && useradd --system --uid 10001 --gid dtef --home-dir /app dtef \
    && mkdir -p /var/lib/dtef/uploads \
    && chown -R dtef:dtef /app /var/lib/dtef
COPY --from=build --chown=dtef:dtef /workspace/target/dtef-3.0.0.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=4s --start-period=45s --retries=5 CMD curl -fsS http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
