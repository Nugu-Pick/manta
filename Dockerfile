FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew bootJar --no-daemon --console=plain

FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

RUN useradd --system --uid 10001 --create-home manta

COPY --from=builder --chown=manta:manta /workspace/build/libs/*.jar app.jar
COPY --chown=manta:manta build/api-spec/openapi3.yaml build/api-spec/openapi3.yaml

USER manta

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
