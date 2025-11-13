#!/bin/bash

# ============================================
# Notification Service Kafka Consumer Test Script
# ============================================
# Issue #35: Kafka Consumer 통합 테스트 (Docker 환경)
# 실행: bash notification-service/scripts/test-kafka-consumer.sh
#
# 사전 조건:
#   1. Docker Compose로 전체 서비스 실행 중
#   2. Kafka 브로커 실행 중 (localhost:9092)
#   3. notification-service 실행 중 (localhost:8700)
#
# 테스트 시나리오:
#   1. order.created 이벤트 발행 → 알림 생성 확인
#   2. order.created 멱등성 검증 (동일 eventId 중복 발행)
#   3. delivery.status.changed 이벤트 발행 → 알림 생성 확인
#   4. delivery.status.changed 멱등성 검증 (동일 eventId 중복 발행)

BASE_URL="http://localhost:8700/api/v1/notifications"
KAFKA_BROKER="localhost:9092"

# 색상 출력
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 파일 경로 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$(dirname "$SCRIPT_DIR")/test-results"
RESULT_FILE="$LOG_DIR/kafka-test-$(date +%Y%m%d-%H%M%S).log"

# 테스트 결과 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo "========================================" | tee $RESULT_FILE
echo "Notification Service Kafka Consumer Test" | tee -a $RESULT_FILE
echo "Start Time: $(date)" | tee -a $RESULT_FILE
echo "========================================" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

# ============================================
# Kafka 연결 확인
# ============================================
echo -e "${BLUE}[PREREQUISITE] Kafka 브로커 연결 확인${NC}" | tee -a $RESULT_FILE

# kafka-broker-api-versions.sh로 브로커 확인 (Windows는 docker exec 사용)
docker exec kafka-ofl kafka-broker-api-versions --bootstrap-server $KAFKA_BROKER > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Kafka 브로커 연결 성공${NC}" | tee -a $RESULT_FILE
else
    echo -e "${RED}❌ Kafka 브로커 연결 실패. docker-compose up -d로 Kafka를 먼저 실행하세요.${NC}" | tee -a $RESULT_FILE
    exit 1
fi

echo "" | tee -a $RESULT_FILE

# ============================================
# notification-service 연결 확인
# ============================================
echo -e "${BLUE}[PREREQUISITE] notification-service 연결 확인${NC}" | tee -a $RESULT_FILE

health_check=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8700/actuator/health)

if [ "$health_check" == "200" ]; then
    echo -e "${GREEN}✅ notification-service 실행 중${NC}" | tee -a $RESULT_FILE
else
    echo -e "${RED}❌ notification-service가 실행되지 않았습니다. 서비스를 먼저 시작하세요.${NC}" | tee -a $RESULT_FILE
    exit 1
fi

echo "" | tee -a $RESULT_FILE

# ============================================
# Test 1: order.created 이벤트 발행 → 알림 생성 확인
# ============================================
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo -e "${BLUE}[TEST $TOTAL_TESTS] order.created 이벤트 발행 → 알림 생성 확인${NC}" | tee -a $RESULT_FILE

# 고유한 eventId 생성 (Windows PowerShell 사용)
EVENT_ID="test-event-$(powershell -Command "[guid]::NewGuid().ToString()")"
ORDER_ID=$(powershell -Command "[guid]::NewGuid().ToString()")
START_HUB_ID=$(powershell -Command "[guid]::NewGuid().ToString()")
DEST_HUB_ID=$(powershell -Command "[guid]::NewGuid().ToString()")

echo "Event ID: $EVENT_ID" | tee -a $RESULT_FILE
echo "Order ID: $ORDER_ID" | tee -a $RESULT_FILE

# Kafka 메시지 생성 (JSON 형식)
KAFKA_MESSAGE=$(cat <<EOF
{
  "eventId": "$EVENT_ID",
  "occurredAt": "$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")",
  "order": {
    "orderId": "$ORDER_ID",
    "ordererInfo": "테스트주문자 / test@example.com",
    "requestingCompanyName": "공급업체",
    "receivingCompanyName": "수령업체",
    "productInfo": "테스트 상품 x 10",
    "requestDetails": "빠른 배송 부탁드립니다",
    "route": {
      "startHubId": "$START_HUB_ID",
      "startHubName": "서울센터",
      "waypointHubNames": ["대전센터"],
      "destinationHubId": "$DEST_HUB_ID",
      "destinationHubName": "부산센터"
    },
    "receiver": {
      "name": "김수령",
      "address": "부산시 해운대구",
      "slackId": "U01234567"
    },
    "hubManager": {
      "slackId": "C09QY22AMEE",
      "name": "Test Manager"
    }
  }
}
EOF
)

echo "Kafka Message:" | tee -a $RESULT_FILE
echo "$KAFKA_MESSAGE" | tee -a $RESULT_FILE

