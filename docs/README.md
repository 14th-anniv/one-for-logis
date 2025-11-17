# 📚 one-for-logis 문서 가이드

본 디렉토리는 one-for-logis 프로젝트의 모든 기술 문서를 포함하고 있습니다.

## 📂 디렉토리 구조

```
docs/
├── 01-overview/            # 프로젝트 개요
├── 02-development/         # 개발 가이드
├── 03-infrastructure/      # 인프라 및 배포
├── 04-testing/             # 테스트 가이드
├── 05-api-specs/           # API 명세
├── 06-work-log/            # 작업 이력
├── 07-issues/              # Issue 상세 문서
├── 08-pull-requests/       # PR 리뷰 문서
└── 99-archive/             # 보관/참고용 문서
```

---

## 🎯 01-overview: 프로젝트 개요

프로젝트의 전반적인 정보와 아키텍처를 설명하는 문서입니다.

| 문서 | 설명 |
|------|------|
| [project-intro.md](01-overview/project-intro.md) | 프로젝트 소개, 주요 기능, 기술 스택, 팀 구성 |
| [architecture.md](01-overview/architecture.md) | MSA 아키텍처 설계, 서비스 구성, 통신 패턴 |
| [team-conventions.md](01-overview/team-conventions.md) | Git 컨벤션, 코드 스타일, 네이밍 규칙 |
| [github-rules.md](01-overview/github-rules.md) | GitHub Issue/PR 작성 규칙, 워크플로우 |

**추천 읽기 순서**: project-intro.md → architecture.md → team-conventions.md

---

## 💻 02-development: 개발 가이드

개발에 필요한 기술적 정보와 비즈니스 로직을 설명합니다.

| 문서 | 설명 |
|------|------|
| [database-schema.md](02-development/database-schema.md) | ERD, 테이블 명세, 관계 설명 |
| [business-rules.md](02-development/business-rules.md) | 도메인 로직, 비즈니스 규칙, 주요 플로우 |
| [package-structure.md](02-development/package-structure.md) | DDD 패키지 구조, 계층별 책임 (작성 예정) |
| [api-design.md](02-development/api-design.md) | API 설계 원칙, 응답 형식 (작성 예정) |

**핵심 문서**: database-schema.md, business-rules.md

---

## 🚀 03-infrastructure: 인프라 및 배포

Docker 환경 설정 및 서비스 구현 현황을 다룹니다.

| 문서 | 설명 |
|------|------|
| [docker-environment.md](03-infrastructure/docker-environment.md) | Docker Compose 설정, 환경 변수, 실행 가이드 |
| [service-status.md](03-infrastructure/service-status.md) | 각 서비스별 구현 현황 및 진행률 |
| [environment-variables.md](03-infrastructure/environment-variables.md) | 환경 변수 상세 설명 (작성 예정) |

**시작 시 필수**: docker-environment.md

---

## 🧪 04-testing: 테스트 가이드

테스트 전략 및 트러블슈팅 정보를 제공합니다.

| 문서 | 설명 |
|------|------|
| [testing-guide.md](04-testing/testing-guide.md) | 단위/통합/E2E 테스트 전략 및 예제 |
| [troubleshooting.md](04-testing/troubleshooting.md) | 자주 발생하는 문제와 해결 방법 |

**문제 해결**: troubleshooting.md 먼저 확인

---

## 📡 05-api-specs: API 명세

각 서비스의 상세 API 명세를 제공합니다.

| 문서 | 설명 |
|------|------|
| [notification-service-api.md](05-api-specs/notification-service-api.md) | notification-service REST API 상세 명세 |

**추가 예정**: 다른 서비스 API 명세

