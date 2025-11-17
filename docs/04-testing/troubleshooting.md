# Troubleshooting Guide

This document provides solutions to common issues encountered in the 14logis project.

**Last Updated**: 2025-11-06

---

## Authentication & Authorization

### Problem: FeignClient call fails with 401 Unauthorized

**Symptoms**:
- Service-to-service calls return 401
- Gateway routes correctly but downstream service rejects request

**Root Cause**: Missing or invalid authentication headers

**Solutions**:

1. **Gateway must forward authentication headers**:
```yaml
# gateway-service/application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: notification-service
          uri: lb://NOTIFICATION-SERVICE
          predicates:
            - Path=/api/v1/notifications/**
          filters:
            - AddRequestHeader=X-User-Id, ${user.id}
            - AddRequestHeader=X-User-Role, ${user.role}
```

2. **Services must trust gateway headers** (NOT JWT):
```java
@Configuration
public class SecurityConfig extends SecurityConfigBase {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(new HeaderAuthFilter(),
                UsernamePasswordAuthenticationFilter.class);
    }
}
```

3. **For internal service calls, use FeignClient with proper config**:
```java
@FeignClient(name = "notification-service",
             configuration = FeignClientConfig.class)
public interface NotificationServiceClient {
    @PostMapping("/api/v1/notifications/orders")
    NotificationResponse createNotification(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Role") String role,
        @RequestBody OrderNotificationRequest request
    );
}
```

---

## Database & JPA

### Problem: Soft delete records appear in search results

**Symptoms**:
- Deleted entities returned by `findAll()` or custom queries
- Test assertions fail due to unexpected deleted records

**Root Cause**: Queries not filtering by `deleted_at IS NULL`

**Solutions**:

1. **Use @SQLRestriction on entity** (Recommended):
```java
@Entity
@Table(name = "p_notifications")
@SQLRestriction("deleted_at IS NULL")
public class Notification extends BaseEntity {
    // fields...
}
```

2. **Add filter to repository query**:
```java
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientSlackId(String recipientSlackId);
    // JPA automatically adds "deleted_at IS NULL" if @SQLRestriction present

    // OR manual query
    @Query("SELECT n FROM Notification n WHERE n.recipientSlackId = :slackId AND n.deletedAt IS NULL")
    List<Notification> findActiveByRecipientSlackId(@Param("slackId") String slackId);
}
```

3. **Use custom delete method** (never physical delete):
```java
public void delete(Notification notification) {
    notification.markAsDeleted(getCurrentUsername());
    save(notification);
}
```

---

### Problem: LazyInitializationException

**Symptoms**:
```
org.hibernate.LazyInitializationException: could not initialize proxy - no Session
```

**Root Cause**: Accessing lazy-loaded entity outside transaction scope

**Solutions**:

1. **Add @Transactional to service method**:
```java
@Service
public class NotificationService {
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID id) {
        Notification notification = repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Notification not found"));

        // Access lazy fields within transaction
        notification.getExternalApiLog(); // OK

        return NotificationResponse.from(notification);
    }
}
```

2. **Use fetch join in query**:
```java
@Query("SELECT n FROM Notification n LEFT JOIN FETCH n.externalApiLog WHERE n.id = :id")
Optional<Notification> findByIdWithApiLog(@Param("id") UUID id);
```

3. **Enable lazy loading in view (not recommended)**:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        enable_lazy_load_no_trans: true  # NOT recommended for production
```

---

## Microservices Communication

### Problem: Circular dependency between services

**Symptoms**:
- Service A calls B, B calls A → deadlock
- Startup fails with circular bean dependency

**Root Cause**: Bidirectional synchronous calls between services

**Solutions**:

1. **Refactor to unidirectional dependency**:
```
Before: order-service ↔ delivery-service
After:  order-service → delivery-service (one-way)
```

2. **Use event-driven approach** (Recommended):
```java
// order-service publishes event
@Service
public class OrderService {
    @Autowired
    private KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void createOrder(OrderRequest request) {
        Order order = // create order...
        orderRepository.save(order);

        // Publish event (no circular call)
        kafkaTemplate.send("order-created", new OrderCreatedEvent(order));
    }
}

