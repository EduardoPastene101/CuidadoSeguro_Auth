# Etapa 1: Build con Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY jwt-common ./jwt-common
COPY src ./src
RUN mvn install -f jwt-common/pom.xml -DskipTests -q
RUN mvn clean package -DskipTests

# Etapa 2: Runtime con OpenJDK
FROM openjdk:17-jdk-slim

# Instalar dependencias necesarias
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Crear usuario no root para seguridad
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/auth-service-1.0.0.jar app.jar

# Crear directorio para logs
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Cambiar al usuario no root
USER appuser

# Exponer puerto
EXPOSE 8081

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/auth/health || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
