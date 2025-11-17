# [Challenge] 일일 경로 최적화 알림 기능

## 📝 Description
- 매일 06:00 업체 배송 담당자에게 최적화된 배송 순서 알림 발송
- ChatGPT API를 활용한 TSP(Traveling Salesman Problem) 해결
- Naver Maps Directions 5 API 연동하여 실제 경로 및 시간 계산
- 최적화된 경로 정보를 p_company_delivery_routes 테이블에 저장

## ⭐ To-do
- [ ] Spring Scheduler 구성 (@Scheduled, cron 표현식)
- [ ] 발송 시각 설정 파일 관리 (application.yml)
- [ ] delivery-service FeignClient 구현 (당일 배송 목록 조회)
- [ ] ChatGPT API 클라이언트 구현
- [ ] ChatGPT TSP 프롬프트 설계 및 테스트
- [ ] 최적 방문 순서 파싱 로직
- [ ] Naver Maps Directions 5 API 클라이언트 구현
- [ ] waypoints 파라미터 구성 로직
- [ ] p_company_delivery_routes 테이블 엔티티 생성
- [ ] 경로 정보 저장 로직
- [ ] ChatGPT 기반 메시지 생성
- [ ] Slack 알림 발송
- [ ] 테스트 코드 작성 (스케줄러 동작 확인)
- [ ] API 호출 로깅 (p_external_api_logs)

## ✅ ETC
- **API 키 필요**:
  - OPENAI_API_KEY (ChatGPT)
  - NAVER_MAPS_CLIENT_ID
  - NAVER_MAPS_CLIENT_SECRET
  - SLACK_BOT_TOKEN
- 테스트 시 cron 표현식 변경 가능하도록 설정
- ChatGPT 무료 플랜 제한 확인 필요
- 예정 일정: 4-5일 소요

---

## 기술 스택

| 기술 | 용도 | 비고 |
|------|------|------|
| Spring Scheduler | 매일 06:00 자동 실행 | @Scheduled |
| ChatGPT API | TSP 해결, 메시지 생성 | gpt-4 또는 gpt-3.5-turbo |
| Naver Maps API | 경로 및 시간 계산 | Directions 5 API |
| Slack API | 알림 발송 | chat.postMessage |
| PostgreSQL | 경로 정보 저장 | p_company_delivery_routes |

---

## 구현 상세 계획

### 1. Spring Scheduler 설정

**application.yml**:
```yaml
scheduler:
  daily-route-notification:
    cron: "0 0 6 * * ?"  # 매일 06:00
    enabled: true
    timezone: Asia/Seoul
```

**스케줄러 클래스**:
```java
@Component
@EnableScheduling
public class DailyRouteScheduler {

    private final DailyRouteNotificationService notificationService;

    @Scheduled(cron = "${scheduler.daily-route-notification.cron}")
    @ConditionalOnProperty(
        value = "scheduler.daily-route-notification.enabled",
        havingValue = "true",
        matchIfMissing = false
    )
    public void sendDailyRouteNotifications() {
        log.info("일일 경로 최적화 알림 스케줄러 시작");
        notificationService.processAndSendDailyRoutes();
    }
}
```

---

### 2. ChatGPT API 연동

#### 2.1 의존성 추가 (build.gradle)
```gradle
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

#### 2.2 ChatGPT API 클라이언트
```java
@Service
public class ChatGptApiClient {

    @Value("${chatgpt.api-key}")
    private String apiKey;

    @Value("${chatgpt.model}")
    private String model; // gpt-4 or gpt-3.5-turbo

    private static final String CHATGPT_API_URL =
        "https://api.openai.com/v1/chat/completions";

    public String generateResponse(String prompt) {
        // OpenAI API 호출 로직
        // RestTemplate 또는 WebClient 사용
    }
}
```

#### 2.3 application.yml 설정
```yaml
chatgpt:
  api-key: ${OPENAI_API_KEY}
  model: gpt-3.5-turbo  # 또는 gpt-4
  max-tokens: 1000
  temperature: 0.7
