# ---------- Build stage ----------
FROM amazoncorretto:17-alpine-jdk AS builder
WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test

# ---------- Run stage ----------
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]