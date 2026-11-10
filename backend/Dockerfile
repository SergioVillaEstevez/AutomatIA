# Imagen de producción del API. Render la construye desde este archivo.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update \
  && apt-get install -y --no-install-recommends postgresql-client ca-certificates python3 \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --create-home --uid 10001 neiver
COPY --from=build /src/target/neiver-labs-0.1.0.jar /app/app.jar
COPY scripts/render-entrypoint.sh /app/render-entrypoint.sh
RUN chmod +x /app/render-entrypoint.sh && chown -R neiver:neiver /app
USER neiver
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/app/render-entrypoint.sh"]
