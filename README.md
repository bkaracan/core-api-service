# Core REST API Service

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1%20GA-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Kurumsal ölçekte, yüksek performanslı ve gözlemlenebilir Spring Boot REST API mikroservisi.

---

## 🛠 Teknoloji Yığını & Standartlar
- **Çalışma Zamanı (Runtime):** Java 21 LTS (Virtual Threads - Project Loom)
- **Framework:** Spring Boot 4.1.1 GA (Spring Framework 7 & Jakarta EE 11)
- **Veritabanı & ORM:** PostgreSQL 16+ & Spring Data JPA (Hibernate)
- **Veritabanı Sürümleme:** Flyway Migration (`V1__init_schema.sql`)
- **API Dokümantasyonu:** Springdoc OpenAPI v3 (Swagger UI)
- **Gözlemlenebilirlik:** Spring Boot Actuator & Micrometer Prometheus

---

## 🚀 Yerel Kurulum & İlk Çalıştırma

### 1. Ön Gereksinimler
- JDK 21 LTS kurulu olmalı (`java -version`)
- Docker Desktop / Docker Engine çalışır durumda olmalı

### 2. Ortam Değişkenlerini Hazırlama (.env)
```bash
# .env.example şablonundan yerel .env dosyasını oluşturun:
# Windows CMD:
copy .env.example .env

# Linux / macOS / Git Bash:
cp .env.example .env
```

### 3. Veritabanını Başlatma
```bash
# Docker Compose ile PostgreSQL 16 konteynerini başlatın:
docker compose up -d
```

### 4. Uygulamayı Çalıştırma
```bash
# Linux / macOS / Git Bash:
./mvnw spring-boot:run

# Windows CMD / PowerShell:
mvnw.cmd spring-boot:run
```

---

## 🔍 Canlı Doğrulama & Endpoint Bağlantıları
- **Swagger UI v3 (Interaktif API):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI 3.1 Spec (JSON):** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator Health Probe:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Prometheus Metrikleri:** [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)

---

## 🧪 Testleri Çalıştırma
Testler gerçek PostgreSQL üzerinde Testcontainers kullanılarak koşulur:
```bash
./mvnw clean verify
```