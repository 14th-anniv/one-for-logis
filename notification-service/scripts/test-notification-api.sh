#!/bin/bash

# ============================================
# Notification Service API Test Script (cURL)
# ============================================
# Issue #14: notification-service REST API 검증
# 실행: bash notification-service/scripts/test-notification-api.sh

BASE_URL="http://localhost:8700/api/v1/notifications"
GATEWAY_URL="http://localhost:8000/api/v1/notifications"

# 색상 출력
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 디렉토리 생성 (notification-service 디렉토리 기준)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$(dirname "$SCRIPT_DIR")/test-results"
mkdir -p $LOG_DIR
RESULT_FILE="$LOG_DIR/api-test-$(date +%Y%m%d-%H%M%S).log"

# 테스트 결과 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo "========================================" | tee $RESULT_FILE
echo "Notification Service API Test" | tee -a $RESULT_FILE
echo "Start Time: $(date)" | tee -a $RESULT_FILE
echo "========================================" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

# 테스트 헬퍼 함수
function run_test() {
    local test_name=$1
    local method=$2
    local url=$3
    local data=$4
    local headers=$5
    local expected_status=$6

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${BLUE}[TEST $TOTAL_TESTS] $test_name${NC}" | tee -a $RESULT_FILE
    echo "Request: $method $url" | tee -a $RESULT_FILE

    # cURL 실행
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$url" \
            -H "Content-Type: application/json" \
            $headers \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$url" \
            $headers)
    fi

    # HTTP 상태 코드 추출
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    # 결과 출력
    echo "Response Status: $http_code" | tee -a $RESULT_FILE
    echo "Response Body:" | tee -a $RESULT_FILE
    echo "$body" | jq '.' 2>/dev/null | tee -a $RESULT_FILE || echo "$body" | tee -a $RESULT_FILE

    # 검증
    if [ "$http_code" == "$expected_status" ]; then
        echo -e "${GREEN}✅ PASS${NC}" | tee -a $RESULT_FILE
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAIL (Expected: $expected_status, Got: $http_code)${NC}" | tee -a $RESULT_FILE
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi

    echo "" | tee -a $RESULT_FILE
    sleep 1
}

# ============================================
# Test 1: 주문 알림 발송 (Internal API - No Auth)
# ============================================
# Windows 환경: PowerShell로 UUID 생성
ORDER_ID=$(powershell -Command "[guid]::NewGuid().ToString()")
ORDER_DATA=$(cat <<EOF
{
  "orderId": "$ORDER_ID",
  "orderSummary": "주문자: 테스트업체",
  "supplierCompanyName": "공급업체명",
  "receiverCompanyName": "수령업체명",
  "productSummary": "상품: 테스트상품 x 10",
  "deliveryRequest": "빠른 배송 요청",
  "departureHubName": "경기남부 허브",
  "transitHubNames": ["대전 허브", "대구 허브"],
  "arrivalHubName": "부산 허브",
  "destinationAddress": "부산시 해운대구",
  "deliveryPersonInfo": "배송담당: 홍길동",
  "recipientSlackId": "U123456",
  "recipientName": "부산허브 관리자"
}
EOF
)

run_test \
    "주문 알림 발송 (POST /order)" \
    "POST" \
    "$BASE_URL/order" \
    "$ORDER_DATA" \
    "" \
    "200"

# ============================================
# Test 1-1: 실제 Slack 채널 발송 테스트 (Optional)
# ============================================
# Note: 실제 Slack 채널 ID로 메시지 발송 (C09QY22AMEE)
REAL_ORDER_ID=$(powershell -Command "[guid]::NewGuid().ToString()")
REAL_SLACK_DATA=$(cat <<EOF
{
  "orderId": "$REAL_ORDER_ID",
  "ordererInfo": "Kim / kim@test.com",
  "requestingCompanyName": "Supplier Co",
  "receivingCompanyName": "Receiver Co",
  "productInfo": "Test Product x 10",
  "requestDetails": "Fast delivery please",
  "departureHub": "Gyeonggi South",
  "waypoints": ["Daejeon", "Daegu"],
  "destinationHub": "Busan",
  "destinationAddress": "Haeundae-gu, Busan",
  "deliveryPersonInfo": "Hong / U999999",
  "recipientSlackId": "C09QY22AMEE",
  "recipientName": "Notification Channel"
}
EOF
)

