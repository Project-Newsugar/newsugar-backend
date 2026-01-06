# ---------- Build stage ----------
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY . .
RUN gradle clean bootJar -x test

# ---------- Run stage ----------
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]