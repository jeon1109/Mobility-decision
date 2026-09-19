# MOBIUS AI

**상황 기반 이동 추천 시스템** — 사용자의 상태·목적·지역을 분석하고, OpenAI와 서울시 실시간 도시데이터를 활용해 최적의 이동 전략을 제안합니다.

| 항목     | 내용                                                         |
|--------|------------------------------------------------------------|
| 프로젝트명  | `musinsaPointSystem`                                       |
| 메인 클래스 | `com.example.musinsaPointSystem.MssPointSystemApplication` |
| 기본 포트  | `9093`                                                     |
| 실행 프로필 | `local` (필수)                                               |

---

## 주요 기능

| 기능           | 설명                                                             |
|--------------|----------------------------------------------------------------|
| **이동 의사결정 지원** | 실제 이동 후보를 Java로 평가하고 Spring AI가 근거·대안·위험을 설명 |
| **장소 자동완성** | Backend Kakao Local Adapter로 출발지·목적지 검색, 좌표 및 행정구역 확인 |
| **RAG Q&A**  | VectorStore에 문서를 저장하고 질의응답 (`/api/ask`)                        |
| **회원 인증**    | JWT Access/Refresh 토큰, Redis 저장, REST API                      |

---

## 기술 스택

| 구분            | 기술                                                          |
|---------------|-------------------------------------------------------------|
| Language      | Java 21                                                     |
| Framework     | Spring Boot 3.5.15                                           |
| AI            | Spring AI 1.1.8, OpenAI |
| Database      | MySQL 8, Spring Data JPA, QueryDSL                          |
| Cache         | Redis                                                       |
| Security      | Spring Security, JWT (jjwt 0.13)                            |
| View          | Thymeleaf, Tailwind CDN                                     |
| API Docs      | springdoc-openapi (Swagger UI)                              |
| Monitoring    | Spring Boot Actuator, Micrometer                            |

> 상세 버전·클래스 매핑: [docs/TECH_STACK.md](docs/TECH_STACK.md)

---

## 시스템 흐름

```
[React Client]
    ↓ POST /api/v1/mobility-decisions (기존 계약 유지)
MobilityDecisionOrchestrator
    ├─ CurrentEvidenceUseCase (CITYDATA + 주변역 + 지하철 도착)
    ├─ MobilityCandidateProvider (Kakao Routing)
    ├─ CandidateEvaluator (결정론 평가)
    ├─ DecisionEvaluator (Spring AI 설명)
    └─ DecisionPersistenceService (짧은 DB Transaction)
```

---

## 빠른 시작

### 사전 준비

| 구성요소             | 설명                  |
|------------------|---------------------|
| JDK 21           | Java Toolchain      |
| MySQL            | 기본 `localhost:3308` |
| Redis            | Refresh Token 저장    |
| 서울시 Open API Key | 실시간 도시데이터           |
| Kakao REST API Key | 장소검색·행정구역·주변역·Routing 확인 |

### 설정

`src/main/resources/application-local.yml`을 환경에 맞게 수정합니다.

```yaml
spring:
  datasource:           # MySQL
  data.redis:           # Redis
  ai.openai.api-key:    # OpenAI Key

jwt:                    # JWT secret, 만료 시간
public.api.key:         # 서울시 CITYDATA API Key
seoul.open-api.key:     # 서울시 지하철 도착 API Key
location.kakao.api-key: # Kakao REST API Key
```

> API Key·DB 비밀번호는 환경 변수로 관리하는 것을 권장합니다.

### 빌드 & 실행

```bash
# Windows
gradlew.bat clean build
gradlew.bat bootRun --args="--spring.profiles.active=local"

# macOS / Linux
./gradlew clean build
./gradlew bootRun --args='--spring.profiles.active=local'
```

**IntelliJ 실행 시 VM 옵션**

```
-Dspring.profiles.active=local
```

> `local` 프로필 없이 실행하면 MySQL·OpenAI·JWT 설정이 로드되지 않습니다.

### 접속 URL

| URL                                         | 설명             |
|---------------------------------------------|----------------|
| http://localhost:9091/                      | 로그인            |
| http://localhost:9091/signup                | 회원가입           |
| http://localhost:9091/main                  | AI 이동 추천 (메인)  |
| http://localhost:9091/swagger-ui/index.html | Swagger API 문서 |
| http://localhost:9091/actuator/health       | Health Check   |

---

## API 목록

### 화면 (MVC)

| Method | Path                 | 설명                       |
|--------|----------------------|--------------------------|
| GET    | `/`                  | 로그인 페이지                  |
| GET    | `/signup`            | 회원가입 페이지                 |
| GET    | `/main`              | AI 추천 입력 폼               |
| POST   | `/api/recommend`     | AI 이동 추천 → `result.html` |
| POST   | `/api/vector`        | RAG 문서 4건 인덱싱            |
| GET    | `/api/ask?question=` | RAG 기반 질의응답 (JSON)       |

### 회원 (REST)

| Method | Path                 | 설명             | 응답               |
|--------|----------------------|----------------|------------------|
| POST   | `/v1/member/join`    | 회원가입           | `MemberResponse` |
| POST   | `/v1/member/login`   | 로그인            | `TokenDto`       |
| POST   | `/v1/member/reissue` | Refresh 토큰 재발급 | `TokenDto`       |
| GET    | `/v1/member/logout`  | 로그아웃           | 200 OK           |

