# PR #54: 허브 경로 CRUD + 다익스트라 최단 경로 구현

## Issue Number
> closed #45  
> closed #46  
> closed #47

## 📝 Description

### Docker
- docker-compose.yml 복구 (사라진 파일 재추가)
- 서비스 구성: eureka-server, gateway, hub-service, postgres, redis

### Hub Route CRUD
- 허브 직통 경로 등록, 수정, 삭제 API
- 허브 경로 전체 조회 + 페이징 처리
- 허브 경로 ID로 단일 조회
- 허브 직통 경로 단일 조회 (출발 허브 ID + 도착 허브 ID 기준)
- **허브 간 최단 경로 조회 (다익스트라 알고리즘 적용)**

### Redis 3단계 캐싱 전략
1. **직통 경로 캐싱**: `hub:route:from:{fromId}:to:{toId}` → HubRoute JSON
2. **허브 연결 그래프 캐싱**: `hub:graph:{hubId}` → Hash<toHubId, {distance, time}>
3. **최단 경로 캐싱**: `hub:path:from:{fromId}:to:{toId}` → ShortestRouteResponse JSON

### Sample Data
- `hub.sql`: 17개 허브 + 52개 직통 경로 데이터 제공

## 📊 변경 사항

### 변경 파일 (28개 파일, +1243/-47)

#### Domain Layer
- **HubRoute Entity**: 
  - `pathNodes` 필드 추가 (TEXT 타입, JSON 저장)
  - `RouteType` enum 추가: DIRECT (직통), RELAY (중계)
  - Factory method: `createDirectRoute()`, `createRelayRoute()`

#### Application Layer
- **DijkstraService**: 다익스트라 최단 경로 알고리즘 구현
- **HubRouteCacheService**: Redis 캐싱 로직 분리
  - 직통 경로 캐싱/무효화
  - 그래프 구조 캐싱 (인접 리스트)
  - 최단 경로 결과 캐싱
- **HubRouteService**: 
  - CRUD 비즈니스 로직
  - 직통 경로 우선 조회 → 없으면 다익스트라 계산
  - Bulk Hub 조회 최적화

#### Presentation Layer
- **HubRouteController**: 
  - 7개 엔드포인트 추가
  - Swagger 문서화 완료
- **DTO 추가**:
  - `HubRouteRequest/Response`
  - `ShortestRouteResponse` (최단 경로 전용)
  - `RouteEdgeResponse` (경로 간선 정보)
  - `HubSimpleResponse` (허브 간략 정보)

#### Infrastructure Layer
- **HubRouteJpaRepository**: 
  - `deleteAllByRouteType(RouteType type)` 메서드 추가
  - 커스텀 쿼리: 출발/도착 허브 기반 조회

#### Configuration
- **RedisConfig**: ObjectMapper Bean 설정
- **ErrorCode**: Redis, HubRoute 관련 에러 10개 추가

## 🌐 Test Result

### 허브 경로 전체 조회
- 페이징 처리 확인 (size=10, page=0)
- DIRECT 경로 52개 조회 성공

### 허브 최단 경로 조회
- 서울 → 부산: RELAY 경로 (중계 허브: 대전)
- 총 거리, 총 시간, 경유 노드, 경로 간선 정보 반환
- Redis 캐싱 적용 확인

## 🔍 코드 리뷰 결과

### ✅ 잘된 점

#### 1. Redis 캐싱 전략 우수
- 3단계 캐싱으로 성능 최적화
- Pipeline 사용으로 네트워크 비용 절감
- 캐시 무효화 전략 명확 (직통 경로 변경 시 RELAY만 삭제)

#### 2. 다익스트라 알고리즘 논리 정확
- 기본 다익스트라 로직 구현
- 경로 복원 (backtracking) 구현
- BigDecimal 사용으로 부동소수점 오차 방지

#### 3. 성능 최적화
- Bulk Hub 조회: `getHubsBulk()` (N+1 방지)
- 직통 경로 우선 조회 → 없을 때만 다익스트라 계산
- Redis 캐싱으로 재계산 방지

#### 4. DDD 구조 준수
- Service 계층 분리: HubRouteService, DijkstraService, HubRouteCacheService
- DTO를 application layer로 명확히 분리
- Factory method 패턴 일관성 유지

### 🚨 Critical Issues (필수 수정)