```

---

### 3. ChatGPT TSP 프롬프트 설계

#### 3.1 배송 순서 최적화 프롬프트
```java
public String buildTspPrompt(HubLocation hub, List<DeliveryDestination> destinations) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("당신은 배송 경로 최적화 전문가입니다. ");
    prompt.append("다음 배송지들의 최적 방문 순서를 계산해주세요.\n\n");

    prompt.append("[출발지]\n");
    prompt.append(String.format("%s (%.6f, %.6f)\n\n",
        hub.getName(), hub.getLatitude(), hub.getLongitude()));

    prompt.append("[배송지 목록]\n");
    for (int i = 0; i < destinations.size(); i++) {
        DeliveryDestination dest = destinations.get(i);
        prompt.append(String.format("%d. %s: (%.6f, %.6f)\n",
            i + 1, dest.getCompanyName(), dest.getLatitude(), dest.getLongitude()));
    }

    prompt.append("\n[제약 조건]\n");
    prompt.append("- 09:00 출발, 18:00까지 복귀\n");
    prompt.append("- 각 배송지 체류 시간: 20분\n");
    prompt.append("- 최단 거리 우선\n\n");

    prompt.append("최적 방문 순서를 번호로만 반환해주세요.\n");
    prompt.append("형식 예시: 3,1,4,2");

    return prompt.toString();
}
```

**ChatGPT 응답 예시**:
```
3,1,4,2
```

#### 3.2 응답 파싱 로직
```java
public List<Integer> parseOptimizedOrder(String chatGptResponse) {
    // "3,1,4,2" -> [3, 1, 4, 2]
    return Arrays.stream(chatGptResponse.trim().split(","))
        .map(String::trim)
        .map(Integer::parseInt)
        .collect(Collectors.toList());
}
```

---

### 4. Naver Maps Directions 5 API 연동

#### 4.1 API 엔드포인트
```
GET https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving
```

#### 4.2 요청 파라미터
```java
public NaverMapsResponse calculateRoute(
    Location start,
    Location goal,
    List<Location> waypoints
) {
    String waypointsParam = waypoints.stream()
        .map(loc -> String.format("%.6f,%.6f", loc.getLongitude(), loc.getLatitude()))
        .collect(Collectors.joining("|"));

    UriComponents uri = UriComponentsBuilder
        .fromHttpUrl("https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving")
        .queryParam("start", String.format("%.6f,%.6f", start.getLongitude(), start.getLatitude()))
        .queryParam("goal", String.format("%.6f,%.6f", goal.getLongitude(), goal.getLatitude()))
        .queryParam("waypoints", waypointsParam)
        .queryParam("option", "traoptimal")  // 실시간 교통 최적
        .build();

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-NCP-APIGW-API-KEY-ID", naverMapsClientId);
    headers.set("X-NCP-APIGW-API-KEY", naverMapsClientSecret);

    // RestTemplate 또는 WebClient로 호출
}
```

#### 4.3 응답 데이터 구조
```json
{
  "route": {
    "traoptimal": [
      {
        "summary": {
          "duration": 5400000,  // 밀리초
          "distance": 42500     // 미터
        },
        "path": [[lng, lat], ...],
        "section": [...]
      }
    ]
  }
}
```

---

### 5. p_company_delivery_routes 테이블 설계

#### 5.1 테이블 스키마
```sql
CREATE TABLE p_company_delivery_routes (
    route_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id UUID NOT NULL,
    departure_hub_id UUID NOT NULL,
    receiver_company_id UUID NOT NULL,

    -- 경로 정보
    estimated_distance_km DECIMAL(10, 2),
    estimated_duration_min INTEGER,
    actual_distance_km DECIMAL(10, 2),
    actual_duration_min INTEGER,

    -- 배송 순서 및 상태
    delivery_sequence INTEGER NOT NULL,  -- AI가 계산한 최적 순서
    current_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',

    -- 담당자
    delivery_staff_id BIGINT,

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);
```

#### 5.2 JPA 엔티티
```java
@Entity
@Table(name = "p_company_delivery_routes")
@Where(clause = "deleted_at IS NULL")
public class CompanyDeliveryRoute {

    @Id
    @GeneratedValue
    private UUID routeId;

    @Column(nullable = false)
    private UUID deliveryId;

    @Column(nullable = false)
    private UUID departureHubId;

    @Column(nullable = false)
    private UUID receiverCompanyId;

    private BigDecimal estimatedDistanceKm;
    private Integer estimatedDurationMin;
    private BigDecimal actualDistanceKm;
    private Integer actualDurationMin;

    @Column(nullable = false)
    private Integer deliverySequence;  // ChatGPT가 계산한 순서

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteStatus currentStatus;

    private Long deliveryStaffId;

