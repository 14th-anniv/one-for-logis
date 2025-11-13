package com.oneforlogis.notification.infrastructure.client.gemini;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gemini API 키 검증 통합 테스트
 * 실제 Gemini API를 호출하여 API Key의 유효성을 검증합니다.
 *
 * 실행 방법:
 * - 환경 변수 설정: ENABLE_REAL_API_TESTS=true
 * - Gradle: ./gradlew test -DENABLE_REAL_API_TESTS=true
 * - IDE: Run Configuration에 환경 변수 추가
 */
@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:19092"  // 존재하지 않는 포트 (Kafka 비활성화)
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "ENABLE_REAL_API_TESTS", matches = "true")
class GeminiApiKeyIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiKeyIntegrationTest.class);

    @Autowired
    private GeminiApiClient geminiApiClient;

    @Test
    void Gemini_API_Key_Test() {
        // given
        String testPrompt = "Hello, please respond with 'API key is valid'";
        GeminiRequest request = GeminiRequest.createTextRequest(testPrompt);
        log.info("[Gemini API 키 검증] API 키 검증 시작");
        log.info("  - 테스트 프롬프트: {}", testPrompt);

        // when
        GeminiResponse response = null;
        String errorMessage = null;

        try {
            response = geminiApiClient.generateContent(request);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                String content = response.getContent();
                log.info("[Gemini API 키 검증] 성공");
                log.info("  - 응답: {}", content);
                log.info("  - Finish Reason: {}", response.getCandidates().get(0).getFinishReason());
            } else {
                log.warn("[Gemini API 키 검증] 응답이 비어있음");
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("[Gemini API 키 검증] 실패: {}", errorMessage, e);
        }

        // then
        assertThat(response).isNotNull()
                .withFailMessage("Gemini API 응답이 null입니다. 오류: " + errorMessage);

        assertThat(response.getCandidates())
                .isNotNull()
                .isNotEmpty()
                .withFailMessage("Gemini API 키가 유효하지 않거나 응답이 비어있습니다");

        assertThat(response.getContent())
                .isNotNull()
                .isNotEmpty()
                .withFailMessage("Gemini API 응답 내용이 비어있습니다");

        log.info("[Gemini API 키 검증] ✅ 검증 완료 - API 키가 유효합니다");
    }

    @Test
    void Gemini_API로_간단한_계산_요청을_수행한다() {
        // given
        String mathPrompt = "What is 15 + 27? Please answer with just the number.";
        GeminiRequest request = GeminiRequest.createTextRequest(mathPrompt);
        log.info("[Gemini API 계산 테스트] 시작");
        log.info("  - 질문: {}", mathPrompt);

        // when
        GeminiResponse response = geminiApiClient.generateContent(request);
        String answer = response.getContent();

        // then
        assertThat(response).isNotNull();
        assertThat(answer).isNotNull()
                .contains("42")
                .withFailMessage("Gemini가 올바른 답변(42)을 제공하지 않았습니다. 응답: " + answer);

        log.info("[Gemini API 계산 테스트] ✅ 성공");
        log.info("  - 답변: {}", answer);
    }
}