// delivery-service consumes event
@Service
public class DeliveryEventListener {
    @KafkaListener(topics = "order-created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Create delivery asynchronously
        deliveryService.createDelivery(event);
    }
}
```

3. **Introduce intermediary service**:
```
order-service → orchestration-service → delivery-service
```

---

### Problem: Transaction rollback doesn't work across services

**Symptoms**:
- Order created but delivery creation fails
- Data inconsistency between services

**Root Cause**: MSA architecture doesn't support distributed ACID transactions

**Solutions**:

1. **Implement Saga pattern** (Recommended):
```java
@Service
public class OrderOrchestrationService {

    public void createOrder(OrderRequest request) {
        try {
            // Step 1: Create order
            Order order = orderService.createOrder(request);

            // Step 2: Create delivery
            Delivery delivery = deliveryService.createDelivery(order);

            // Step 3: Send notification
            notificationService.sendNotification(order, delivery);

        } catch (DeliveryCreationException e) {
            // Compensating transaction: Cancel order
            orderService.cancelOrder(order.getId(), "Delivery creation failed");
            throw e;
        }
    }
}
```

2. **Use eventual consistency with retries**:
```java
@Retryable(
    value = {DeliveryServiceException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 2000, multiplier = 2)
)
public Delivery createDeliveryWithRetry(Order order) {
    return deliveryClient.createDelivery(order);
}
```

3. **Implement outbox pattern** (Advanced):
- Save event to local DB in same transaction as entity
- Background process publishes events to message queue
- Guarantees at-least-once delivery

---

## External API Integration

### Problem: Slack API rate limit exceeded

**Symptoms**:
```json
{
  "ok": false,
  "error": "rate_limited",
  "retry_after": 60
}
```

**Root Cause**: Too many API calls in short time

**Solutions**:

1. **Implement exponential backoff**:
```java
@Configuration
public class Resilience4jConfig {

    @Bean
    public Retry slackRetry() {
        return Retry.of("slack", RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .intervalFunction(IntervalFunction.ofExponentialBackoff(
                Duration.ofSeconds(2), 2.0
            ))
            .retryOnException(e -> e instanceof SlackRateLimitException)
            .build());
    }
}
```

2. **Check Retry-After header**:
```java
public SlackResponse sendMessage(String channel, String message) {
    try {
        return slackClient.send(channel, message);
    } catch (WebClientResponseException.TooManyRequests e) {
        int retryAfter = Integer.parseInt(
            e.getHeaders().getFirst("Retry-After")
        );
        Thread.sleep(retryAfter * 1000);
        return slackClient.send(channel, message); // Retry
    }
}
```

3. **Implement message queue**:
- Buffer Slack messages in Kafka/RabbitMQ
- Process with controlled rate (e.g., 1 msg/second)

---

### Problem: Gemini API returns 400 Bad Request

**Symptoms**:
```json
{
  "error": {
    "code": 400,
    "message": "Invalid request"
  }
}
```

**Root Causes & Solutions**:

1. **Invalid JSON format**:
```java
// ❌ Wrong: String with quotes
String prompt = "\"Calculate departure time\"";

// ✅ Correct: Plain string
String prompt = "Calculate departure time";
```

2. **Missing required fields**:
```java
GeminiRequest request = GeminiRequest.builder()
    .contents(List.of(new Content(prompt)))
    .generationConfig(GenerationConfig.builder()
        .temperature(0.7)
        .maxOutputTokens(1024)
        .build())
    .build();
```

3. **API key invalid or expired**:
```bash
# Test API key
curl https://generativelanguage.googleapis.com/v1/models \
  -H "Authorization: Bearer YOUR_API_KEY"
```

---

## Docker & Docker Compose

### Problem: Service cannot connect to PostgreSQL

**Symptoms**:
```
Connection to localhost:5432 refused
```

**Root Cause**: Using `localhost` instead of Docker service name

**Solution**:

```yaml
# ❌ Wrong (application.yml)
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/oneforlogis_notification_db

# ✅ Correct (use Docker service name)
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/oneforlogis_notification_db
```

**Docker Compose**:
```yaml
services:
  postgres:
    image: postgres:17-alpine
    # ...

  notification-service:
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/oneforlogis
```

---

### Problem: Port already in use

**Symptoms**:
```
Error starting userland proxy: listen tcp 0.0.0.0:8700: bind: address already in use
```

**Solutions**:

1. **Find and kill process**:
```bash
# Windows
netstat -ano | findstr :8700
taskkill /PID <PID> /F

