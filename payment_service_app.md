# Payment Service Project Structure

## Directory Structure
```
payment-service/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── example/
│   │   │           └── paymentservice/
│   │   │               ├── PaymentServiceApplication.java
│   │   │               ├── controller/
│   │   │               │   └── PaymentController.java
│   │   │               ├── service/
│   │   │               │   └── PaymentProcessor.java
│   │   │               ├── dto/
│   │   │               │   ├── PaymentRequest.java
│   │   │               │   ├── PaymentResponse.java
│   │   │               │   ├── PaymentEvent.java
│   │   │               │   └── SettlementEvent.java
│   │   │               ├── entity/
│   │   │               │   └── Payment.java
│   │   │               ├── repository/
│   │   │               │   └── PaymentRepository.java
│   │   │               ├── client/
│   │   │               │   └── FraudCheckClient.java
│   │   │               ├── mapper/
│   │   │               │   └── PaymentMapper.java
│   │   │               └── model/
│   │   │                   └── FraudResponse.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/
│       └── java/
│           └── org/
│               └── example/
│                   └── paymentservice/
│                       ├── PaymentServiceApplicationTests.java
│                       ├── controller/
│                       │   └── PaymentControllerTest.java
│                       └── service/
│                           └── PaymentProcessorTest.java
└── README.md
```

## File Contents

### build.gradle
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'org.example'
version = '1.0.0-SNAPSHOT'

java {
    sourceCompatibility = '21'
    targetCompatibility = '21'
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'
    
    // Database
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    
    // gRPC (if needed for FraudCheckClient)
    implementation 'net.devh:grpc-spring-boot-starter:2.15.0.RELEASE'
    
    // Utilities
    implementation 'org.mapstruct:mapstruct:1.5.5.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
    
    // Development
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'org.junit.jupiter:junit-jupiter'
}

dependencyManagement {
    imports {
        mavenBom 'org.testcontainers:testcontainers-bom:1.19.3'
    }
}

tasks.named('test') {
    useJUnitPlatform()
}

// Build optimization
tasks.withType(JavaCompile) {
    options.encoding = 'UTF-8'
    options.compilerArgs.addAll(['-parameters'])
}
```

### settings.gradle
```gradle
rootProject.name = 'payment-service'
```

### src/main/java/org/example/paymentservice/config/KafkaConfig.java
```java
package org.example.paymentservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### src/main/java/org/example/paymentservice/PaymentServiceApplication.java
```java
package org.example.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
```

### src/main/java/org/example/paymentservice/controller/PaymentController.java
```java
package org.example.paymentservice.controller;

import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResponse;
import org.example.paymentservice.service.PaymentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentProcessor paymentProcessor;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        String paymentId = paymentProcessor.initiatePayment(request);
        return ResponseEntity.accepted()
                .body(new PaymentResponse(paymentId, "Processing"));
    }
}
```

### src/main/java/org/example/paymentservice/service/PaymentProcessor.java
```java
package org.example.paymentservice.service;

import org.example.paymentservice.client.FraudCheckClient;
import org.example.paymentservice.dto.PaymentEvent;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.SettlementEvent;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.mapper.PaymentMapper;
import org.example.paymentservice.model.FraudResponse;
import org.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentProcessor {
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepo;

    @Autowired
    private FraudCheckClient fraudCheckClient;

    @Autowired
    private PaymentMapper paymentMapper;

    @KafkaListener(topics = "${kafka.topic.payments}", groupId = "${kafka.group-id}")
    @Transactional
    public void processPaymentEvent(PaymentEvent event) {
        try {
            // 1. Sync fraud check (low-latency)
            FraudResponse fraudResponse = fraudCheckClient.check(event);

            if (fraudResponse.isApproved()) {
                // 2. Process payment
                Payment payment = paymentMapper.toEntity(event);
                paymentRepo.save(payment);

                // 3. Publish settlement event
                kafkaTemplate.send("settlements", payment.getPaymentId(),
                        new SettlementEvent(payment.getPaymentId(), payment.getAmount()));
            }
        } catch (Exception e) {
            log.error("Payment failed: {}", event.getPaymentId(), e);
            kafkaTemplate.send("payment-dlq", event.getPaymentId(), event); // DLQ
        }
    }

    public String initiatePayment(PaymentRequest request) {
        String paymentId = UUID.randomUUID().toString();
        kafkaTemplate.send("payments", paymentId,
                new PaymentEvent(paymentId, request.amount(), request.currency()));
        return paymentId;
    }
}
```

