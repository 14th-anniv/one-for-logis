# 프로젝트 소개

## 프로젝트 개요

**one-for-logis** (14logis)는 물류의 모든 과정을 하나로 연결하는 B2B 스마트 통합 물류 플랫폼입니다.

### 핵심 목표
- 17개 지역 허브 센터 기반 전국 물류 네트워크 구축
- 마이크로서비스 아키텍처(MSA)로 확장 가능한 시스템 설계
- AI 기반 지능형 배송 시간 계산 및 최적화
- 실시간 배송 추적 및 알림 시스템

## 주요 기능

### 1. 허브 관리 (Hub Management)
- 17개 허브 센터 CRUD
- 다익스트라 알고리즘 기반 최단 경로 계산
- Redis 3단계 캐싱 (직통 경로 → 그래프 → 최단 경로)
- 직통/중계 경로 자동 구분

### 2. 업체 관리 (Company Management)
- 업체 등록/수정/삭제 (소속 허브 연결)
- 업체별 상품 관리
- 허브별 업체 조회/검색

### 3. 상품 및 재고 관리 (Product & Inventory)
- 상품 등록/수정/삭제
- 재고 실시간 추적
- 업체별 상품 관리

### 4. 주문 관리 (Order Management)
- 주문 생성 시 공급업체/수신업체/상품 검증 (FeignClient)
- 재고 가용성 체크
- 배송 자동 생성 트리거

### 5. 배송 관리 (Delivery Management)
- 배송 담당자 라운드로빈 자동 할당
- 허브 간 경로 자동 계산
- 배송 상태 실시간 업데이트 (Kafka 이벤트)
- 배송 조회/검색 (JPA Specification)

### 6. 알림 시스템 (Notification System) ⭐
- **AI 기반 출발 시한 계산** (Google Gemini API)
- Slack API 실시간 메시지 발송
- Kafka 이벤트 기반 자동 알림
  - 주문 생성 → 허브 관리자 알림
  - 배송 상태 변경 → 담당자 알림
- 외부 API 호출 로그 및 통계
- **Challenge**: Naver Maps API 기반 일일 배송 경로 최적화 (TSP)

### 7. 사용자 관리 (User Management)
- 4단계 권한 시스템 (MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER)
- JWT 기반 인증/인가
- 회원 가입 → 관리자 승인 → 로그인
- Redis 기반 Refresh Token 관리

## 시스템 아키텍처

### 마이크로서비스 구성
```
├── eureka-server (8761)       - 서비스 디스커버리
├── gateway-service (8000)     - API Gateway, JWT 인증
├── user-service (8100)        - 사용자 관리
├── hub-service (8200)         - 허브 및 경로 관리
├── company-service (8300)     - 업체 관리
├── order-service (8400)       - 주문 관리
├── product-service (8500)     - 상품 및 재고 관리
├── delivery-service (8600)    - 배송 추적 및 담당자 관리
├── notification-service (8700) - 알림 및 AI 통합
└── zipkin-server (9411)       - 분산 추적
```

### 주요 기술 스택
- **언어/프레임워크**: Java 17, Spring Boot 3.x
- **MSA**: Spring Cloud (Eureka, Gateway, OpenFeign)
- **보안**: Spring Security, JWT
- **데이터베이스**: PostgreSQL 17 (스키마 분리)
- **메시징**: Kafka (이벤트 기반 통신)
- **캐싱**: Redis
- **모니터링**: Zipkin, Actuator
- **API 문서**: Springdoc OpenAPI (Swagger)
- **외부 API**: Slack API, Google Gemini API, Naver Maps API
- **빌드**: Gradle (멀티 모듈)
- **컨테이너**: Docker, Docker Compose

## 프로젝트 특징

### 1. DDD (Domain-Driven Design) 패턴
- Presentation → Application → Domain → Infrastructure 계층 분리
- 도메인 로직 순수성 유지
- Repository 인터페이스/구현 분리

### 2. MSA 통신 패턴
- 서비스 간 REST API 통신 (FeignClient)
- 이벤트 기반 비동기 통신 (Kafka)
- Circuit Breaker 및 Fallback 패턴
- 최종 일관성 (Eventual Consistency)

### 3. 데이터 관리
- PostgreSQL 스키마 분리 (서비스별 독립)
- Soft Delete 패턴 (deleted_at, deleted_by)
- 감사 추적 (Audit Fields)
- 스냅샷 패턴 (알림 발신자 정보 보존)

### 4. 보안 설계
- Gateway: JWT 인증만 수행
- 각 서비스: 자체 권한 검사 (@PreAuthorize)
- 계층적 권한 구조 (MASTER > HUB_MANAGER > DELIVERY_MANAGER > COMPANY_MANAGER)

## 개발 팀 구성
- 5인 팀 프로젝트
- 각자 2개 서비스 담당
- 스크럼 방식 애자일 개발
- GitHub Issue/PR 기반 협업
- 코드 리뷰 필수

## 문서 구조
상세 문서는 다음 링크를 참조하세요:
- [아키텍처 설계](./architecture.md)
- [팀 컨벤션](./team-conventions.md)
- [데이터베이스 스키마](../02-development/database-schema.md)
- [비즈니스 규칙](../02-development/business-rules.md)
- [테스트 가이드](../04-testing/testing-guide.md)

## 프로젝트 기간
- 기획 및 설계: 2024-10-28 ~ 11-03
- 개발: 2024-11-04 ~ 11-13
- 총 기간: 약 3주
