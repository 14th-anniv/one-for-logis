# Docker 실행 가이드

## 사전 준비

1. **Docker Desktop 실행**
   - Windows 시작 메뉴에서 "Docker Desktop" 실행
   - 작업 표시줄에서 Docker 아이콘이 초록색인지 확인

2. **환경 변수 확인**
   - `.env` 파일이 프로젝트 루트에 있는지 확인
   - 필수 환경 변수가 모두 설정되어 있는지 확인

## Docker로 전체 시스템 실행

### 1. 모든 서비스 빌드 및 실행

```bash
# 프로젝트 루트에서 실행
docker-compose up -d
```

**설명**:
- `up`: 서비스 시작
- `-d`: 백그라운드 실행 (detached mode)

### 2. 특정 서비스만 실행

```bash
# PostgreSQL만 실행
docker-compose up -d postgres

# Eureka Server만 실행
docker-compose up -d eureka-server

# notification-service만 실행
docker-compose up -d notification-service
```

### 3. 서비스 실행 순서 (권장)

```bash
# 1단계: 데이터베이스 실행
docker-compose up -d postgres

# 2단계: Eureka Server 실행 (30초 대기)
docker-compose up -d eureka-server
sleep 30

# 3단계: 비즈니스 서비스 실행
docker-compose up -d user-service hub-service order-service notification-service

# 4단계: Gateway 실행
docker-compose up -d gateway-service
```

## Docker 상태 확인

### 실행 중인 컨테이너 확인

```bash
docker-compose ps
```

**예상 출력**:
```
NAME                              STATUS    PORTS
oneforlogis-postgres              Up        0.0.0.0:5432->5432/tcp
oneforlogis-eureka                Up        0.0.0.0:8761->8761/tcp
oneforlogis-gateway               Up        0.0.0.0:8000->8000/tcp
oneforlogis-notification-service  Up        0.0.0.0:8700->8700/tcp
```

### 로그 확인

```bash
# 모든 서비스 로그 실시간 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f notification-service

# 마지막 100줄만 확인
docker-compose logs --tail=100 notification-service
```

### 컨테이너 내부 접속

```bash
# PostgreSQL 컨테이너 접속
docker exec -it oneforlogis-postgres psql -U root -d oneforlogis_notification

# notification-service 컨테이너 쉘 접속
docker exec -it oneforlogis-notification-service sh
```

## Eureka Dashboard 확인

브라우저에서 접속:
```
http://localhost:8761
```

**확인 사항**:
- NOTIFICATION-SERVICE가 UP 상태인지
- 다른 서비스들도 정상 등록되었는지

## 서비스 중지

### 모든 서비스 중지

```bash
# 컨테이너 중지 (데이터 유지)
docker-compose stop

# 컨테이너 중지 및 삭제 (데이터 유지)
docker-compose down

# 컨테이너, 볼륨, 네트워크 모두 삭제 (완전 초기화)
docker-compose down -v
```

### 특정 서비스만 중지

```bash
docker-compose stop notification-service
```

## 서비스 재시작

```bash
# 전체 재시작
docker-compose restart

# 특정 서비스만 재시작
docker-compose restart notification-service
```

## 이미지 다시 빌드

코드 변경 후 이미지 재빌드:

```bash
# 전체 재빌드
docker-compose build

# 특정 서비스만 재빌드
docker-compose build notification-service

# 재빌드 후 실행
docker-compose up -d --build notification-service
```

## 문제 해결

### PostgreSQL 연결 실패

```bash
# PostgreSQL 로그 확인
docker-compose logs postgres

# PostgreSQL 컨테이너 재시작
docker-compose restart postgres

# PostgreSQL 헬스체크 확인
docker inspect oneforlogis-postgres | grep -A 10 Health
```

**데이터베이스가 생성되지 않은 경우**:
```bash
# 데이터베이스 수동 생성
docker exec oneforlogis-postgres psql -U root -c "CREATE DATABASE oneforlogis_notification;"

# 생성된 데이터베이스 확인
docker exec oneforlogis-postgres psql -U root -c "\l"
```

### Eureka 등록 실패

```bash
# Eureka Server 로그 확인
docker-compose logs eureka-server

# notification-service 로그 확인
docker-compose logs notification-service

# 네트워크 연결 확인
docker exec oneforlogis-notification-service ping eureka-server
```

**Actuator health check 404 오류**:
- `spring-boot-starter-actuator` 의존성 누락
- `build.gradle`에 actuator 추가 후 재빌드 필요

### 포트 충돌

```bash
# 포트 사용 중인 프로세스 확인 (PowerShell)
netstat -ano | findstr :8700

# 프로세스 종료 (PID 확인 후)
taskkill /PID <PID> /F
```

### Windows 파일 잠금 문제

```bash
# Gradle 데몬 중지
./gradlew --stop

# build 디렉토리 수동 삭제 후 재빌드
# Windows 탐색기에서 notification-service/build 폴더 삭제
./gradlew :notification-service:clean :notification-service:build -x test
```

### 환경 변수가 적용되지 않는 경우

```bash
# .env 파일 확인
cat .env

# Docker Compose에서 환경변수 확인
docker exec oneforlogis-notification-service env | grep POSTGRES

# 환경변수 없이 직접 설정값 전달 (임시)
docker-compose down
# docker-compose.yml의 environment 섹션에 직접 값 입력 후
docker-compose up -d
```

