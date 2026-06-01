FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="daydayup"

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY target/daydayup-gateway.jar app.jar

EXPOSE 9000

USER app
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
