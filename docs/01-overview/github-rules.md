# GitHub 규칙

## 기본 규칙

- 백로그 작성
- 라벨 사용
    - 라벨 규칙

        | 작업 타입 | 작업 내용 |
        | --- | --- |
        | chore | 내부 파일 수정 |
        | feat | 새로운 기능 구현 |
        | fix | 코드 오류 해결 |
        | del | 쓸모없는 코드 삭제 |
        | docs | 문서 개정 |
        | move | 프로젝트 내 파일이나 코드의 이동 |
        | rename | 파일 이름 변경 |
        | style | 코드가 아닌 스타일 변경 |
        | init | Initial commit을 하는 경우 |
        | refactor | 로직 변경 없는 코드 수정 |
        | test | 테스트 코드 |
- 이슈는 기능 단위로 생성
    - 세부 이슈는 하위로 생성
- 푸시할 때 노란 느낌표 최대한 해결하기 **⚠️**

- PR 규칙
    - 테스트 완료된 작업만 PR하기
    - PR 고봉밥으로 올리지 않기
    - 1시간 내 코드 리뷰 진행
        - 하던 일이 있어도 최대한 확인해주기
    - 1명 이상 코드 리뷰 진행 → 반영 후 merge
    - 스크럼 시간에 밀린 PR 확인하기

## 브랜치 전략

| 브랜치 | 용도 | 네이밍 예시 |
| --- | --- | --- |
| `main` | 실제 배포용 | - |
| `dev`  | 개발 통합용 | - |
| `doc/*` | 문서 작업 | `docs/#issueNum-readme-update` |
| `feature/*` | 새로운 기능 개발 | `feature/#issueNum-create-user` |
| `release/*` | 버전 | `release/1.0.0` |
| `fix/*` | 버그 수정 | `fix/#issueNum-jwt-token-error` |
| `integration/` | 통합 |  |

## 커밋 컨벤션

```java
> type: 기능 요약 (: 뒤 한 칸 뛰고)

- chore: 내부 파일 수정
- feat: 새로운 기능 구현
- fix: 코드 수정, 버그, 오류 해결
- del: 쓸모없는 코드 삭제
- docs: README나 WIKI 등의 문서 개정
- move: 프로젝트 내 파일이나 코드의 이동
- rename: 파일 이름의 변경
- style: 코드가 아닌 스타일 변경을 하는 경우
- init: Initial commit을 하는 경우
- refactor: 로직은 변경 없는 클린 코드를 위한 코드 수정
- test: 테스트 코드
```

- 규칙
    1. 제목의 첫 글자는 소문자로 시작
    2. 제목은 명령문 사용
    3. 제목과 본문을 빈 행으로 분리
    4. 제목은 50자로 제한

```java
feat: 게시글 목록 조회 API 구현

		- 에러코드 추가
		- 서비스 구현

# 12
```

---

## Issue/PR 템플릿

**<프로젝트 생성 후 삽입>**

- Issue Template

    ```markdown
    [TYPE]: 이슈 제목
    예시 **feat: 유저 생성 기능 구현**
    
    -> 커밋 컨벤션 참고해서 [type]에 맞춰 작성해주세요!
    ```

    ```markdown
    ---
    name: Issue Template
    about: 이슈 템플릿
    title: ''
    labels: ''
    assignees: ''
    
    ---
    
    ## 📝 Description
    - 진행할 작업을 설명해주세요.
    
    ## ⭐ To-do
    - [ ] 작업을 수행하기 위해 해야할 태스크를 작성해주세요.
    
    ## ✅ ETC
    - 특이사항 및 예정 개발 일정을 작성해주세요.
    ```

- PR Template
```- PR 제목-> [type]: 기능 요약
    - ex) feat: 좋아요 API 구현 → **PR** 만들 때 참고```

```## Issue Number
<!-- 작업한 이슈 번호를 명시해주세요 -->
- closed #[issue-number]

## 📝 Description
- 작업 내용에 대한 설명을 적어주세요.

## 🌐 Test Result
- local에서 postman으로 요청한 결과를 첨부합니다.

## 🔎 To Reviewer
- 리뷰 받고 싶은 포인트를 작성합니다.```
