package com.oneforlogis.notification.infrastructure.client.slack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slack API 키 검증 통합 테스트
 * 실제 Slack API를 호출하여 Bot Token의 유효성을 검증합니다.
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
class SlackApiAuthIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SlackApiAuthIntegrationTest.class);

    @Value("${external-api.slack.bot-token}")
    private String botToken;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Test
    void Slack_API_Key_Test() {
        // given
        log.info("[Slack API 키 검증] 토큰 검증 시작");

        // when
        SlackAuthTestResponse response = webClientBuilder
                .baseUrl("https://slack.com/api")
                .build()
                .post()
                .uri("/auth.test")
                .header("Authorization", "Bearer " + botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(SlackAuthTestResponse.class)
                .doOnSuccess(res -> {
                    log.info("[Slack API 키 검증] 성공");
                    log.info("  - 워크스페이스: {}", res.getTeam());
                    log.info("  - 사용자: {}", res.getUser());
                    log.info("  - 봇 이름: {}", res.getBot() != null ? res.getBot().getBotName() : "N/A");
                })
                .doOnError(error -> log.error("[Slack API 키 검증] 실패: {}", error.getMessage()))
                .onErrorResume(error -> {
                    log.error("[Slack API 키 검증] API 호출 실패", error);
                    return Mono.just(SlackAuthTestResponse.failure("API 호출 실패: " + error.getMessage()));
                })
                .block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.isOk()).isTrue()
                .withFailMessage("Slack API 키가 유효하지 않습니다. 응답: " + response.getError());
        assertThat(response.getTeam()).isNotNull();
        assertThat(response.getUser()).isNotNull();

        log.info("[Slack API 키 검증] ✅ 검증 완료 - 토큰이 유효합니다");
    }

    // Slack auth.test API 응답 DTO
    static class SlackAuthTestResponse {
        private boolean ok;
        private String url;
        private String team;
        private String user;
        private String teamId;
        private String userId;
        private String error;
        private BotInfo bot;

        public boolean isOk() { return ok; }
        public void setOk(boolean ok) { this.ok = ok; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getTeam() { return team; }
        public void setTeam(String team) { this.team = team; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public BotInfo getBot() { return bot; }
        public void setBot(BotInfo bot) { this.bot = bot; }

        static SlackAuthTestResponse failure(String error) {
            SlackAuthTestResponse response = new SlackAuthTestResponse();
            response.setOk(false);
            response.setError(error);
            return response;
        }
    }

    static class BotInfo {
        private String botId;
        private String botName;

        public String getBotId() { return botId; }
        public void setBotId(String botId) { this.botId = botId; }
        public String getBotName() { return botName; }
        public void setBotName(String botName) { this.botName = botName; }
    }
}
