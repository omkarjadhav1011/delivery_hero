# syntax=docker/dockerfile:1
# Local and CI backend image, built from source (document 18).
# Production uses deploy/backend/Dockerfile with the jar that GitHub Actions builds.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src/backend
COPY backend/ ./
# Tests and formatting checks run in CI and the pre-push hook, not in this image build
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw -B -q -Dmaven.test.skip=true -Dspotless.check.skip=true package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app app
WORKDIR /app
COPY --from=build /src/backend/target/delivery-hero.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