    // Audit fields
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private LocalDateTime deletedAt;
    private String deletedBy;
}
```

---

### 6. 비즈니스 플로우

```
[06:00 스케줄러 실행]
    ↓
[delivery-service 호출]
    ↓ (당일 배송 목록 조회)
[업체 배송 담당자별 그룹핑]
    ↓
[ChatGPT API 호출]
    ↓ (TSP 해결: 최적 방문 순서)
[Naver Maps API 호출]
    ↓ (waypoints 기반 경로 계산)
[p_company_delivery_routes 저장]
    ↓
[ChatGPT로 메시지 생성]
    ↓
[Slack API로 담당자에게 발송]
    ↓ (p_notifications 저장)
[완료]
```

---

### 7. Slack 알림 메시지 예시

**ChatGPT 메시지 생성 프롬프트**:
```
다음 배송 정보를 기반으로 업체 배송 담당자에게 보낼 알림 메시지를 작성해주세요.

[배송 정보]
배송 담당자: 홍길동
출발 허브: 경기 남부 센터
배송 일자: 2025-11-05
총 배송 건수: 4건

[최적 경로]
1. A업체 (경기도 성남시) - 예상 15분 소요
2. B업체 (경기도 용인시) - 예상 20분 소요
3. C업체 (경기도 수원시) - 예상 25분 소요
4. D업체 (경기도 안양시) - 예상 18분 소요

총 예상 소요 시간: 1시간 38분
총 이동 거리: 42.5km

친절하고 명확한 메시지로 작성해주세요.
```

**생성된 메시지 예시**:
```
안녕하세요, 홍길동 님!

오늘(2025-11-05) 배송 경로를 안내드립니다.

📍 출발지: 경기 남부 센터
📦 총 배송 건수: 4건
🚚 총 이동 거리: 42.5km
⏱ 예상 소요 시간: 1시간 38분

[최적 배송 순서]
1️⃣ A업체 (성남시) - 15분
2️⃣ B업체 (용인시) - 20분
3️⃣ C업체 (수원시) - 25분
4️⃣ D업체 (안양시) - 18분

안전 운전하세요!
```

---

### 8. 에러 처리 및 폴백

#### 8.1 ChatGPT API 실패 시
- **폴백 알고리즘**: Nearest Neighbor (가장 가까운 다음 지점)
- 또는 순서대로 배송 (입력 순서 유지)

#### 8.2 Naver Maps API 실패 시
- **사전 계산된 평균 값 사용** (허브별 평균 거리/시간)
- 로그 기록 및 관리자 알림

#### 8.3 Slack API 실패 시
- **재시도**: 최대 3회, Exponential Backoff
- 실패 시 p_notifications에 FAILED 상태로 저장

---

### 9. 테스트 전략

#### 9.1 단위 테스트
- ChatGPT 프롬프트 생성 로직
- 응답 파싱 로직
- Naver Maps waypoints 구성 로직

#### 9.2 통합 테스트
- ChatGPT API Mock 테스트 (WireMock)
- Naver Maps API Mock 테스트
- 스케줄러 동작 확인 (AwaitilityTest)

#### 9.3 E2E 테스트
- 실제 API 키로 전체 플로우 테스트
- 배송 목록 조회 → 최적화 → 경로 계산 → 알림 발송

---

### 10. 모니터링 및 개선

#### 10.1 API 사용량 모니터링
- ChatGPT API 호출 횟수 및 비용 추적
- p_external_api_logs 테이블에 기록
- 일일 할당량 초과 시 알림

#### 10.2 성능 개선
- ChatGPT 응답 캐싱 (동일 배송지 패턴)
- Naver Maps 결과 캐싱 (경로 재사용)

#### 10.3 알고리즘 개선
- ChatGPT TSP 결과 vs Nearest Neighbor 비교
- 실제 배송 시간 데이터 수집 후 AI 재학습

---

## 참고 문서
- [OpenAI API Documentation](https://platform.openai.com/docs/api-reference)
- [Naver Maps Directions 5 API](https://api.ncloud-docs.com/docs/ai-naver-mapsdirections-driving)
- [Slack API - chat.postMessage](https://api.slack.com/methods/chat.postMessage)
- notification-service-overview.md
- CLAUDE.md

---

## 변경 이력
| 날짜 | 작성자 | 변경 내용 |
|------|--------|----------|
| 2025-11-04 | Team | 초안 작성 (Gemini AI → ChatGPT로 변경) |