# Kafka로 메시지 발행 (docker exec 사용)
# JSON을 한 줄로 압축하여 전송
echo "$KAFKA_MESSAGE" | tr -d '\n' | tr -d '\r' | docker exec -i kafka-ofl kafka-console-producer \
  --bootstrap-server $KAFKA_BROKER \
  --topic order.created

echo "✅ Kafka 메시지 발행 완료" | tee -a $RESULT_FILE
echo "⏳ Consumer 처리 대기 중 (5초)..." | tee -a $RESULT_FILE
sleep 5

# notification-service에서 알림 생성 확인
# Note: API에 eventId로 조회하는 엔드포인트가 없으므로, 전체 조회 후 필터링
# 실제로는 MASTER 권한 필요하지만, 테스트용으로 직접 DB 조회 또는 로그 확인 필요

echo "" | tee -a $RESULT_FILE
echo -e "${YELLOW}⚠️ 알림 생성 확인은 notification-service 로그 또는 DB를 직접 확인하세요.${NC}" | tee -a $RESULT_FILE
echo -e "${YELLOW}예상 로그: 📦 Received order.created event - eventId: $EVENT_ID${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

# 수동 검증 안내
echo "수동 검증 방법:" | tee -a $RESULT_FILE
echo "1. Docker 로그 확인:" | tee -a $RESULT_FILE
echo "   docker logs notification-service | grep '$EVENT_ID'" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE
echo "2. DB 확인 (H2 Console 또는 PostgreSQL):" | tee -a $RESULT_FILE
echo "   SELECT * FROM p_notifications WHERE event_id = '$EVENT_ID';" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

PASSED_TESTS=$((PASSED_TESTS + 1))
echo -e "${GREEN}✅ 테스트 완료 (수동 검증 필요)${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

# ============================================
# Test 2: 멱등성 검증 - 동일한 eventId로 중복 발행
# ============================================
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo -e "${BLUE}[TEST $TOTAL_TESTS] 멱등성 검증 - 동일한 eventId로 중복 발행${NC}" | tee -a $RESULT_FILE

# 동일한 eventId로 다시 발행
echo "동일한 Event ID로 재발행: $EVENT_ID" | tee -a $RESULT_FILE

echo "$KAFKA_MESSAGE" | tr -d '\n' | tr -d '\r' | docker exec -i kafka-ofl kafka-console-producer \
  --bootstrap-server $KAFKA_BROKER \
  --topic order.created

echo "✅ Kafka 메시지 재발행 완료" | tee -a $RESULT_FILE
echo "⏳ Consumer 처리 대기 중 (5초)..." | tee -a $RESULT_FILE
sleep 5

echo "" | tee -a $RESULT_FILE
echo -e "${YELLOW}⚠️ 멱등성 확인: 동일한 eventId로 2번 발행했지만, 알림은 1개만 생성되어야 합니다.${NC}" | tee -a $RESULT_FILE
echo -e "${YELLOW}예상 로그: ⏭️ Event already processed (idempotency) - eventId: $EVENT_ID${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

echo "수동 검증 방법:" | tee -a $RESULT_FILE
echo "1. Docker 로그 확인 (멱등성 로그):" | tee -a $RESULT_FILE
echo "   docker logs notification-service | grep 'idempotency' | grep '$EVENT_ID'" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE
echo "2. DB 확인 (1개만 존재해야 함):" | tee -a $RESULT_FILE
echo "   SELECT COUNT(*) FROM p_notifications WHERE event_id = '$EVENT_ID';" | tee -a $RESULT_FILE
echo "   (결과: 1)" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

PASSED_TESTS=$((PASSED_TESTS + 1))
echo -e "${GREEN}✅ 테스트 완료 (수동 검증 필요)${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

# ============================================
# Test 3: delivery.status.changed 이벤트 발행 → 알림 생성 확인
# ============================================
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo -e "${BLUE}[TEST $TOTAL_TESTS] delivery.status.changed 이벤트 발행 → 알림 생성 확인${NC}" | tee -a $RESULT_FILE

# 고유한 eventId 생성
DELIVERY_EVENT_ID="test-delivery-event-$(powershell -Command "[guid]::NewGuid().ToString()")"
DELIVERY_ID=$(powershell -Command "[guid]::NewGuid().ToString()")
ORDER_ID=$(powershell -Command "[guid]::NewGuid().ToString()")

echo "Event ID: $DELIVERY_EVENT_ID" | tee -a $RESULT_FILE
echo "Delivery ID: $DELIVERY_ID" | tee -a $RESULT_FILE

# Kafka 메시지 생성 (JSON 형식)
DELIVERY_KAFKA_MESSAGE=$(cat <<EOF
{
  "eventId": "$DELIVERY_EVENT_ID",
  "occurredAt": "$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")",
  "delivery": {
    "deliveryId": "$DELIVERY_ID",
    "orderId": "$ORDER_ID",
    "previousStatus": "HUB_WAITING",
    "currentStatus": "HUB_MOVING",
    "recipientSlackId": "C09QY22AMEE",
    "recipientName": "Test Hub Manager"
  }
}
EOF
)

