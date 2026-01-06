FROM gradle:8.5-jdk17 AS builder
WORKDIR /build

COPY . .

RUN gradle clean build -x test

FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]