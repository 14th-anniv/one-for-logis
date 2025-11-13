# MSA 기반 B2B 대상 물류 관리 및 배송 플랫폼 - One For logis 🚚
<a href="https://club-project-one.vercel.app/" target="_blank">
<img src="" alt="이미지가 있을 시 작성" width="100%"/>
</a>

<br>
<br>

# 📦 Project Overview (프로젝트 개요)
전국 허브 간 물류 이동 경로를 관리하고, 배송 상태를 실시간 추적할 수 있는 MSA 기반 물류 관리 플랫폼. <br>
허브 간 최단 경로 탐색(다익스트라 적용), Redis 캐싱 및 Kafka 이벤트 기반 구조를 통해 안정성과 확장성을 확보합니다.

## 🗓️ 개발 기간
2025.10.31 ~ 2025.11.13

</br>

# ⚒️ Technology Stack (기술 스택)

[![Java](https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot%203.3.2-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-228B22?style=for-the-badge&logo=java&logoColor=white)](https://spring.io/projects/spring-data-jpa)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-security)
[![Kafka](https://img.shields.io/badge/Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)

[![PostgreSQL](https://img.shields.io/badge/PostgreSQL%2017-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

[![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white)](https://www.jetbrains.com/idea/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/)
[![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)
[![Slack](https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white)](https://slack.com/)
[![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)](https://www.notion.so/)

[![Gemini](https://img.shields.io/badge/Gemini-8A2BE2?style=for-the-badge)](https://gemini.com/)


<br>
<br>

# ⭐ Team Members (팀원)
| name | role | 담당 파트 |                                                                       Github                                                                        |
|:----:|:----:|:-----:|:---------------------------------------------------------------------------------------------------------------------------------------------------:|
| 김다윤  |  팀장  | 허브, 공통모듈 | <a href="https://github.com/dyun23"><img src="https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=Github&logoColor=white"></a> |
| 박근용  |  멤버  | Slack 알림 | <a href="https://github.com/GoodNyong"><img src="https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=Github&logoColor=white"></a>    |
| 박진성  |  멤버  | 회원 |  <a href="https://github.com/Sp-PJS"><img src="https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=Github&logoColor=white"></a>  |
| 설정아  |  멤버  | 주문 |  <a href="https://github.com/AlkongDalkonge"><img src="https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=Github&logoColor=white"></a>  |
| 안소나  |  멤버  | 업체, 상품 |  <a href="https://github.com/sonaanweb"><img src="https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=Github&logoColor=white"></a>   |
| 이다인  |  멤버  | 배송 |  <a href="https://github.com/dain391"><img src="https://img.shields.io/badge/Github-181717?style=for-the-badge&logo=Github&logoColor=white"></a>    |

<br>
<br>

# 🗝️ Key Features (주요 기능)
- **기능**
    - 기능 설명 작성
- **기능**
    - 기능 설명 작성

<br>
<br>

# 🌐 ERD 및 시스템 아키텍처
<img width="2000" height="1200" alt="Image" src="https://github.com/user-attachments/assets/8655eac9-07ce-42a3-9f91-01082bb34747" />
<img width="900" height="700" alt="Image" src="https://github.com/user-attachments/assets/d70e94f2-dd90-490e-a9e1-f0a8f2aee5f1" />


# 📂 Project Structure (프로젝트 구조)
```plaintext
각 서비스 모듈은 4계층 레이어드 아키텍처를 기본으로 합니다.
└─main
    ├─java
    │  └─com
    │      └─oneforlogis
    │          └─service-module
    │              ├─application
    │              │  └─dto
    │              │      ├─request
    │              │      └─response
    │              ├─domain
    │              │  ├─model
    │              │  └─repository
    │              ├─global
    │              ├─infrastructure
    │              │  ├─client
    │              │  │  └─dto
    │              │  └─persistence
    │              └─presentation
    │                  └─controller
    │                      └─internal
    │                          └─dto
    └─resources
```

<br>
<br>

# ✈️ Development Workflow (개발 워크플로우)

## 브랜치 전략, 커밋 컨벤션
Git Flow를 기반으로 하며, 다음과 같은 브랜치를 사용합니다.

- **Branch**
    - **전략**
  
      | Branch Type | Description                                       |
      |-------------|---------------------------------------------------|
      | `dev`       | 주요 개발 branch, `main`으로 merge 전 거치는 branch |
      | `feature`   | 각자 개발할 branch, 기능 단위로 생성하기, 할 일 issue 등록 후 branch 생성 및 작업 |

    - **네이밍**
        - `{header}/#{issue number}-feature`
        - 예) `feature/#issueNum-create-user`


- **커밋 메시지 규칙**
    ```bash
    > type: 기능 요약

      - chore: 내부 파일 수정
      - feat: 새로운 기능 구현
      - add: feat 이외의 부수적인 코드 추가, 라이브러리 추가, 새로운 파일 생성 시
      - fix: 코드 수정, 버그, 오류 해결
      - del: 쓸모없는 코드 삭제
      - docs: README나 WIKI 등의 문서 개정
      - move: 프로젝트 내 파일이나 코드의 이동
      - rename: 파일 이름의 변경
      - merge: 다른 브랜치를 merge하는 경우
      - style: 코드가 아닌 스타일 변경을 하는 경우
      - init: Initial commit을 하는 경우
      - refactor: 로직은 변경 없는 클린 코드를 위한 코드 수정

      ex) feat: 게시글 목록 조회 API 구현
    ```

<br>
<br>

# 📄 기술 문서
[API 명세서 자세히 보기](https://teamsparta.notion.site/29d2dc3ef5148147b0b4ddc502359f62?v=2a02dc3ef51480b183d6000c635a5567)