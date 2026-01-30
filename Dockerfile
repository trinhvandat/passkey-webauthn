# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper và pom.xml trước để cache dependencies
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Cấp quyền execute cho mvnw
RUN chmod +x mvnw

# Download dependencies (layer này được cache nếu pom.xml không đổi)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build ứng dụng, skip tests để build nhanh hơn
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app

# Tạo non-root user để tăng security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/bash appuser

# Copy JAR file từ builder stage
COPY --from=builder /app/target/*.jar app.jar

# Chuyển sang non-root user
USER appuser

# Expose port (mặc định Spring Boot)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning cho container
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Entrypoint
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