echo "Kafka Message:" | tee -a $RESULT_FILE
echo "$DELIVERY_KAFKA_MESSAGE" | tee -a $RESULT_FILE

# Kafka로 메시지 발행 (docker exec 사용)
echo "$DELIVERY_KAFKA_MESSAGE" | tr -d '\n' | tr -d '\r' | docker exec -i kafka-ofl kafka-console-producer \
  --bootstrap-server $KAFKA_BROKER \
  --topic delivery.status.changed

echo "✅ Kafka 메시지 발행 완료" | tee -a $RESULT_FILE
echo "⏳ Consumer 처리 대기 중 (5초)..." | tee -a $RESULT_FILE
sleep 5

echo "" | tee -a $RESULT_FILE
echo -e "${YELLOW}⚠️ 알림 생성 확인은 notification-service 로그 또는 DB를 직접 확인하세요.${NC}" | tee -a $RESULT_FILE
echo -e "${YELLOW}예상 로그: 🚚 Received delivery.status.changed event - eventId: $DELIVERY_EVENT_ID${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

echo "수동 검증 방법:" | tee -a $RESULT_FILE
echo "1. Docker 로그 확인:" | tee -a $RESULT_FILE
echo "   docker logs notification-service | grep '$DELIVERY_EVENT_ID'" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE
echo "2. DB 확인 (H2 Console 또는 PostgreSQL):" | tee -a $RESULT_FILE
echo "   SELECT * FROM p_notifications WHERE event_id = '$DELIVERY_EVENT_ID';" | tee -a $RESULT_FILE
echo "   (message_type = 'DELIVERY_STATUS_UPDATE' 확인)" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

PASSED_TESTS=$((PASSED_TESTS + 1))
echo -e "${GREEN}✅ 테스트 완료 (수동 검증 필요)${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

# ============================================
# Test 4: 멱등성 검증 - 동일한 eventId로 중복 발행 (delivery.status.changed)
# ============================================
TOTAL_TESTS=$((TOTAL_TESTS + 1))

echo -e "${BLUE}[TEST $TOTAL_TESTS] 멱등성 검증 - 동일한 eventId로 중복 발행 (delivery)${NC}" | tee -a $RESULT_FILE

# 동일한 eventId로 다시 발행
echo "동일한 Event ID로 재발행: $DELIVERY_EVENT_ID" | tee -a $RESULT_FILE

echo "$DELIVERY_KAFKA_MESSAGE" | tr -d '\n' | tr -d '\r' | docker exec -i kafka-ofl kafka-console-producer \
  --bootstrap-server $KAFKA_BROKER \
  --topic delivery.status.changed

echo "✅ Kafka 메시지 재발행 완료" | tee -a $RESULT_FILE
echo "⏳ Consumer 처리 대기 중 (5초)..." | tee -a $RESULT_FILE
sleep 5

echo "" | tee -a $RESULT_FILE
echo -e "${YELLOW}⚠️ 멱등성 확인: 동일한 eventId로 2번 발행했지만, 알림은 1개만 생성되어야 합니다.${NC}" | tee -a $RESULT_FILE
echo -e "${YELLOW}예상 로그: ⏭️ Event already processed (idempotency) - eventId: $DELIVERY_EVENT_ID${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

echo "수동 검증 방법:" | tee -a $RESULT_FILE
echo "1. Docker 로그 확인 (멱등성 로그):" | tee -a $RESULT_FILE
echo "   docker logs notification-service | grep 'idempotency' | grep '$DELIVERY_EVENT_ID'" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE
echo "2. DB 확인 (1개만 존재해야 함):" | tee -a $RESULT_FILE
echo "   SELECT COUNT(*) FROM p_notifications WHERE event_id = '$DELIVERY_EVENT_ID';" | tee -a $RESULT_FILE
echo "   (결과: 1)" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

PASSED_TESTS=$((PASSED_TESTS + 1))
echo -e "${GREEN}✅ 테스트 완료 (수동 검증 필요)${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

# ============================================
# 테스트 결과 요약
# ============================================
echo "========================================" | tee -a $RESULT_FILE
echo "Test Summary" | tee -a $RESULT_FILE
echo "========================================" | tee -a $RESULT_FILE
echo "Total Tests: $TOTAL_TESTS" | tee -a $RESULT_FILE
echo -e "${GREEN}Completed: $PASSED_TESTS${NC}" | tee -a $RESULT_FILE
echo "End Time: $(date)" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

echo -e "${GREEN}✅ Kafka Consumer 테스트 완료!${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE
echo -e "${YELLOW}📝 참고: 이 테스트는 수동 검증이 필요합니다.${NC}" | tee -a $RESULT_FILE
echo -e "${YELLOW}   Docker 로그 또는 DB를 확인하여 알림 생성 및 멱등성을 검증하세요.${NC}" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE
echo "Results saved to: $RESULT_FILE" | tee -a $RESULT_FILE
