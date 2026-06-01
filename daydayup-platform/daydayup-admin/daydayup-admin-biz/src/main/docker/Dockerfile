FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="daydayup"

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY target/daydayup-admin-biz.jar app.jar

EXPOSE 9100

USER app
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
