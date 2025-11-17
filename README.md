# one-for-logis

> 물류의 모든 과정을 하나로 연결하는 스마트 통합 물류 플랫폼

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://www.docker.com/)

## 📋 프로젝트 소개

**one-for-logis**는 전국 17개 지역 허브를 기반으로 한 B2B 물류 관리 시스템입니다.
마이크로서비스 아키텍처(MSA)로 설계되어 확장 가능하고 안정적인 물류 프로세스를 제공합니다.

### 핵심 기능
- 🏢 **허브 & 경로 관리**: 다익스트라 알고리즘 기반 최단 경로 계산, Redis 3단계 캐싱
- 🏭 **업체 & 상품 관리**: 허브별 업체 관리, 실시간 재고 추적
- 📦 **주문 & 배송 관리**: 자동 배송 생성, 라운드로빈 담당자 할당, 실시간 상태 추적
- 🔔 **AI 기반 알림**: Google Gemini API를 활용한 출발 시한 계산, Slack 실시간 알림
- 🔐 **4단계 권한 시스템**: MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER

### 주요 특징
- **MSA 아키텍처**: 9개의 독립적인 마이크로서비스 + 공통 라이브러리
- **DDD 패턴**: Presentation → Application → Domain → Infrastructure 계층 분리
- **이벤트 기반 통신**: Kafka를 활용한 비동기 메시징
- **분산 추적**: Zipkin을 통한 서비스 간 호출 모니터링
- **외부 API 통합**: Slack, Google Gemini, Naver Maps

## 🏗️ 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                       API Gateway (8000)                         │
│                    JWT Authentication                            │
└─────────────────────────────────────────────────────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
┌───────▼────────┐    ┌─────────▼────────┐    ┌─────────▼────────┐
│  User Service  │    │   Hub Service    │    │ Company Service  │
│     (8100)     │    │     (8200)       │    │      (8300)      │
│                │    │                  │    │                  │
│  - 사용자 관리  │    │  - 허브 관리      │    │  - 업체 관리      │
│  - JWT 인증    │    │  - 경로 계산      │    │  - 허브별 업체    │
└────────────────┘    └──────────────────┘    └──────────────────┘

┌────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ Order Service  │    │ Product Service  │    │ Delivery Service │
│     (8400)     │    │      (8500)      │    │      (8600)      │
│                │    │                  │    │                  │
│  - 주문 관리    │    │  - 상품 관리      │    │  - 배송 추적      │
│  - Saga 패턴   │    │  - 재고 관리      │    │  - 담당자 할당    │
└────────────────┘    └──────────────────┘    └──────────────────┘

┌────────────────────────────────────────────────────────────────┐
│              Notification Service (8700)                       │
│                                                                │
│  - AI 출발 시한 계산 (Gemini API)                              │
│  - Slack 메시지 발송                                           │
│  - Kafka 이벤트 기반 자동 알림                                  │
│  - 외부 API 로그 및 통계                                        │
└────────────────────────────────────────────────────────────────┘

┌────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ Eureka Server  │    │  Zipkin Server   │    │   PostgreSQL     │
│     (8761)     │    │      (9411)      │    │      (5432)      │
│                │    │                  │    │                  │
│  - 서비스 등록  │    │  - 분산 추적      │    │  - 스키마 분리    │
└────────────────┘    └──────────────────┘    └──────────────────┘
```

## 🛠️ 기술 스택

### Backend
- **언어**: Java 17
- **프레임워크**: Spring Boot 3.x
- **MSA**: Spring Cloud (Eureka, Gateway, OpenFeign)
- **보안**: Spring Security, JWT
- **데이터베이스**: PostgreSQL 17
- **메시징**: Apache Kafka
- **캐싱**: Redis
- **빌드**: Gradle (멀티 모듈)

### Infrastructure
- **컨테이너**: Docker, Docker Compose
- **모니터링**: Zipkin, Spring Boot Actuator
- **API 문서**: Springdoc OpenAPI (Swagger)

### External APIs
- **Slack API**: 실시간 알림 발송
- **Google Gemini API**: AI 기반 출발 시한 계산, 경로 최적화
- **Naver Maps API**: 경유지 기반 경로 계산

## 🚀 시작하기

### 사전 요구사항
- Java 17+
- Docker & Docker Compose
- Gradle 8.x+

### 환경 변수 설정
`.env.example`을 참고하여 `.env` 파일을 생성하세요:
```bash
cp .env.example .env
# .env 파일을 열어 필요한 값 설정
```

### 실행 방법

#### 1. Docker Compose로 전체 시스템 실행 (권장)
```bash
# 컨테이너 시작
docker-compose -f docker-compose-team.yml up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그 확인
docker-compose logs -f notification-service

# 컨테이너 종료
docker-compose down
```

#### 2. 로컬에서 개별 서비스 실행
```bash
# 전체 빌드
./gradlew build

# 특정 서비스 빌드
./gradlew :notification-service:build

