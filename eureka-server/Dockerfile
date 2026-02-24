FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring

COPY target/eureka-server-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8761/actuator/health || exit 1

LABEL authors="Admin"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom \
  -jar /app/app.jar"]