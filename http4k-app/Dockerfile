FROM eclipse-temurin:21-jre-alpine

COPY build/libs/http4k-app.jar /app/http4k-app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/http4k-app.jar"]