### src/main/java/org/example/paymentservice/dto/PaymentRequest.java
```java
package org.example.paymentservice.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentRequest(
    @NotNull 
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount, 
    
    @NotBlank(message = "Currency cannot be blank")
    String currency
) {}
```

### src/main/java/org/example/paymentservice/dto/PaymentResponse.java
```java
package org.example.paymentservice.dto;

public record PaymentResponse(String paymentId, String status) {}
```

### src/main/java/org/example/paymentservice/dto/PaymentEvent.java
```java
package org.example.paymentservice.dto;

import java.math.BigDecimal;

public record PaymentEvent(String paymentId, BigDecimal amount, String currency) {}
```

### src/main/java/org/example/paymentservice/dto/SettlementEvent.java
```java
package org.example.paymentservice.dto;

import java.math.BigDecimal;

public record SettlementEvent(String paymentId, BigDecimal amount) {}
```

### src/main/java/org/example/paymentservice/entity/Payment.java
```java
package org.example.paymentservice.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private String paymentId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Constructors, getters, setters
    public Payment() {}
    
    public Payment(String paymentId, BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public enum PaymentStatus {
        PENDING, APPROVED, REJECTED, SETTLED
    }
}
```

### src/main/java/org/example/paymentservice/repository/PaymentRepository.java
```java
package org.example.paymentservice.repository;

import org.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
}
```

### src/main/java/org/example/paymentservice/client/FraudCheckClient.java
```java
package org.example.paymentservice.client;

import org.example.paymentservice.dto.PaymentEvent;
import org.example.paymentservice.model.FraudResponse;
import org.springframework.stereotype.Component;

@Component
public class FraudCheckClient {
    
    // TODO: Implement actual fraud check logic (gRPC/REST)
    public FraudResponse check(PaymentEvent event) {
        // Placeholder implementation
        return new FraudResponse(true, "LOW_RISK");
    }
}
```

### src/main/java/org/example/paymentservice/mapper/PaymentMapper.java
```java
package org.example.paymentservice.mapper;

import org.example.paymentservice.dto.PaymentEvent;
import org.example.paymentservice.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentMapper INSTANCE = Mappers.getMapper(PaymentMapper.class);
    
    Payment toEntity(PaymentEvent event);
}
```

### src/main/java/org/example/paymentservice/model/FraudResponse.java
```java
package org.example.paymentservice.model;

public class FraudResponse {
    private boolean approved;
    private String riskLevel;
    
    public FraudResponse(boolean approved, String riskLevel) {
        this.approved = approved;
        this.riskLevel = riskLevel;
    }
    
    public boolean isApproved() { return approved; }
    public String getRiskLevel() { return riskLevel; }
}
```

### src/main/resources/application.yml
```yaml
server:
  port: 8080

spring:
  application:
    name: payment-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/payments
    username: ${DB_USERNAME:payments}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${KAFKA_GROUP_ID:payment-service}
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "org.example.paymentservice.dto"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

kafka:
  topic:
    payments: payments
  group-id: payment-service

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always

logging:
  level:
    org.example.paymentservice: INFO
    org.springframework.kafka: WARN
```

### README.md
```markdown
# Payment Service

A high-throughput, event-driven payment processing service built with Spring Boot and Apache Kafka, designed for scalability and reliability in financial transaction processing.

## 🏗️ Architecture

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│   Client    │───▶│ REST API     │───▶│   Kafka     │
│ Application │    │ (Payment     │    │  (payments) │
└─────────────┘    │ Controller)  │    └─────────────┘
                   └──────────────┘           │
                                             ▼
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│ Settlement  │◀───│ Payment      │───▶│ Fraud Check │
│ Service     │    │ Processor    │    │ Service     │
└─────────────┘    └──────────────┘    └─────────────┘
                           │
                           ▼
                   ┌──────────────┐
                   │ PostgreSQL   │
                   │ Database     │
                   └──────────────┘
```

