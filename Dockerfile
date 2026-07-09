# syntax=docker/dockerfile:1

# ---- Build Stage (runs natively for speed; the JAR is architecture-independent) ----
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /workspace/app
COPY . .
RUN ./gradlew clean bootJar --no-daemon -x test
# Extract the executable jar into a CDS/AOT-friendly exploded layout (thin jar + lib/)
RUN java -Djarmode=tools -jar build/libs/app.jar extract --destination /workspace/extracted

# ---- AOT Training Stage (runs on the TARGET platform so the cache matches runtime arch) ----
FROM eclipse-temurin:25-jdk AS aot
WORKDIR /app
COPY --from=build /workspace/extracted/ ./
# Java 25 AOT cache (JEP 514): a single training run records class loading and
# method profiles, then writes the cache on exit. Spring exits after context
# refresh; database/Flyway are disabled so the run needs no external services.
RUN java -XX:AOTCacheOutput=app.aot \
      -Dspring.context.exit=onRefresh \
      -Dspring.flyway.enabled=false \
      -Dspring.sql.init.mode=never \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
      -jar app.jar

# ---- Run Stage (same base + same /app path so the AOT cache stays valid) ----
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=aot /app/ ./
EXPOSE 8082
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "app.jar"]
