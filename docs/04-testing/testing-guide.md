# Testing Guide

This document provides guidelines for writing and running tests in the 14logis project.

**Last Updated**: 2025-11-06

---

## Testing Strategy

### Test Pyramid

```
        /\
       /  \
      / E2E\     ← End-to-End (10%)
     /______\
    /        \
   /Integration\ ← Integration Tests (30%)
  /____________\
 /              \
/   Unit Tests   \ ← Unit Tests (60%)
/________________\
```

**Philosophy**:
- Most tests at Unit level (fast, isolated)
- Moderate Integration tests (DB, API, external services)
- Few E2E tests (critical user flows only)

---

## Unit Tests

### Service Layer Tests

**Focus**: Business logic in isolation

**Pattern**: Mock all dependencies

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SlackClientWrapper slackClient;

    @Mock
    private GeminiClientWrapper geminiClient;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("주문 알림 생성 - 성공")
    void createOrderNotification_Success() {
        // Given
        OrderNotificationRequest request = createValidRequest();
        when(geminiClient.calculateDepartureTime(any()))
            .thenReturn("2025-11-09T09:00:00");
        when(slackClient.sendMessage(any()))
            .thenReturn(new SlackResponse("ok", "C123", "T123"));

        // When
        NotificationResponse response = notificationService
            .createOrderNotification(request);

        // Then
        assertThat(response.getStatus()).isEqualTo(MessageStatus.SENT);
        verify(notificationRepository).save(any(Notification.class));
    }
}
```

### Domain Logic Tests

**Focus**: Entity validation and business rules

```java
@Test
@DisplayName("알림 생성 - USER 타입은 sender 정보 필수")
void createNotification_UserType_RequiresSenderInfo() {
    // Given
    Notification notification = Notification.builder()
        .senderType(SenderType.USER)
        .senderUsername(null) // Invalid!
        .recipientSlackId("U123")
        .messageContent("Test")
        .build();

    // When & Then
    assertThatThrownBy(() -> notification.validate())
        .isInstanceOf(NotificationException.class)
        .hasMessageContaining("sender_username is required");
}
```

---

## Integration Tests

### Repository Tests

**Focus**: JPA queries and database interactions

**Setup**: Use `@DataJpaTest` with H2 or TestContainers

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class NotificationRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
        .withDatabaseName("test_notification_db");

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("수신자 Slack ID로 알림 조회")
    void findByRecipientSlackId_ReturnsNotifications() {
        // Given
        Notification notification = createTestNotification();
        notificationRepository.save(notification);

        // When
        List<Notification> results = notificationRepository
            .findByRecipientSlackId("U123");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRecipientSlackId()).isEqualTo("U123");
    }
}
```

### Controller Tests (WebMvcTest)

**Focus**: HTTP layer, request/response validation

```java
@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    @DisplayName("주문 알림 API - 성공 (201 Created)")
    void createOrderNotification_ReturnsCreated() throws Exception {
        // Given
        OrderNotificationRequest request = createValidRequest();
        NotificationResponse response = createSuccessResponse();
        when(notificationService.createOrderNotification(any()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/notifications/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Id", "1")
                .header("X-User-Role", "HUB_MANAGER")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.messageId").exists())
            .andExpect(jsonPath("$.data.status").value("SENT"));
    }
}
```

### External API Tests (MockWebServer)

**Focus**: HTTP client behavior, retry logic

```java
@ExtendWith(MockitoExtension.class)
class SlackClientTest {

    private MockWebServer mockWebServer;
    private SlackClient slackClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();
        slackClient = new SlackClient(webClient, "test-token");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Slack API 호출 - 재시도 후 성공")
    void sendMessage_RetryOnFailure_Success() {
        // Given
        mockWebServer.enqueue(new MockResponse().setResponseCode(500)); // 1st fail
        mockWebServer.enqueue(new MockResponse().setResponseCode(500)); // 2nd fail
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"ok\":true,\"channel\":\"C123\"}")
            .setResponseCode(200)); // 3rd success

        // When
        SlackResponse response = slackClient.sendMessage("U123", "Test");

        // Then
        assertThat(response.isOk()).isTrue();
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }
}
```