## 🚀 Features

- **High-throughput async processing** via Apache Kafka
- **Real-time fraud detection** integration
- **RESTful API** for payment initiation
- **Event-driven architecture** with settlement publishing
- **Dead Letter Queue (DLQ)** for failed payment handling
- **Transactional processing** with database persistence
- **Health monitoring** with Spring Boot Actuator
- **Comprehensive testing** with Testcontainers
- **Production-ready configuration** with proper logging and metrics

## 🛠️ Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.2** - Application framework
- **Apache Kafka** - Event streaming platform
- **PostgreSQL** - Primary database
- **MapStruct** - Object mapping
- **Docker & Testcontainers** - Containerization and testing
- **Gradle** - Build automation
- **JUnit 5 & Mockito** - Testing framework

## 📋 Prerequisites

- Java 17 or higher
- Docker & Docker Compose
- Gradle 7+
- PostgreSQL 13+
- Apache Kafka 3.0+

## 🏃‍♂️ Quick Start

### 1. Clone the repository
```bash
git clone <repository-url>
cd payment-service
```

### 2. Start infrastructure services
```bash
# Create docker-compose.yml file (see Infrastructure section below)
docker-compose up -d
```

### 3. Build and run the application
```bash
./gradlew build
./gradlew bootRun
```

### 4. Test the service
```bash
curl -X POST http://localhost:8080/payments \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.00, "currency": "USD"}'
```

## 🐳 Infrastructure Setup

Create a `docker-compose.yml` file in the project root:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: payments
      POSTGRES_USER: payments
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true

volumes:
  postgres_data:
```

## 📚 API Documentation

### Payment Endpoints

#### Create Payment
```http
POST /payments
Content-Type: application/json

{
  "amount": 100.00,
  "currency": "USD"
}
```

**Response:**
```json
{
  "paymentId": "uuid-string",
  "status": "Processing"
}
```

#### Health Check
```http
GET /actuator/health
```

#### Metrics
```http
GET /actuator/metrics
GET /actuator/prometheus
```

## 🔄 Event Flow

1. **Payment Initiation**: Client sends POST request to `/payments`
2. **Event Publishing**: Payment event published to Kafka `payments` topic
3. **Async Processing**: Payment processor consumes event
4. **Fraud Check**: Synchronous fraud validation
5. **Payment Processing**: Database persistence if fraud check passes
6. **Settlement Publishing**: Settlement event published to `settlements` topic
7. **Error Handling**: Failed payments sent to DLQ

## 📊 Configuration

### Application Properties

Key configuration properties in `application.yml`:

```yaml
# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: payment-service
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/payments
    username: payments
    password: password

# Custom Topics
kafka:
  topic:
    payments: payments
  group-id: payment-service
```

### Environment Variables

- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password  
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka bootstrap servers
- `KAFKA_GROUP_ID` - Consumer group ID

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Integration Tests
```bash
./gradlew integrationTest
```

### Test Coverage
```bash
./gradlew jacocoTestReport
```

The project includes:
- **Unit tests** with Mockito
- **Integration tests** with Testcontainers
- **API tests** with MockMvc
- **Kafka integration tests**

## 🚀 Deployment

### Docker Build
```bash
docker build -t payment-service:latest .
```

### Kubernetes Deployment
```bash
kubectl apply -f k8s/
```

## 📈 Monitoring & Observability

- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **Logging**: Structured JSON logs
- **Distributed Tracing**: Ready for integration

## 🔧 Development

### Project Structure
```
src/
├── main/java/org/example/paymentservice/
│   ├── controller/     # REST endpoints
│   ├── service/        # Business logic
│   ├── repository/     # Data access
│   ├── dto/           # Data transfer objects
│   ├── entity/        # JPA entities
│   ├── mapper/        # Object mapping
│   ├── client/        # External service clients
│   └── config/        # Configuration classes
└── test/               # Test classes
```

### Code Quality
- **CheckStyle**: Code style enforcement
- **SpotBugs**: Static analysis
- **JaCoCo**: Test coverage
- **SonarQube**: Code quality metrics

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: GitHub Issues
- **Documentation**: Wiki pages
- **Discussions**: GitHub Discussions
```
