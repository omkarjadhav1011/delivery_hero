# Backend image, built on the server from the official multi-architecture image (DEC-137, DEC-151).
# PIN: replace the FROM line with the eclipse-temurin:21-jre@sha256:... line from scripts/pin-images.sh
FROM eclipse-temurin:21-jre

# curl serves the container health check and the deploy script's lock check
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --home-dir /app app

WORKDIR /app
COPY app.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
