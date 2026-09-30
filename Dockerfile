# =====================================================================
# Stage 1: Build & Dependency Caching (Java 21 LTS)
# =====================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# =====================================================================
# Stage 2: Extract Spring Boot Layered Jar (Spring Boot 3.3+ / 4.x tools)
# =====================================================================
FROM eclipse-temurin:21-jre-jammy AS extractor
WORKDIR /workspace
COPY --from=builder /workspace/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# =====================================================================
# Stage 3: Minimal, Hardened Production Runtime
# =====================================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Güvenlik standardı: root olmayan kullanıcı oluştur
RUN groupadd -r spring && useradd -r -g spring spring && chown -R spring:spring /app
USER spring:spring

# Katmanları en az değişenden en sık değişene doğru kopyala (Docker Layer Caching)
COPY --from=extractor --chown=spring:spring /workspace/extracted/dependencies/ ./
COPY --from=extractor --chown=spring:spring /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /workspace/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.launch.JarLauncher"]