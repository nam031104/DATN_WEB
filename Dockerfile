# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Cache dependencies trước để build nhanh hơn
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source và build jar
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Chạy bằng user thường thay vì root
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