## 개발 워크플로우

### 1. 코드 수정 후 반영

```bash
# 방법 1: 서비스 재빌드 및 재시작
./gradlew :notification-service:build
docker-compose build notification-service
docker-compose up -d notification-service

# 방법 2: 로컬에서 직접 실행 (빠름)
./gradlew :notification-service:bootRun
```

### 2. 로컬 개발 환경 (혼합 방식)

```bash
# Docker: 인프라만 실행
docker-compose up -d postgres eureka-server

# 로컬: 개발 중인 서비스만 실행
./gradlew :notification-service:bootRun
```

## Docker Compose 명령어 요약

| 명령어 | 설명 |
|--------|------|
| `docker-compose up -d` | 모든 서비스 백그라운드 실행 |
| `docker-compose ps` | 실행 중인 컨테이너 목록 |
| `docker-compose logs -f` | 로그 실시간 확인 |
| `docker-compose stop` | 모든 서비스 중지 |
| `docker-compose down` | 컨테이너 중지 및 삭제 |
| `docker-compose restart` | 모든 서비스 재시작 |
| `docker-compose build` | 이미지 재빌드 |
| `docker-compose exec <service> sh` | 컨테이너 쉘 접속 |

## 권장 개발 환경

**초기 설정 및 테스트**:
```bash
docker-compose up -d
```

**일상적인 개발**:
```bash
# 인프라만 Docker
docker-compose up -d postgres eureka-server

# 개발 중인 서비스는 로컬 실행
./gradlew :notification-service:bootRun
```

**배포 전 통합 테스트**:
```bash
docker-compose up -d
# 모든 서비스 통합 테스트 수행
```

## Health Check 확인

### notification-service

```bash
# Health endpoint 확인
curl http://localhost:8700/actuator/health

# 예상 응답 (정상)
{
  "status": "UP",
  "components": {
    "db": {"status": "UP", "database": "PostgreSQL"},
    "discoveryComposite": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 전체 서비스 Health Check

```bash
# Eureka Dashboard에서 확인
curl http://localhost:8761

# 또는 브라우저에서
http://localhost:8761
```

## 자주 발생하는 이슈와 해결

### 1. DB 연결 실패: "database does not exist"

**증상**: notification-service가 "FATAL: database 'oneforlogis_notification' does not exist" 에러 발생

**원인**: init-databases.sql 스크립트가 실행되지 않음

**해결**:
```bash
# 데이터베이스 수동 생성
docker exec oneforlogis-postgres psql -U root -c "CREATE DATABASE oneforlogis_user;"
docker exec oneforlogis-postgres psql -U root -c "CREATE DATABASE oneforlogis_hub;"
docker exec oneforlogis-postgres psql -U root -c "CREATE DATABASE oneforlogis_order;"
docker exec oneforlogis-postgres psql -U root -c "CREATE DATABASE oneforlogis_notification;"

# 서비스 재시작
docker-compose restart notification-service
```

### 2. Actuator 404 오류

**증상**: `/actuator/health` 접근 시 404 Not Found

**원인**: `spring-boot-starter-actuator` 의존성 누락

**해결**:
```gradle
// build.gradle에 추가
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
}
```
```bash
# 재빌드 및 재배포
./gradlew :notification-service:clean :notification-service:build -x test
docker-compose build notification-service
docker-compose up -d notification-service
```

### 3. 하드코딩된 DB 접속 정보

**문제**: application.yml에 기본값으로 DB 접속 정보가 노출됨

**나쁜 예**:
```yaml
datasource:
  username: ${POSTGRES_USER:root}  # 기본값 노출
  password: ${POSTGRES_PASSWORD:airgear}  # 보안 위험!
```

**좋은 예**:
```yaml
datasource:
  username: ${POSTGRES_USER}  # 환경변수 필수
  password: ${POSTGRES_PASSWORD}  # 기본값 없음
```

### 4. Gradle 빌드 실패 (Windows 파일 잠금)

**증상**: "Unable to delete directory" 에러

**해결**:
```bash
# Gradle 데몬 중지
./gradlew --stop

# Windows 탐색기에서 build 폴더 직접 삭제
# 또는 PowerShell에서
Remove-Item -Recurse -Force notification-service/build
Remove-Item -Recurse -Force common-lib/build

# 재빌드
./gradlew :notification-service:clean :notification-service:build -x test
```

## 참고: 환경변수 설정

`.env` 파일 예시:
```properties
# PostgreSQL
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=root
POSTGRES_PASSWORD=airgear

# DB 이름
USER_DB=oneforlogis_user
HUB_DB=oneforlogis_hub
ORDER_DB=oneforlogis_order
NOTIFICATION_DB=oneforlogis_notification

# Service Ports
USER_SERVICE_PORT=8100
HUB_SERVICE_PORT=8200
ORDER_SERVICE_PORT=8400
NOTIFICATION_SERVICE_PORT=8700

# Eureka & Gateway
EUREKA_SERVER_URL=http://localhost:8761/eureka
EUREKA_PORT=8761
GATEWAY_PORT=8000
```

**주의**: `.env` 파일은 `.gitignore`에 포함되어 git에 추적되지 않습니다.