# Linux/Mac
lsof -i :8700
kill -9 <PID>
```

2. **Change port in docker-compose.yml**:
```yaml
services:
  notification-service:
    ports:
      - "8701:8700"  # Map to different host port
```

3. **Stop all Docker containers**:
```bash
docker-compose down
docker ps -a  # Verify all stopped
```

---

## Testing Issues

### Problem: Test fails due to Redis not running

**Symptoms**:
```
Connection refused: localhost:6379
```

**Solutions**:

1. **Use embedded Redis for tests**:
```groovy
// build.gradle
testImplementation 'it.ozimov:embedded-redis:0.7.3'
```

```java
@TestConfiguration
public class EmbeddedRedisConfig {
    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        redisServer = new RedisServer(6370); // Different port
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        redisServer.stop();
    }
}
```

2. **Disable Redis in test profile**:
```yaml
# application-test.yml
spring:
  cache:
    type: none  # Disable caching
```

3. **Mock cache service**:
```java
@MockBean
private HubCacheService hubCacheService;
```

---

## Build & Dependency Issues

### Problem: Gradle build fails with "Cannot resolve dependency"

**Symptoms**:
```
Could not resolve com.oneforlogis:common-lib:0.0.1-SNAPSHOT
```

**Solutions**:

1. **Build common-lib first**:
```bash
./gradlew :common-lib:build :common-lib:publishToMavenLocal
```

2. **Clean and rebuild**:
```bash
./gradlew clean build --refresh-dependencies
```

3. **Check settings.gradle includes common-lib**:
```groovy
rootProject.name = 'oneforlogis'
include 'common-lib'
include 'notification-service'
// ...
```

---

## Performance Issues

### Problem: Slow API response time

**Diagnosis**:

1. **Enable SQL logging**:
```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
```

2. **Check for N+1 queries**:
```java
// ❌ N+1 problem
List<Order> orders = orderRepository.findAll();
orders.forEach(order -> {
    order.getItems().size(); // Lazy load → N queries
});

// ✅ Solution: Use fetch join
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items")
List<Order> findAllWithItems();
```

3. **Add database indexes**:
```sql
CREATE INDEX idx_notifications_recipient
ON p_notifications(recipient_slack_id, deleted_at);
```

4. **Use caching for rarely-changing data**:
```java
@Cacheable(value = "hubs", key = "#hubId")
public Hub getHub(UUID hubId) {
    return hubRepository.findById(hubId)
        .orElseThrow(() -> new NotFoundException("Hub not found"));
}
```

---

## Security Issues

### Problem: Sensitive data exposed in logs

**Symptoms**: API keys, passwords visible in application logs

**Solutions**:

1. **Mask sensitive fields in DTOs**:
```java
@Data
public class SlackResponse {
    private String channel;

    @JsonIgnore  // Never serialize
    private String token;

    @Override
    public String toString() {
        return "SlackResponse{channel='" + channel + "', token='***'}";
    }
}
```

2. **Configure logback to filter patterns**:
```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="ch.qos.logback.classic.PatternLayout">
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </layout>
            <encoder>
                <pattern>%replace(%msg){'(password|token|apiKey)=[^,\s]+', '$1=***'}%n</pattern>
            </encoder>
        </encoder>
    </appender>
</configuration>
```

3. **Use ApiLogDomainService with masking**:
```java
private String maskSensitiveData(String data) {
    return data.replaceAll(
        "(\"(password|token|apiKey)\"\\s*:\\s*\")([^\"]+)(\")",
        "$1***$4"
    );
}
```

---

## Common Error Codes

| Code | Service | Meaning | Solution |
|------|---------|---------|----------|
| 1001 | notification | Invalid sender type | Ensure USER messages have sender info |
| 1002 | notification | Recipient not found | Verify Slack ID exists in user-service |
| 2001 | order | Insufficient inventory | Check product stock before order creation |
| 2002 | order | Invalid company | Verify supplier/receiver company exists |
| 3001 | delivery | No available staff | Assign more delivery personnel |
| 3002 | delivery | Invalid route | Verify hub IDs in route |

---

## Getting Help

1. **Check logs**: `docker-compose logs -f <service-name>`
2. **Search docs**: Use Ctrl+F in docs/ folder
3. **Ask team**: Post in Slack #tech-support channel
4. **Update this doc**: Add new issues and solutions you discover!
