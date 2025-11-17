# Team Conventions

## DDD Package Structure

```markdown
src/main/java/com/oneforlogis/
├── presentation/
│   ├── controller/             # REST API 엔드포인트
│   ├── request/                # 요청 DTO
│   ├── response/               # 응답 DTO
│   └── advice/                 # 예외 처리
│
├── application/
│   ├── [Service]Facade.java    # 유즈케이스 조합 / 흐름 제어
│   └── dto/                    # 응용계층 DTO
│
├── domain/
│   ├── model/                  # 엔티티 (비즈니스 규칙 중심)
│   ├── repository/             # Repository 인터페이스
│   ├── service/                # 도메인 서비스
│   └── exception/              # 도메인 예외
│
├── infrastructure/
│   ├── persistence/            # JPA 엔티티, RepositoryImpl
│   ├── client/                 # FeignClient 등 외부 API 연동
│   ├── config/                 # DB, Cache 등 인프라 설정
│   └── messaging/              # 메시징 시스템 (Kafka, RabbitMQ)
│
└── global/
    ├── config/                 # Security, CORS, Swagger 등 공통 설정
    ├── common/                 # BaseEntity, ApiResponse 등
    ├── exception/              # Global Exception Handler
    └── util/                   # 인증 컨텍스트, 유틸성 클래스
```

**presentation**
    - HTTP 레이어
    - Swagger / @Operation 문서화 담당
    - 요청(Request) / 응답(Response) DTO 관리

---

**application**
    - 유즈케이스 조합 및 흐름 제어
        - 예: 허브 생성 시 권한 체크 → 도메인 서비스 호출 → 결과를 응답 DTO로 변환
    - 다른 마이크로서비스와의 통신 조립도 이 계층에서 처리
        - 예: 허브 삭제 시 route-service에도 soft delete 동기화

---

**domain**
    - 순수 비즈니스 규칙 정의
        - 예: “허브는 deleted_at != null이면 조회 불가”
        - “허브 관리자는 자기 허브만 수정 가능” 등의 권한 제약도 포함 가능
    - Repository 인터페이스 위치 (구현체는 infrastructure로 분리)
    - **DDD 스타일의 핵심 계층**

---

**infrastructure**
    - 기술 의존부 (DB, JPA, FeignClient 등)
    - soft delete 쿼리 필터, 캐싱(Cacheable), Redis 등 IO 관련 처리
    - 외부 통신 / 데이터 접근 계층

---
**global**
    - 공통 에러 처리 / 시큐리티 / 응답 Wrapper 등
    - 모든 서비스에서 동일한 룰 유지
    - 공통화 가능한 요소는 common-lib로 추출

---
**etc**
    - Dto는 파일명에서 제거
    - 도메인명 + 동사 + Request/Response 순서로 네이밍
    - 엔드포인트에 도메인 복수로 통일