**실시간 API 문서**: 각 서비스의 Swagger UI 활용 (http://localhost:{port}/swagger-ui.html)

---

## 📝 06-work-log: 작업 이력

프로젝트 진행 상황 및 완료/남은 작업을 추적합니다.

| 문서 | 설명 |
|------|------|
| [completed-work.md](06-work-log/completed-work.md) | 완료된 PR/Issue 목록 및 상세 내역 |
| [left-issues.md](06-work-log/left-issues.md) | 남은 작업 및 우선순위 |
| [daily-scrum/](06-work-log/daily-scrum/) | 데일리 스크럼 회의록 (YYYY-MM-DD.md) |

**프로젝트 현황 파악**: completed-work.md + left-issues.md

---

## 🐛 07-issues: Issue 상세 문서

각 Issue별 상세 분석 및 해결 과정을 기록합니다.

### 파일명 규칙
```
issue-{3자리번호}-{설명}.md
```

### 주요 Issue 문서
| 번호 | 제목 | 설명 |
|------|------|------|
| [#011](07-issues/issue-011-notification-service-init.md) | notification-service 초기 설정 | 프로젝트 구조, 의존성 설정 |
| [#076](07-issues/issue-076-notification-risk-refactoring.md) | notification-service 리스크 개선 | 트랜잭션 분리, Fallback, 테스트 |
| [#084](07-issues/issue-084-delivery-status-rest-api.md) | 배송 상태 알림 REST API | POST /delivery-status 엔드포인트 |
| [#109](07-issues/issue-109-notification-swagger-fix.md) | Swagger 테스트 수정 | Slack ID 통일, FeignException 처리 |

**전체 목록**: [07-issues/](07-issues/) 디렉토리 참조

---

## 🔍 08-pull-requests: PR 리뷰 문서

각 PR에 대한 상세 리뷰 및 개선 사항을 기록합니다.

### 파일명 규칙
```
pr-{3자리번호}-{설명}.md
```

### 주요 PR 문서
| 번호 | 제목 | 설명 |
|------|------|------|
| [#052](08-pull-requests/pr-052-company-service-select-api.md) | company-service 조회 API | 업체 조회/검색 기능 |
| [#054](08-pull-requests/pr-054-hub-route-crud-dijkstra.md) | hub-service 경로 CRUD | 다익스트라 알고리즘, Redis 캐싱 |
| [#075](08-pull-requests/pr-075-feignclient-status-code-fix.md) | FeignClient 상태 코드 수정 | GlobalExceptionHandler 개선 |
| [#081](08-pull-requests/pr-081-user-login-signup.md) | user-service 로그인/회원가입 | JWT 인증, Redis Refresh Token |
| [#109](08-pull-requests/pr-109-notification-swagger-fix.md) | Swagger 테스트 수정 | FeignException 처리, user-service 연동 |

**전체 목록**: [08-pull-requests/](08-pull-requests/) 디렉토리 참조

---

## 📦 99-archive: 보관/참고용 문서

개발 과정의 참고 자료 및 레거시 문서를 보관합니다.

```
99-archive/
├── 01-initial-planning/      # 초기 기획 문서 (ERD, 테이블 명세, 와이어프레임)
├── 02-service-specs/         # 서비스 상세 명세 (notification-service 설계)
├── 03-implementation-plans/  # 구현 계획서 (Kafka, Issue #33 등)
├── 04-reviews/               # 프로젝트 리뷰 및 회고
├── 05-pr-docs/               # 과거 PR 설명서
├── 06-test-results/          # 테스트 결과 및 검증 문서
├── 07-guides/                # 가이드 및 튜토리얼 (Docker, DDD/MSA)
├── 08-references/            # 참고 자료 (SQL, API 실험 등)
└── 09-presentations/         # 발표 자료
```

**주의**: 이 문서들은 정제되지 않은 원본/과거 자료입니다.

---

## 🚀 빠른 시작 가이드

### 1. 프로젝트 처음 접하는 경우
1. [project-intro.md](01-overview/project-intro.md) - 프로젝트 개요
2. [architecture.md](01-overview/architecture.md) - 시스템 구조 이해
3. [docker-environment.md](03-infrastructure/docker-environment.md) - 환경 설정 및 실행
4. [database-schema.md](02-development/database-schema.md) - DB 구조 파악

### 2. 개발 시작하는 경우
1. [team-conventions.md](01-overview/team-conventions.md) - 팀 규칙 숙지
2. [business-rules.md](02-development/business-rules.md) - 비즈니스 로직 이해
3. [testing-guide.md](04-testing/testing-guide.md) - 테스트 작성 방법
4. 담당 서비스의 API 명세 확인

### 3. 특정 서비스 개발하는 경우
1. [service-status.md](03-infrastructure/service-status.md) - 현재 구현 상황 확인
2. [completed-work.md](06-work-log/completed-work.md) - 관련 완료 작업 확인
3. 관련 Issue/PR 문서 참조
4. [troubleshooting.md](04-testing/troubleshooting.md) - 자주 발생하는 문제 확인

### 4. 문제 발생 시
1. [troubleshooting.md](04-testing/troubleshooting.md) - 해결 방법 검색
2. 관련 Issue/PR 문서에서 유사 사례 찾기
3. Slack/GitHub Issue로 질문

---

## 📌 문서 작성 규칙

### Issue 문서
- 파일명: `issue-{3자리번호}-{kebab-case-description}.md`
- 내용: 문제 정의, 해결 방법, 구현 내역, 테스트 결과

### PR 문서
- 파일명: `pr-{3자리번호}-{kebab-case-description}.md`
- 내용: 변경 사항, 긍정적 부분, 개선 필요 사항, 테스트 결과

### 일반 문서
- 파일명: `{kebab-case-description}.md` (소문자)
- 마크다운 형식 준수
- 목차 포함 (문서가 긴 경우)
- 관련 문서 링크 추가

---

## 🔄 문서 업데이트 정책

### 반드시 업데이트해야 하는 경우
- **작업 완료 시**: completed-work.md AND service-status.md 동시 업데이트
- **아키텍처 변경 시**: architecture.md, database-schema.md 업데이트
- **새로운 문제 해결 시**: troubleshooting.md에 추가
- **Issue/PR 완료 시**: 해당 번호의 문서 생성 또는 업데이트

### 문서 동기화
- CLAUDE.md 수정 시 관련 docs/ 파일도 업데이트
- 여러 문서가 관련된 경우 일관성 유지

---

## 📚 추가 자료

### 외부 문서
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Spring Cloud 공식 문서](https://spring.io/projects/spring-cloud)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [Kafka 공식 문서](https://kafka.apache.org/documentation/)

### 팀 Notion
- 회의록, 스프린트 계획은 팀 Notion 참조

---

**Last Updated**: 2024-11-13
**Maintainer**: one-for-logis 개발팀