run_test \
    "실제 Slack 채널 발송 (POST /order - Real Slack)" \
    "POST" \
    "$BASE_URL/order" \
    "$REAL_SLACK_DATA" \
    "" \
    "201"

# ============================================
# Test 2: 수동 메시지 발송 - 권한 없음 (Auth Required)
# ============================================
MANUAL_DATA=$(cat <<EOF
{
  "recipientSlackId": "U789012",
  "recipientName": "수신자 이름",
  "messageContent": "테스트 메시지입니다."
}
EOF
)

run_test \
    "수동 메시지 발송 - 권한 없음 (POST /manual)" \
    "POST" \
    "$BASE_URL/manual" \
    "$MANUAL_DATA" \
    "" \
    "403"

# ============================================
# Test 3: 알림 단일 조회 (Auth Required)
# ============================================
# 실제 ID는 DB에서 조회 필요, 여기서는 임의 UUID 사용
NOTIFICATION_ID=$(powershell -Command "[guid]::NewGuid().ToString()")

run_test \
    "알림 단일 조회 - 권한 없음 (GET /{id})" \
    "GET" \
    "$BASE_URL/$NOTIFICATION_ID" \
    "" \
    "" \
    "403"

# ============================================
# Test 4: 알림 목록 조회 (MASTER Only)
# ============================================
run_test \
    "알림 목록 조회 - 권한 없음 (GET /?page=0&size=10)" \
    "GET" \
    "$BASE_URL?page=0&size=10&sortBy=createdAt&direction=DESC" \
    "" \
    "" \
    "403"

# ============================================
# Test 5: 외부 API 로그 전체 조회 (MASTER Only)
# ============================================
run_test \
    "외부 API 로그 전체 조회 - 권한 없음 (GET /api-logs)" \
    "GET" \
    "$BASE_URL/api-logs" \
    "" \
    "" \
    "403"

# ============================================
# Test 6: 외부 API 로그 Provider별 조회 (MASTER Only)
# ============================================
run_test \
    "외부 API 로그 Provider별 조회 - 권한 없음 (GET /api-logs/provider/SLACK)" \
    "GET" \
    "$BASE_URL/api-logs/provider/SLACK" \
    "" \
    "" \
    "403"

# ============================================
# Test 7: 외부 API 로그 메시지 ID별 조회 (MASTER Only)
# ============================================
MESSAGE_ID=$(powershell -Command "[guid]::NewGuid().ToString()")
run_test \
    "외부 API 로그 메시지별 조회 - 권한 없음 (GET /api-logs/message/{id})" \
    "GET" \
    "$BASE_URL/api-logs/message/$MESSAGE_ID" \
    "" \
    "" \
    "403"

# ============================================
# 테스트 결과 요약
# ============================================
echo "========================================" | tee -a $RESULT_FILE
echo "Test Summary" | tee -a $RESULT_FILE
echo "========================================" | tee -a $RESULT_FILE
echo "Total Tests: $TOTAL_TESTS" | tee -a $RESULT_FILE
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}" | tee -a $RESULT_FILE
echo -e "${RED}Failed: $FAILED_TESTS${NC}" | tee -a $RESULT_FILE
echo "End Time: $(date)" | tee -a $RESULT_FILE
echo "" | tee -a $RESULT_FILE

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed!${NC}" | tee -a $RESULT_FILE
else
    echo -e "${RED}❌ Some tests failed. Check $RESULT_FILE for details.${NC}" | tee -a $RESULT_FILE
fi

echo "" | tee -a $RESULT_FILE
echo "Results saved to: $RESULT_FILE" | tee -a $RESULT_FILE