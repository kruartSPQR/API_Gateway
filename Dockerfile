FROM gradle:jdk17-alpine AS builder

WORKDIR /opt/app

COPY . .

RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /opt/app/build/libs/*.jar ApiGateway.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "ApiGateway.jar"]