**로그인 / 재발급 / 로그아웃** 요청 시 `Authorization: Bearer {token}` 헤더가 필요합니다.

### Mobility Decision (REST)

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/v1/places/search?q=` | 장소 자동완성 |
| POST | `/api/v1/places/resolve` | 좌표 기반 행정구역·CITYDATA 지역 판별 |
| POST | `/api/v1/mobility/context` | Location 기준 현재 Evidence 조회 |
| POST | `/api/v1/mobility/decision-cases` | 실제 후보 수집·평가·AI 설명·Decision 저장 |
| POST | `/api/v1/mobility/decision-cases/{id}/selection` | 사용자 최종 후보 선택 저장 |

DecisionCase 생성과 사용자 선택 API는 JWT 인증이 필요합니다.

---

## 위치 및 서울 교통 데이터

장소검색과 역지오코딩은 Backend `PlaceSearchPort`/`ReverseGeocodingPort` 뒤의 Kakao Adapter가 담당합니다. 선택된 좌표는 `CityDataAreaResolver`가 서울시 CITYDATA 지원 지역으로 변환합니다. 교통 데이터는 `TrafficDataProvider`를 통해 서울시 REST API에서 조회하며 미완성 MCP Client는 제거했습니다.

```text
MobilityRestController → MobilityDecisionOrchestrator
  -> CurrentEvidenceUseCase
  -> KakaoRoutingCandidateAdapter
  -> CandidateEvaluator
  -> SpringAiDecisionEvaluator
  -> DecisionPersistenceService
```

---
## AI 추천 상세

### 입력 (`MobilityContext`)

| 필드          | 예시         |
|-------------|------------|
| `condition` | 피곤함, 급함    |
| `purpose`   | 출근, 쇼핑     |
| `areaName`  | 강남역        |
| `areaCode`  | 서울시 POI 코드 |

### 출력 (`MobilityDecision`)

```json
{
  "recommendation": "추천 이동 수단/경로",
  "reason": "추천 이유",
  "usedData": [
    "참고한 데이터"
  ],
  "priority": [
    "우선 고려 요소"
  ],
  "warning": "주의사항"
}
```

### 처리 단계

1. `IdempotencyUtil` — 요청 파라미터 SHA-256 캐시 키 생성
2. `RecommendationCacheService` — 동일 요청 캐시 조회
3. `MobilityAgentService` — ChatClient + Tool Calling으로 AI 호출
4. Jackson — JSON → `MobilityDecision` 파싱 후 결과 화면 렌더링

---

## 프로젝트 구조

```
musinsaPointSystem/
├── src/main/java/com/example/musinsaPointSystem/
│   ├── MssPointSystemApplication.java
│   ├── data/                    # AI 핵심
│   │   ├── apiController/       # ApiController, ApiViewrController
│   │   └── apiService/          # MobilityAgent, Recommendation, ApiService
│   ├── config/                  # AiConfig, SeoulCityDataTool, UserQueueConfig
│   ├── dto/                     # MobilityContext, MobilityDecision
│   ├── users/                   # MemberController, UserServiceImpl
│   ├── redis/                   # TokenProvider, RedisUtil
│   └── common/                  # Security, JWT, 예외 처리
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.yml
│   └── templates/auth/doro/     # login, signup, doro, result, error
├── docs/
│   ├── TECH_STACK.md            # 사용 기술 정리
│   ├── PROJECT_GUIDE.md         # 아키텍처·코드 읽는 순서
│   └── RABBITMQ_AND_TIMEUNIT_GUIDE.md
└── build.gradle
```

---

## RabbitMQ

| 항목          | 값                 |
|-------------|-------------------|
| Exchange    | `members` (Topic) |
| Queue (가입)  | `member.users`    |
| Queue (로그인) | `member.joins`    |

가입·로그인 성공 시 `UserServiceImpl`에서 메시지를 발행합니다. Consumer(`@RabbitListener`)는 아직 없습니다.

---

## 문서

| 문서                                                                         | 내용                     |
|----------------------------------------------------------------------------|------------------------|
| [docs/TECH_STACK.md](docs/TECH_STACK.md)                                   | 사용 기술·버전·기능별 매핑        |
| [docs/PROJECT_GUIDE.md](docs/PROJECT_GUIDE.md)                             | 아키텍처, 패키지 설명, 코드 읽는 순서 |
| [docs/RABBITMQ_AND_TIMEUNIT_GUIDE.md](docs/RABBITMQ_AND_TIMEUNIT_GUIDE.md) | RabbitMQ 이벤트 설계 가이드    |

---

## 알려진 제한사항

- JWT 인증 필터가 Security 체인에 완전히 연결되지 않음 (대부분 경로 `permitAll`)
- RabbitMQ Consumer 미구현 (발행만)
- WebSocket 채팅 UI(`chatroom.html`) — 서버 핸들러 없음
- AI 캐시는 JVM 인메모리 (`ConcurrentHashMap`) — 재시작 시 초기화

---

## 라이선스

개인 학습·포트폴리오 프로젝트입니다.
