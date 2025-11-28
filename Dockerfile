# Dockerfile para Spring Boot Backend
# Multi-stage build para optimizar tamaño de imagen

# STAGE 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar archivos de Maven
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Dar permisos de ejecución a mvnw
RUN chmod +x mvnw

# Descargar dependencias (se cachea esta capa)
RUN ./mvnw dependency:go-offline

# Copiar código fuente
COPY src ./src

# Compilar aplicación
RUN ./mvnw clean package -DskipTests

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Crear carpeta para uploads antes de cambiar de usuario
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads

USER spring:spring

# Copiar JAR compilado desde stage anterior
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Variables de entorno por defecto (se sobreescriben con docker-compose)
ENV SPRING_PROFILES_ACTIVE=prod

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/home/data || exit 1

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
