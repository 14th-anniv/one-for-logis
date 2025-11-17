# PR #40: 허브 Update/Delete 구현

## 주요 변경사항

1. Hub Service - CRUD 완성

     - ✅ 허브 수정/삭제 API 구현
     - ✅ Redis 캐싱 적용 (@CachePut, @CacheEvict)
     - ✅ 캐시 갱신 API 추가 (/cache/refresh)

2. Bug Fix - 403 예외처리 문제 해결

     - ✅ CustomAccessDeniedHandler 추가로 AccessDeniedException 정상 처리
     - ✅ SecurityConfigBase에 handler 등록
     - ✅ GlobalExceptionHandler에도 예외 핸들러 추가

3. 전체 서비스 스캔 범위 통일

     - ✅ 모든 서비스 @SpringBootApplication(scanBasePackages = "com.oneforlogis")로 변경
     - ✅ @Import 어노테이션 제거 (common-lib 자동 스캔)

   -------------------------------------------------------------------------------

🔍 상세 리뷰

1. Redis 캐싱 구현 (RedisConfig.java)

     // ✅ 장점
     - LocalDateTime 직렬화 지원 (JavaTimeModule)
     - TTL 7일 설정
     - 캐시 키 전략 명확 ("hub:{UUID}")
     
     // ⚠️ 개선 제안

     - 캐시 일관성 이슈: refreshHubCache()에서 multiSet으로 전체 캐시를 갱신하는데, 기존 캐시된 
개별 허브 데이터와 불일치 가능성 있음. CacheManager.getCache().clear() 후 재캐싱 고려.
- 키 전략: @CachePut(key = "#hubId")는 UUID만 키로 사용하는데, refreshHubCache는 "hub:" +
UUID 형태. 키 전략 통일 필요.

2. HubService 비즈니스 로직

     // ✅ 장점
     - Soft Delete 검증 추가 (isDeleted() 체크)
     - flush()로 영속성 컨텍스트 즉시 반영
     - 트랜잭션 경계 명확
     
     // ⚠️ 주의사항

     - hubRepository.flush(): updateHub에서만 사용. createHub에서는 불필요하므로 일관성 확인 
필요.
- 캐시 키 불일치: @CachePut(key = "#result.id")와 @CachePut(key = "#hubId")가 다름. SpEL
표현식 통일 필요.

3. CustomAccessDeniedHandler

     // ✅ 장점
     - JSON 형식 응답으로 API 일관성 유지
     - 로깅으로 디버깅 용이
     
     // ⚠️ 개선 제안

     - HandlerExceptionResolver 미사용: 생성자에서 주입받지만 실제로 사용하지 않음. 제거하거나 
활용 필요.
- 응답 포맷: ApiResponse 객체를 직접 사용하면 더 깔끔 (현재는 수동 JSON 생성).

4. ErrorCode 추가

     // ✅ 장점
     - 허브 도메인별 에러 코드 추가
     - 서비스별 섹션 구분 명확
     
     // ✅ 완벽

5. scanBasePackages 변경

     // ✅ 장점
     - @Import 제거로 코드 간결
     - common-lib 빈 자동 등록
     
     // ⚠️ 트레이드오프

     - 컴포넌트 스캔 범위 확대: 의도치 않은 빈 등록 가능성. 테스트 환경에서 검증 필요.
     - 성능 영향: 스캔 범위가 넓어져 애플리케이션 시작 속도 약간 느려질 수 있음.

   -------------------------------------------------------------------------------

📝 제안사항

Priority High:

     - Redis 캐시 키 전략 통일  // 현재
       @CachePut(value = "hub", key = "#result.id")  // createHub
       @CachePut(value = "hub", key = "#hubId")      // updateHub
       refreshHubCache() -> "hub:{UUID}"             // manual set
       
       // 제안: SpEL 표현식으로 통일
       @CachePut(value = "hub", key = "'hub:' + #result.id")
       @CachePut(value = "hub", key = "'hub:' + #hubId")
     - refreshHubCache 개선  @Transactional
       public void refreshHubCache() {
           cacheManager.getCache("hub").clear();  // 기존 캐시 클리어
           List<Hub> hubs = hubRepository.findByDeletedFalse();
           hubs.forEach(hub -> redisTemplate.opsForValue()
               .set("hub:" + hub.getId(), HubResponse.from(hub), Duration.ofDays(7)));
       }

Priority Medium: 3. CustomAccessDeniedHandler 개선

     // HandlerExceptionResolver 활용하거나 제거
     // ApiResponse 객체 사용
     String json = objectMapper.writeValueAsString(
         new ApiResponse<>(false, 403, ErrorCode.FORBIDDEN_ACCESS.getMessage(), null)
     );

     - HubUpdateRequest 검증 추가  // 현재 검증 없음 -> @NotBlank, @NotNull 추가 권장
       public record HubUpdateRequest(
           @NotBlank String name,
           @NotBlank String address,
           @NotNull @DecimalMin("0") BigDecimal lat,
           @NotNull @DecimalMin("0") BigDecimal lon
       )

Priority Low: 5. Redis Config 주석 간소화 (주석이 영어/한글 혼재)

   -------------------------------------------------------------------------------

✅ 테스트 결과 확인

PR 설명에 Swagger 테스트 스크린샷이 있어 좋습니다. 추가로 확인 필요:

     - Redis 캐시 동작 검증 (Redis CLI로 키 확인)
     - 동시성 테스트 (update/delete 동시 요청)
     - 403 응답 포맷 검증

   -------------------------------------------------------------------------------

💡 종합 의견

Approve 조건부:

     - 캐시 키 전략 통일 후 머지 권장
     - 나머지는 후속 이슈로 개선 가능

장점:

     - 403 버그 해결이 깔끔함
     - 공통 설정 자동 스캔으로 보일러플레이트 제거
     - Redis 캐싱 도입으로 성능 개선 기대

우려사항:

     - 캐시 키 불일치로 인한 데이터 불일치 가능성 ⚠️