#### 1. PriorityQueue 비교자 버그 (심각도: 매우 높음)
**현재 코드** (`DijkstraService.java`):
```java
Map<UUID, BigDecimal> distances = new HashMap<>();
PriorityQueue<UUID> pq = new PriorityQueue<>(Comparator.comparing(distances::get));

for (UUID hubId : graph.keySet()) {
    distances.put(hubId, BigDecimal.valueOf(Double.MAX_VALUE));
}
```

**문제점**:
- PriorityQueue 생성 시점에 `distances::get`을 캡처하면, distances Map이 비어있어 **NullPointerException 발생 가능**
- Comparator가 생성 시점의 Map 상태를 참조 → 의도대로 동작하지 않음

**해결책 (택 1)**:

**Option 1: Node 래핑 클래스 사용 (권장)**
```java
class Node {
    UUID hubId;
    BigDecimal distance;
    
    Node(UUID hubId, BigDecimal distance) {
        this.hubId = hubId;
        this.distance = distance;
    }
}

PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparing(n -> n.distance));

// 사용
distances.put(startHub, BigDecimal.ZERO);
pq.add(new Node(startHub, BigDecimal.ZERO));

while (!pq.isEmpty()) {
    Node current = pq.poll();
    UUID hubId = current.hubId;
    // ...
    pq.add(new Node(neighbor, newDist));
}
```

**Option 2: Custom Comparator (간단한 수정)**
```java
PriorityQueue<UUID> pq = new PriorityQueue<>((a, b) -> 
    distances.get(a).compareTo(distances.get(b))
);
```

#### 2. 중복 방문 방지 누락 (심각도: 높음)
**현재 코드**:
```java
while (!pq.isEmpty()) {
    UUID current = pq.poll();
    // visited 체크 없음 → 동일 노드 여러 번 처리 가능
    
    for (HubEdge edge : graph.get(current)) {
        // ...
        pq.add(neighbor); // 중복 추가됨
    }
}
```

**문제점**:
- 동일 노드가 PriorityQueue에 여러 번 추가됨
- 시간 복잡도 증가: O(E log V) → O(E² log V)
- 허브 개수가 많아지면 성능 급격히 저하

**해결책**:
```java
Set<UUID> visited = new HashSet<>();

while (!pq.isEmpty()) {
    UUID current = pq.poll();
    
    if (visited.contains(current)) continue;
    visited.add(current);
    
    if (!graph.containsKey(current)) continue;
    
    for (HubEdge edge : graph.get(current)) {
        UUID neighbor = edge.toHubId();
        
        if (visited.contains(neighbor)) continue; // 이미 방문한 노드 스킵
        
        BigDecimal newDist = distances.get(current).add(edge.routeDistance());
        
        if (newDist.compareTo(distances.get(neighbor)) < 0) {
            distances.put(neighbor, newDist);
            times.put(neighbor, times.get(current) + edge.routeTime());
            previous.put(neighbor, current);
            pq.add(neighbor);
        }
    }
}
```

#### 3. startHub가 pathNodes에 누락 (심각도: 중간)
**현재 코드**:
```java
List<UUID> path = new ArrayList<>();
UUID current = targetHub;

while (previous.containsKey(current)) {
    path.add(current);
    current = previous.get(current);
}

Collections.reverse(path);
// path에 startHub가 없음! [중간허브1, 중간허브2, targetHub]만 포함
```

**해결책**:
```java
List<UUID> path = new ArrayList<>();
UUID current = targetHub;

while (previous.containsKey(current)) {
    path.add(current);
    current = previous.get(current);
}

path.add(startHub); // 출발지 추가
Collections.reverse(path); // 이제 [startHub, 중간허브1, 중간허브2, targetHub]
```

#### 4. Redis 캐시 TTL 누락 (심각도: 중간)
**현재 코드** (`HubRouteCacheService.java`):
```java
public void updateDirectRouteCache(HubRoute route) {
    String key = String.format(DIRECT_ROUTE_KEY + KEY_FORMAT, ...);
    redisTemplate.opsForValue().set(key, json);
    // TTL 없음! 메모리 누적 위험
}

public void updateShortestPathCache(...) {
    String key = String.format(SHORTEST_PATH_KEY + KEY_FORMAT, ...);
    redisTemplate.opsForValue().set(key, json);
    // TTL 없음!
}
```

**문제점**:
- 캐시 데이터가 무한정 누적 → Redis 메모리 부족
- 오래된 데이터가 계속 남아있을 가능성

