# PR #32: UserPrincipal 및 Hub Service 초기 설정

## Issue Number
> closed #3

## 📝 Description
### common-lib
- 각 서비스에 있던 JpaAuditConfig common-lib로 분리
- common-lib에 UserPrincipal 추가 및 변환 로직 구현
  <img width="223" height="119" alt="image" src="https://github.com/user-attachments/assets/e5be78f1-c775-468f-875b-fbb955bf7dfb" />

- 추상클래스 SecurityConfigBase 추가
  각 서비스에서 상속받아 SecurityConfig 구현
  <img width="431" height="103" alt="image" src="https://github.com/user-attachments/assets/4c67c4cd-5001-433b-9591-1868915114fc" />

인가 과정 추가 필요시 하단 사진처럼 configureAuthorization 오버라이딩해서 인가 과정 추가하면 됩니다
<img width="812" height="174" alt="image" src="https://github.com/user-attachments/assets/4f45d376-abc0-42f6-a9e1-189dc485d6c0" />


- 역할 Enum 추가
  <img width="597" height="208" alt="image" src="https://github.com/user-attachments/assets/a0cececc-42b9-4f63-830b-c04853863f1c" />

### ApiResponse
- success 변수 isSuccess로 변경 및 message 변수 추가
  <img width="500" height="261" alt="image" src="https://github.com/user-attachments/assets/a5a5f73a-a3b0-4afa-8b38-3a97584bc970" />

- ResponseEntity와 ApiResponse 중복으로 인해 중첩 해제
  <img width="1503" height="584" alt="image" src="https://github.com/user-attachments/assets/324d4db5-dcc3-45a4-81e9-879681bb2991" />

> ApiResponse 변경으로 인해 GlobalExceptionHandler도 수정

### Swagger
- 스웨거 상단에 헤더값 등록 가능
  <img width="631" height="583" alt="image" src="https://github.com/user-attachments/assets/b65a3889-7503-4ebe-8530-a916788e4845" />

### Hub
- Hub 관련 엔티티 추가
- 신규 허브 생성 API 구현(#3)

## 🌐 Test Result
- 도커 컴포즈 실행 시 유레카 연결 화면
  <img width="651" height="489" alt="image" src="https://github.com/user-attachments/assets/e854536d-dac3-4556-a8ab-945d4f6248c2" />

- 신규 허브 생성 응답
  <img width="481" height="323" alt="image" src="https://github.com/user-attachments/assets/5c1a468c-049a-4398-8cf5-76073997388c" />
  <img width="1555" height="89" alt="image" src="https://github.com/user-attachments/assets/cd1c59c4-20d0-46c8-9926-ab7d1e14726b" />


## 🔎 To Reviewer
- 각 서비스 Application에 해당 어노테이션 추가하면 config 빈으로 등록 가능합니다
  @Import({
  com.oneforlogis.common.config.SwaggerConfig.class,
  com.oneforlogis.common.config.JpaAuditConfig.class
  })

- ApiResponse 변경되어 다른 모듈에 구현한 부분 있다면 Controller에 수정 부탁드립니다!
- ex) ResponseEntity<ApiResponse<T>> -> ApiResponse<T>