# 특정 서비스 실행
./gradlew :notification-service:bootRun
```

### 접속 정보
- **API Gateway**: http://localhost:8000
- **Eureka Dashboard**: http://localhost:8761
- **Zipkin UI**: http://localhost:9411
- **Swagger UI** (각 서비스):
  - User: http://localhost:8100/swagger-ui.html
  - Hub: http://localhost:8200/swagger-ui.html
  - Company: http://localhost:8300/swagger-ui.html
  - Order: http://localhost:8400/swagger-ui.html
  - Product: http://localhost:8500/swagger-ui.html
  - Delivery: http://localhost:8600/swagger-ui.html
  - Notification: http://localhost:8700/swagger-ui.html

## 📁 프로젝트 구조

```
one-for-logis/
├── common-lib/                 # 공통 라이브러리 (BaseEntity, ApiResponse, Security)
├── eureka-server/              # 서비스 디스커버리
├── gateway-service/            # API Gateway
├── user-service/               # 사용자 관리
├── hub-service/                # 허브 및 경로 관리
├── company-service/            # 업체 관리
├── product-service/            # 상품 및 재고 관리
├── order-service/              # 주문 관리
├── delivery-service/           # 배송 관리
├── notification-service/       # 알림 및 AI 통합
├── docs/                       # 프로젝트 문서
│   ├── 01-overview/            # 프로젝트 개요
│   ├── 02-development/         # 개발 가이드
│   ├── 03-infrastructure/      # 인프라 및 배포
│   ├── 04-testing/             # 테스트 가이드
│   ├── 05-api-specs/           # API 명세
│   ├── 06-work-log/            # 작업 이력
│   ├── 07-issues/              # Issue 상세
│   └── 08-pull-requests/       # PR 리뷰
├── docker/                     # Docker 초기화 스크립트
├── scripts/                    # DB 초기화 스크립트
├── docker-compose-team.yml     # 팀 개발 환경
└── build.gradle                # Gradle 빌드 설정
```

## 📚 문서

### 주요 문서
- **[프로젝트 소개](docs/01-overview/project-intro.md)**: 상세한 프로젝트 개요
- **[아키텍처](docs/01-overview/architecture.md)**: MSA 아키텍처 설계 및 서비스 구성
- **[팀 컨벤션](docs/01-overview/team-conventions.md)**: Git, 코드 스타일, 네이밍 규칙
- **[데이터베이스 스키마](docs/02-development/database-schema.md)**: ERD 및 테이블 명세
- **[비즈니스 규칙](docs/02-development/business-rules.md)**: 도메인 로직 및 비즈니스 룰
- **[테스트 가이드](docs/04-testing/testing-guide.md)**: 단위/통합/E2E 테스트 전략
- **[Docker 환경](docs/03-infrastructure/docker-environment.md)**: Docker 설정 및 환경 변수

### API 문서
각 서비스의 Swagger UI를 통해 API 명세를 확인할 수 있습니다.
- [Notification Service API](docs/05-api-specs/notification-service-api.md)

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 서비스 테스트
./gradlew :notification-service:test

# 특정 테스트 클래스 실행
./gradlew :notification-service:test --tests NotificationServiceTest
```

## 👥 팀 구성

- **5인 팀 프로젝트**
- 각자 2개 서비스 담당
- 스크럼 방식 애자일 개발
- GitHub Issue/PR 기반 협업
- 코드 리뷰 필수

## 📅 프로젝트 기간

- **기획 및 설계**: 2024-10-28 ~ 11-03
- **개발**: 2024-11-04 ~ 11-13
- **총 기간**: 약 3주

## 📝 Git Workflow

### Commit Convention
```
type: summary (lowercase, imperative, max 50 chars)
```
- **Types**: feat, fix, chore, docs, refactor, test, style

### Branch Strategy
- `main`: Production (사용하지 않음)
- `dev`: 통합 브랜치
- `feature/#issueNum-description`: 새 기능
- `fix/#issueNum-description`: 버그 수정

### Pull Request
1. Issue 생성
2. 브랜치 생성 (feature/#issueNum-description)
3. 개발 및 테스트
4. PR 생성 (→ dev)
5. 코드 리뷰
6. 승인 후 Merge

## 🔐 보안

- **JWT 인증**: Access Token (15분) + Refresh Token (7일)
- **4단계 권한**: MASTER > HUB_MANAGER > DELIVERY_MANAGER > COMPANY_MANAGER
- **Gateway 인증**: JWT 검증 후 사용자 컨텍스트 헤더 추가
- **서비스 권한 검사**: @PreAuthorize 기반 권한 검증

## 🤝 기여

이 프로젝트는 팀 프로젝트로 진행되었으며, 외부 기여는 받지 않습니다.

## 📄 라이센스

This project is for educational purposes only.

## 📞 문의

프로젝트 관련 문의는 GitHub Issues를 이용해주세요.

---

**one-for-logis** - 물류의 모든 과정을 하나로 연결하는 스마트 통합 물류 플랫폼
