# PR #44: Hub Service 조회 API 구현

## Issue Number
> closed #8
> closed #9
> closed #10

## 📝 Description
### Feat
- 허브 id로 단일 조회 API 구현
- 허브 이름으로 단일 조회 API 구현 (검색이었는데 Redis 특성상 이름으로 부분 검색 불가로 조회로 변경)
- 허브 전체 데이터 페이징 처리하여 조회 API 구현 (캐시 데이터 x)

### Refactor
- 캐시 로직 추가로 인해 domain에 있던 서비스 application 계층으로 이동 후 CacheSevice 분리
- DDD 원칙에 따라 Repository를 domain 계층과 infrastructure 계층(jpa, Impl)으로 분리

## 🌐 Test Result
- 허브 id로 단일 조회
  <img width="490" height="314" alt="image" src="https://github.com/user-attachments/assets/f083845a-18e7-4db5-a63a-a08102966e7c" />

- 허브 이름으로 단일 조회
  <img width="481" height="299" alt="image" src="https://github.com/user-attachments/assets/a19cf0ae-5c4d-4142-b423-2e1ddcf84fff" />

- 허브 전체 조회
  <img width="514" height="430" alt="image" src="https://github.com/user-attachments/assets/30d7d708-ce96-48d4-96d8-7810e517976e" />
  <img width="513" height="431" alt="image" src="https://github.com/user-attachments/assets/ea784389-5d98-48eb-8a0a-ec22ff02e79d" />

## 🔎 To Reviewer
- [refactor: DDD 원칙에 따라 hub repository 계층 구조 리팩터링](https://github.com/14th-anniv/one-for-logis/pull/44/commits/463113f7e96a0d45952b38b133ff5c8487e2b60d)

이 커밋부터 확인해주시면 됩니다!