**해결책**:
```java
import java.time.Duration;

// 직통 경로 캐시: 7일 TTL
redisTemplate.opsForValue().set(key, json, Duration.ofDays(7));

// 최단 경로 캐시: 1일 TTL (자주 변경되는 데이터)
redisTemplate.opsForValue().set(key, json, Duration.ofDays(1));

// 그래프 캐시: 7일 TTL
// 각 Hash 필드 추가 후 전체 키에 TTL 설정
redisTemplate.expire(graphKey, Duration.ofDays(7));
```

### ⚠️ 개선 권장 사항

#### 1. 트랜잭션 경계 문제
**현재 코드** (`HubRouteService.java`):
```java
@Transactional
public HubRouteResponse updateHubRoute(Long routeId, HubRouteRequest request) {
    // DB 업데이트
    hubRoute.update(request);
    hubRouteRepository.flush();
    
    // RELAY 경로 삭제 (별도 쿼리)
    hubRouteRepository.deleteAllByRouteType(RouteType.RELAY);
    
    // 캐시 동기화 (Redis)
    hubRouteCacheService.syncOnUpdate(hubRoute);
    
    return HubRouteResponse.from(hubRoute, fromHub, toHub);
}
```

**문제점**:
- 트랜잭션 롤백 시 캐시는 이미 업데이트됨 → **데이터 불일치**
- DB 커밋 전에 캐시를 업데이트하면 일관성 깨짐

**권장 수정**:
```java
// Domain Event 발행
@Transactional
public HubRouteResponse updateHubRoute(Long routeId, HubRouteRequest request) {
    hubRoute.update(request);
    hubRouteRepository.flush();
    hubRouteRepository.deleteAllByRouteType(RouteType.RELAY);
    
    // Event 발행 (트랜잭션 커밋 후 처리)
    applicationEventPublisher.publishEvent(
        new HubRouteUpdatedEvent(hubRoute)
    );
    
    return HubRouteResponse.from(hubRoute, fromHub, toHub);
}

// Event Listener (트랜잭션 커밋 후 실행)
@Component
class HubRouteCacheEventHandler {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRouteUpdated(HubRouteUpdatedEvent event) {
        hubRouteCacheService.syncOnUpdate(event.getRoute());
    }
}
```

#### 2. Edge 필터링 비효율
**현재 코드**:
```java
while (previous.containsKey(current)) {
    UUID prev = previous.get(current);
    path.add(current);
    
    // 매번 stream 필터링 → O(n)
    graph.get(prev).stream()
        .filter(e -> e.toHubId().equals(curr))
        .findFirst()
        .ifPresent(edges::add);
    
    current = prev;
}
```

**개선안**:
```java
// previous에 edge 정보도 함께 저장
Map<UUID, HubEdge> previousEdge = new HashMap<>();

// 다익스트라 수행 중
if (newDist.compareTo(distances.get(neighbor)) < 0) {
    // ...
    previous.put(neighbor, current);
    previousEdge.put(neighbor, edge); // edge 저장
}

// 경로 복원
while (previous.containsKey(current)) {
    path.add(current);
    edges.add(previousEdge.get(current)); // O(1) 조회
    current = previous.get(current);
}
```

#### 3. JSON 직렬화 에러 로깅 부족
**현재 코드**:
```java
try {
    return objectMapper.readValue(json, HubRoute.class);
} catch (JsonProcessingException e) {
    throw new CustomException(ErrorCode.REDIS_DESERIALIZATION_FAILED);
}
```

**문제점**:
- 에러 발생 시 어떤 데이터가 문제인지 알 수 없음
- 디버깅 어려움

**개선안**:
```java
try {
    return objectMapper.readValue(json, HubRoute.class);
} catch (JsonProcessingException e) {
    log.error("Redis deserialization failed. Key: {}, JSON: {}", 
        key, json, e);
    throw new CustomException(ErrorCode.REDIS_DESERIALIZATION_FAILED);
}
```

#### 4. pathNodes JSON 저장 방식 개선
**현재 코드**:
```java
@Column(columnDefinition = "text")
private String pathNodes; // JSON 문자열 저장
```

**문제점**:
- JSON 문자열로 저장하면 JPA 쿼리 불가
- 타입 안정성 부족

