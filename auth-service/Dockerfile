FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1001 spring && adduser -S spring -u 1001

COPY target/auth-service-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

HEALTHCHECK CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]