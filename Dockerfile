# syntax=docker/dockerfile:1

# ---- Build Stage ----
FROM gradle:8.7.0-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:21-jre-alpine
VOLUME /tmp
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8002
USER 1000:1000
ENTRYPOINT ["java","-jar","app.jar"] 