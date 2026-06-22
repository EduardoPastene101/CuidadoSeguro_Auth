# ─── Etapa 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Instalar jwt-common en el repositorio Maven local (dependencia local)
COPY jwt-common ./jwt-common
RUN mvn install -f jwt-common/pom.xml -DskipTests -q

# Descargar dependencias del proyecto principal (cacheado si pom.xml no cambia)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilar
COPY src ./src
RUN mvn clean package -DskipTests

# ─── Etapa 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

RUN apt-get update && apt-get install -y curl wget && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY --from=build /app/target/auth-service-1.0.0.jar app.jar

RUN mkdir -p /app/logs && chown -R appuser:appuser /app

USER appuser

EXPOSE 8081

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/auth/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