**개선 Option 1: JPA Converter**
```java
@Convert(converter = JsonListConverter.class)
private List<UUID> pathNodes;

// Converter 구현
@Converter
public class JsonListConverter implements AttributeConverter<List<UUID>, String> {
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String convertToDatabaseColumn(List<UUID> attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public List<UUID> convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
```

**개선 Option 2: PostgreSQL JSONB 타입**
```java
@Type(type = "jsonb")
@Column(columnDefinition = "jsonb")
private List<UUID> pathNodes;
```

#### 5. 샘플 데이터 자동 로딩
**현재**: `hub.sql` 파일을 수동으로 실행해야 함

**개선안**:
```yaml
# application.yml
spring:
  sql:
    init:
      mode: always
      data-locations: classpath:data/hub.sql
      schema-locations: classpath:schema.sql
```

## 📊 알고리즘 성능 분석

### 다익스트라 시간 복잡도
- **이론**: O((V + E) log V) (우선순위 큐 사용)
- **현재 구현**: O(E² log V) (중복 방문 때문)
- **개선 후**: O((V + E) log V)

### Redis 캐싱 효과
- **직통 경로 조회**: O(1) - Redis GET
- **그래프 로드**: O(V) - Hash 전체 스캔
- **최단 경로 캐싱**: 동일 요청 시 다익스트라 재계산 불필요

### 성능 테스트 권장
- 허브 100개 이상 시나리오
- 동시 요청 처리 (동시성 테스트)
- Redis 메모리 사용량 모니터링

## 📊 종합 평가

| 항목 | 점수 | 평가 |
|------|------|------|
| 알고리즘 정확성 | ⭐⭐⭐⭐☆ | 다익스트라 논리 정확, 구현 버그 있음 |
| Redis 캐싱 설계 | ⭐⭐⭐⭐⭐ | 3단계 캐싱 전략 우수 |
| 성능 최적화 | ⭐⭐⭐⭐☆ | Bulk 조회, Pipeline 사용 |
| 에러 처리 | ⭐⭐⭐☆☆ | 로그 부족, 예외만 던짐 |
| 트랜잭션 관리 | ⭐⭐⭐☆☆ | 캐시 동기화 롤백 이슈 |
| 코드 가독성 | ⭐⭐⭐⭐☆ | 명확한 레이어 분리 |

**총평**: 아이디어와 설계는 매우 훌륭하나, 다익스트라 구현에 Critical한 버그가 있음. PriorityQueue 비교자와 중복 방문 방지를 수정하면 Approve 가능.

## ✅ Merge 전 체크리스트

### 필수 수정 (Blocking Issues)
- [ ] **다익스트라 PriorityQueue 비교자 수정** (Node 클래스 또는 Custom Comparator)
- [ ] **visited Set 추가** (중복 방문 방지)
- [ ] **startHub를 pathNodes에 추가**
- [ ] **Redis 캐시 TTL 설정** (메모리 누적 방지)

### 강력 권장 (권장)
- [ ] 캐시 동기화 트랜잭션 분리 (`@TransactionalEventListener`)
- [ ] Edge 필터링 최적화 (previousEdge Map 사용)
- [ ] JSON 직렬화 에러 로깅 강화

### 선택 사항 (추후 개선)
- [ ] pathNodes JPA Converter 또는 JSONB 타입 사용
- [ ] 단위 테스트 추가 (다익스트라 알고리즘 검증)
- [ ] 성능 테스트 (허브 100개 이상)
- [ ] 샘플 데이터 자동 로딩 설정

## 🔗 Related Links
- PR: https://github.com/14th-anniv/one-for-logis/pull/54
- Issue #45: 허브 경로 등록
- Issue #46: 허브 경로 수정
- Issue #47: 허브 경로 삭제
- Branch: `feature/#45-create-hub-route`

## 👥 Reviewers
- 리뷰 요청 필요

## 💬 To Reviewer
> 고봉밥 죄솸다....  
> 다익스트라 알고리즘 처음 사용해봐서 맞는지 잘 모르겠습니다 ㅠㅜ...

**리뷰어 답변**:
- 다익스트라 핵심 로직은 정확합니다! 👍
- PriorityQueue 비교자와 중복 방문 방지만 수정하면 완벽합니다.
- Redis 캐싱 전략이 매우 우수합니다. TTL만 추가하면 production ready!

---
**작성자**: @dyun23  
**리뷰 완료일**: 2025-11-10  
**상태**: 리뷰 완료 - Critical 수정 필요 (PriorityQueue, visited Set)