---

## E2E Tests

### Critical Flow: Order Creation → Notification

**Focus**: Full integration across services

**Setup**: Docker Compose with all services running

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext
class OrderNotificationE2ETest {

    @Container
    static DockerComposeContainer<?> environment = new DockerComposeContainer<>(
        new File("docker-compose-test.yml")
    )
        .withExposedService("postgres", 5432)
        .withExposedService("eureka-server", 8761)
        .withExposedService("notification-service", 8700);

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("E2E: 주문 생성 → 알림 발송")
    void orderCreation_TriggersNotification() {
        // Step 1: Create order
        OrderCreateRequest orderRequest = createOrderRequest();
        ResponseEntity<OrderResponse> orderResponse = restTemplate
            .postForEntity("/api/v1/orders", orderRequest, OrderResponse.class);

        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Step 2: Verify notification was sent
        String orderId = orderResponse.getBody().getOrderId();
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                ResponseEntity<NotificationResponse[]> notifications = restTemplate
                    .getForEntity("/api/v1/notifications?referenceId=" + orderId,
                        NotificationResponse[].class);

                assertThat(notifications.getBody()).hasSize(1);
                assertThat(notifications.getBody()[0].getStatus())
                    .isEqualTo(MessageStatus.SENT);
            });
    }
}
```

---

## Test Data Management

### Test Fixtures

**Pattern**: Builder pattern for test data

```java
public class NotificationFixtures {

    public static Notification createUserNotification() {
        return Notification.builder()
            .senderType(SenderType.USER)
            .senderUsername("testuser")
            .senderSlackId("U123")
            .senderName("Test User")
            .recipientSlackId("U456")
            .recipientName("Recipient")
            .messageContent("Test message")
            .messageType(MessageType.MANUAL)
            .status(MessageStatus.PENDING)
            .build();
    }

    public static Notification createSystemNotification() {
        return Notification.builder()
            .senderType(SenderType.SYSTEM)
            .recipientSlackId("U456")
            .recipientName("Hub Manager")
            .messageContent("System notification")
            .messageType(MessageType.ORDER_NOTIFICATION)
            .referenceId(UUID.randomUUID())
            .status(MessageStatus.PENDING)
            .build();
    }
}
```

### Database Seeding

**Pattern**: SQL scripts or `@BeforeEach`

```java
@BeforeEach
void seedDatabase() {
    // Create test user
    User user = User.builder()
        .username("testuser")
        .role(Role.HUB_MANAGER)
        .hubId(UUID.fromString("..."))
        .status(UserStatus.APPROVED)
        .build();
    userRepository.save(user);

    // Create test hub
    Hub hub = Hub.builder()
        .name("경기남부")
        .address("...")
        .build();
    hubRepository.save(hub);
}
```

---

## Mocking Strategies

### FeignClient Mocking

**Pattern**: `@MockBean` for service tests

```java
@SpringBootTest
class OrderServiceIntegrationTest {

    @MockBean
    private CompanyServiceClient companyClient;

    @MockBean
    private ProductServiceClient productClient;

    @MockBean
    private NotificationServiceClient notificationClient;

    @Test
    void createOrder_CallsAllDependencies() {
        // Given
        when(companyClient.getCompany(any())).thenReturn(createValidCompany());
        when(productClient.checkInventory(any())).thenReturn(true);
        when(notificationClient.sendNotification(any())).thenReturn(createNotificationResponse());

        // When
        orderService.createOrder(orderRequest);

        // Then
        verify(companyClient, times(2)).getCompany(any()); // supplier + receiver
        verify(productClient).checkInventory(any());
        verify(notificationClient).sendNotification(any());
    }
}
```

### External API Mocking (Slack, Gemini)

**Local Tests**: Use MockWebServer

**Integration Tests**: Use WireMock

```java
@WireMockTest
class SlackIntegrationTest {

