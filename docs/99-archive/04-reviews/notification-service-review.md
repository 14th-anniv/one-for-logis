# Notification Service 최종 검토 보고서

## 1. 전체 아키텍처

- **개요**: Spring Boot 기반의 MSA로, 계층형 아키텍처(`presentation`, `application`, `domain`, `infrastructure`)를 잘 따르고 있습니다.
- **기술 스택**:
    - **Microservice**: Eureka (Service Discovery), Feign (내부 서비스 통신)
    - **Messaging**: Kafka (비동기 이벤트 처리)
    - **External APIs**: Slack (메시지 발송), Google Gemini (AI 기반 콘텐츠 생성)
    - **Database**: PostgreSQL (JPA, Soft Delete 적용)
    - **API Documentation**: Swagger (OpenAPI)
- **총평**: 전반적으로 현대적인 MSA 표준을 잘 따르고 있으며, 확장성과 유지보수성을 고려한 설계가 돋보입니다.

## 2. 계층별 분석

### Presentation Layer (`NotificationController`)

- **잘된 점**:
    - **API 설계**: 역할에 따라 API가 명확하게 분리되어 있고, Swagger를 통한 문서화가 우수합니다.
    - **보안**: `@PreAuthorize`를 사용하여 엔드포인트별로 세밀한 역할 기반 접근 제어(RBAC)를 구현했습니다.
    - **일관성**: 모든 응답이 `ApiResponse` 래퍼 클래스를 사용하여 일관된 포맷을 가집니다.
    - **유효성 검사**: `@Valid`를 이용한 입력값 검증이 잘 적용되어 있습니다.

- **고려할 점**:
    - **동기 호출**: 수동 메시지 발송 시 `user-service`를 Feign Client로 동기 호출합니다. `user-service`의 장애나 지연이 `notification-service`에 직접적인 영향을 미칠 수 있으므로, Resilience4j의 Circuit Breaker 적용을 검토해야 합니다. (의존성은 추가되어 있으나 실제 적용 여부 확인 필요)

### Application Layer (`NotificationService`)

- **잘된 점**:
    - **비즈니스 로직**: 주문 알림과 수동 알림의 로직이 명확히 분리되어 있습니다.
    - **멱등성 보장**: Kafka 이벤트 처리를 위해 `eventId`에 유니크 제약을 두어 중복 처리를 방지하는 설계가 매우 우수합니다.
    - **트랜잭션 관리**: `@Transactional`의 `readOnly` 옵션을 클래스 레벨에 적용하고, 쓰기 메서드에만 별도로 `@Transactional`을 부여하여 트랜잭션 관리를 최적화했습니다.
    - **보안 및 재사용성**: 페이징 처리 로직(`createPageable`)을 헬퍼 메서드로 분리하고, 정렬 기준 필드를 화이트리스트 방식으로 검증하여 SQL Injection을 방지한 점이 인상적입니다.

- **수정이 시급한 문제**:
    - **심각한 성능 저하**: `searchNotifications` 메서드가 **모든 알림을 DB에서 조회한 후 메모리에서 필터링 및 페이징**을 수행합니다. 데이터가 증가하면 심각한 성능 및 메모리 문제를 유발할 것이므로, **JPA Specification이나 Querydsl을 사용하여 DB 레벨에서 필터링하도록 즉시 수정**해야 합니다.

- **개선 제안 (누락된 기능)**:
    - **재시도 메커니즘**: Slack 메시지 발송 실패 시 상태를 `FAILED`로만 기록하고 재시도하지 않습니다. 안정적인 알림 전송을 위해 Spring Retry 등을 이용한 재시도 로직(예: Exponential Backoff) 추가가 필요합니다.
    - **Dead-Letter Queue (DLQ)**: 재시도에도 계속 실패하는 알림을 처리할 전략이 없습니다. 최종 실패한 알림은 별도의 테이블이나 Kafka DLQ 토픽으로 보내 수동 조치할 수 있도록 해야 합니다.
    - **비동기 처리**: Gemini, Slack 등 외부 API 호출이 동기(Blocking) 방식으로 동작합니다. `@Async`나 Project Reactor를 활용하여 비동기 처리로 전환하면 시스템의 처리량과 응답성을 크게 향상시킬 수 있습니다.

### Domain Layer (`Notification` Entity)

- **잘된 점**:
    - **데이터 모델링**: 발신자/수신자 정보를 스냅샷 형태로 저장하여, 시간이 지나도 알림 당시의 데이터를 보존할 수 있도록 설계되었습니다. (Audit 및 추적에 매우 유리)
    - **데이터 무결성**: `@PrePersist`, `@PreUpdate`를 사용해 엔티티 레벨에서 강력한 유효성 검사를 수행하여 데이터 무결성을 보장합니다.
    - **멱등성**: `eventId` 컬럼에 `unique = true` 제약 조건을 설정하여 Kafka 이벤트의 중복 처리를 DB 레벨에서 원천적으로 방지합니다.

### Infrastructure Layer

- **잘된 점**:
    - **관심사 분리**: 도메인 레이어의 Repository 인터페이스와 인프라 레이어의 JPA 구현체를 분리하여, 클린 아키텍처의 원칙을 잘 따르고 있습니다.

## 3. 최종 요약

`notification-service`는 전반적으로 잘 설계된 MSA이지만, **검색 기능의 심각한 성능 문제**를 반드시 해결해야 합니다. 또한, 실제 운영 환경에서 안정성을 높이기 위해 **재시도 및 DLQ 처리, 외부 API 호출 비동기화**와 같은 회복성 패턴을 도입하는 것을 적극 권장합니다.