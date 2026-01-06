FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY build/libs/Newsugar_Back-*.jar app.jar
CMD ["java", "-jar", "app.jar"]
EXPOSE 8080