    @Test
    void sendSlackMessage_IntegrationTest() {
        // Given
        stubFor(post(urlEqualTo("/api/chat.postMessage"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"ok\":true,\"channel\":\"C123\"}")));

        // When
        SlackResponse response = slackClient.sendMessage("U123", "Test");

        // Then
        assertThat(response.isOk()).isTrue();
        verify(postRequestedFor(urlEqualTo("/api/chat.postMessage"))
            .withHeader("Authorization", equalTo("Bearer test-token")));
    }
}
```

---

## Test Configuration

### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

# Disable external API calls
slack:
  enabled: false

gemini:
  enabled: false

# Override bean for tests
spring:
  main:
    allow-bean-definition-overriding: true
```

### Test Profiles

```java
@ActiveProfiles("test")
@SpringBootTest
class MyIntegrationTest {
    // Test uses application-test.yml
}
```

---

## Performance Testing

### Load Testing with Gatling (Future)

```scala
class OrderCreationSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8000")
    .header("Authorization", "Bearer ${token}")

  val scn = scenario("Order Creation")
    .exec(http("Create Order")
      .post("/api/v1/orders")
      .body(StringBody("""{"items":[...]}""")).asJson
      .check(status.is(201)))

  setUp(
    scn.inject(rampUsers(100) during (60 seconds))
  ).protocols(httpProtocol)
}
```

---

## Test Coverage

### Target Coverage
- **Unit Tests**: 80%+ coverage
- **Integration Tests**: Critical paths covered
- **E2E Tests**: Happy path + 1-2 edge cases

### Measuring Coverage

```bash
# Gradle
./gradlew :notification-service:test jacocoTestReport

# View report
open notification-service/build/reports/jacoco/test/html/index.html
```

### Coverage Exclusions
- DTOs (simple getters/setters)
- Configuration classes
- Main application class
- Generated code

---

## Test Naming Conventions

**Pattern**: `methodName_condition_expectedResult`

**Examples**:
- `createNotification_ValidInput_ReturnsCreated()`
- `sendSlackMessage_ApiFailure_RetriesThreeTimes()`
- `findByRecipientSlackId_NoResults_ReturnsEmptyList()`

**Korean Alternative** (for better readability):
- `주문_알림_생성_성공()`
- `Slack_메시지_전송_실패시_3회_재시도()`

---

## Running Tests

### All Tests
```bash
./gradlew test
```

### Single Service
```bash
./gradlew :notification-service:test
```

### Single Test Class
```bash
./gradlew :notification-service:test --tests NotificationServiceTest
```

### Single Test Method
```bash
./gradlew :notification-service:test --tests NotificationServiceTest.createOrderNotification_Success
```

### Integration Tests Only
```bash
./gradlew :notification-service:integrationTest
```

### With Coverage
```bash
./gradlew :notification-service:test jacocoTestReport
```

---

## Best Practices

1. **Fast Tests**: Unit tests should run in < 1 second each
2. **Isolation**: Tests should not depend on each other
3. **Clarity**: Test names should describe what is being tested
4. **AAA Pattern**: Arrange, Act, Assert structure
5. **One Assertion**: Focus on one behavior per test (when possible)
6. **Clean Up**: Use `@AfterEach` to clean test data
7. **Realistic Data**: Use meaningful test data, not just "test123"
8. **Edge Cases**: Test boundary conditions and error scenarios
9. **No Hardcoded Values**: Use constants or test fixtures
10. **CI/CD**: All tests must pass before merge

---

## Future Testing Tools

- **Contract Testing**: Pact for API contracts between services
- **Chaos Engineering**: Chaos Monkey for resilience testing
- **Security Testing**: OWASP ZAP for vulnerability scanning
- **Performance**: JMeter or Gatling for load testing
