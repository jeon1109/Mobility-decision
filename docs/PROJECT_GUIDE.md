# MOBIUS AI 프로젝트 가이드

> 이 문서는 코드를 처음 보는 사람이 **무엇을 하는 프로젝트인지**, **어디를 보면 되는지** 빠르게 파악할 수 있도록 정리한 가이드입니다.

---

## 목차

1. [프로젝트가 하는 일](#1-프로젝트가-하는-일)
2. [전체 아키텍처](#2-전체-아키텍처)
3. [AI 모듈 상세](#3-ai-모듈-상세)
4. [인증 모듈](#4-인증-모듈)
5. [RabbitMQ 모듈](#5-rabbitmq-모듈)
6. [설정 파일 안내](#6-설정-파일-안내)
7. [코드 읽는 순서 (추천)](#7-코드-읽는-순서-추천)
8. [개발 시 참고사항](#8-개발-시-참고사항)

---

## 1. 프로젝트가 하는 일

**MOBIUS AI**는 서울 시민의 이동 상황을 AI가 분석해 **이동 전략을 추천**하는 웹 애플리케이션입니다.

### 사용자 관점

1. 로그인 / 회원가입 (선택)
2. `/main`에서 내 상태(예: 피곤함), 목적(예: 출근), 지역(예: 강남역) 입력
3. AI가 실시간 도시 데이터를 참고해 **이동 수단·경로·주의사항** 제안
4. 결과 화면에서 추천 내용 확인

### 기술 관점

- **Spring AI**로 GPT와 대화
- AI가 필요하면 **Tool Calling**으로 서울시 Open API 호출
- 동일 입력은 **인메모리 캐시**로 재사용
- 부가 기능: **RAG Q&A**, **JWT 인증**, **RabbitMQ 발행**

---

## 2. 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser (Thymeleaf UI)                   │
│   login / signup / main(doro) / result / error                  │
└────────────┬───────────────────────────────┬────────────────────┘
             │ MVC                           │ REST
             ▼                               ▼
┌────────────────────────┐      ┌───────────────────────────────┐
│  ApiViewrController    │      │  MemberController             │
│  (화면 라우팅)          │      │  /v1/member/join, login ...   │
└────────────────────────┘      └───────────────┬───────────────┘
             │                                 │
             ▼                                 ▼
┌────────────────────────┐      ┌───────────────────────────────┐
│  ApiController         │      │  UserServiceImpl              │
│  /api/recommend        │      │  + TokenProvider + RedisUtil  │
│  /api/vector, /api/ask │      │  + RabbitTemplate             │
└───────────┬────────────┘      └───────────────┬───────────────┘
            │                                   │
            ▼                                   ▼
┌────────────────────────┐      ┌───────────────────────────────┐
│ RecommendationService  │      │  MySQL (users_login)          │
│  ├─ Cache              │      │  Redis (Refresh Token)        │
│  └─ MobilityAgent      │      │  RabbitMQ (member.users/joins)│
└───────────┬────────────┘      └───────────────────────────────┘
            │
            ▼
┌───────────────────────────────────────────────────────────────┐
│  MobilityAgentService (Spring AI ChatClient)                  │
│    ├─ System/User Prompt                                      │
│    └─ Tool: SeoulCityDataTool → ApiService → 서울시 Open API  │
└───────────────────────────────────────────────────────────────┘
            │
            ▼
┌───────────────────────────────────────────────────────────────┐
│  OpenAI API (gpt-4o-mini)                                     │
└───────────────────────────────────────────────────────────────┘
```

---

## 3. AI 모듈 상세

### 3.1 핵심 클래스 역할

| 클래스 | 패키지 | 역할 |
|--------|--------|------|
| `ApiViewrController` | `data.apiController` | `/`, `/signup`, `/main` 화면 반환 |
| `ApiController` | `data.apiController` | AI 추천·RAG API 처리 |
| `RecommendationService` | `data.apiService` | 캐시 → AI 호출 → JSON 파싱 |
| `MobilityAgentService` | `data.apiService` | ChatClient로 GPT 호출, Tool 연동 |
| `SeoulCityDataTool` | `config` | `@Tool` — 서울시 실시간 데이터 조회 |
| `ApiService` | `data.apiService` | 서울시 Open API HTTP 호출 |
| `SeoulAreaService` | `data.apiService` | 선택 가능한 서울 지역 10곳 목록 |
| `RecommendationCacheService` | `data.apiService` | `ConcurrentHashMap` 캐시 |
| `IdempotencyUtil` | `common` | 요청 파라미터 → SHA-256 캐시 키 |
| `AiConfig` | `config` | `SimpleVectorStore` Bean (RAG용) |

### 3.2 데이터 모델

**입력 — `MobilityContext`**

| 필드 | 예시 |
|------|------|
| `condition` | 피곤함, 급함, 여유로움 |
| `purpose` | 출근, 쇼핑, 관광 |
| `areaName` | 강남역, 홍대입구 |
| `areaCode` | POI 코드 (서울시 API용) |

**출력 — `MobilityDecision`**

| 필드 | 설명 |
|------|------|
| `recommendation` | 추천 이동 전략 |
| `reason` | 추천 이유 |
| `usedData` | 참고한 데이터 목록 |
| `priority` | 우선 고려 요소 |
| `warning` | 주의사항 |

### 3.3 AI Agent 동작 (Tool Calling)

```
MobilityAgentService
  │
  ├─ system prompt: "이동 의사결정 AI, JSON만 출력"
  ├─ user prompt: 상태/목적/지역 전달
  │
  └─ tools(seoulCityDataTool)
        │
        └─ getSeoulCityData(areaCode)
              └─ ApiService → 서울시 실시간 도시데이터 API
                    (혼잡도, 도로소통, 대중교통, 날씨)
```

AI가 스스로 "실시간 데이터가 필요하다"고 판단하면 Tool을 호출합니다.

### 3.4 RAG (Retrieval-Augmented Generation)

| API | 설명 |
|-----|------|
| `POST /api/vector` | Redis/RabbitMQ/Webhook 관련 문서 4건을 VectorStore에 저장 |
| `GET /api/ask?question=` | `QuestionAnswerAdvisor`로 저장된 문서 기반 Q&A |

RAG는 이동 추천과 **별도 기능**입니다. 학습·실험용으로 포함되어 있습니다.

### 3.5 캐시 전략

- 키: `condition + purpose + areaCode`의 SHA-256
- 저장소: JVM 인메모리 (`ConcurrentHashMap`)
- 서버 재시작 시 캐시 초기화
- 운영 환경에서는 Redis 등 외부 캐시로 교체 권장

---

## 4. 인증 모듈

### 4.1 REST API

| API | 처리 |
|-----|------|
| `POST /v1/member/join` | 이메일 중복 검사 → DB 저장 → RabbitMQ 발행 |
| `POST /v1/member/login` | 비밀번호 검증 → JWT 발급 → Redis에 Refresh 저장 |
| `POST /v1/member/reissue` | Refresh 검증 → 새 토큰 발급 |
| `GET /v1/member/logout` | Redis 토큰 삭제 |

### 4.2 주요 클래스

| 클래스 | 역할 |
|--------|------|
| `MemberController` | REST 엔드포인트 |
| `UserServiceImpl` | 가입·로그인 비즈니스 로직 |
| `TokenProvider` | JWT 생성·검증 |
| `RedisUtil` | Redis CRUD |
| `CustomUserDetailsService` | Spring Security 사용자 로드 |
| `securityConfig` | Security 필터 체인 (대부분 `permitAll`) |

### 4.3 토큰 흐름

```
로그인 성공
  → TokenProvider.generateTokenDto()
  → Access Token (응답) + Refresh Token (Redis 저장)
  → 클라이언트: Access는 헤더, Refresh는 별도 보관
```

---

## 5. RabbitMQ 모듈

### 5.1 구성 (`UserQueueConfig`)

```
Exchange: members (Topic)
  ├─ member.users  ← 가입 성공 시 uid 발행
  └─ member.joins  ← 로그인 성공 시 accessToken 발행
```

### 5.2 현재 상태

- **Publisher**: `UserServiceImpl`에서 `RabbitTemplate.convertAndSend()` 호출
- **Consumer**: 없음 (`@RabbitListener` 미구현)

### 5.3 향후 확장

6종 이벤트(로그인/가입/AI 성공·실패) 설계는 [RABBITMQ_AND_TIMEUNIT_GUIDE.md](RABBITMQ_AND_TIMEUNIT_GUIDE.md) 참고.

---

## 6. 설정 파일 안내

| 파일 | 용도 |
|------|------|
| `application.properties` | 앱명, Thymeleaf, RabbitMQ broker host |
| `application-local.yml` | 로컬 개발 전체 설정 (DB, Redis, AI, JWT) |
| `application-prod.yml` | 운영 설정 |

### 반드시 활성화할 프로필

```bash
-Dspring.profiles.active=local
```

프로필 없이 실행하면 `application-local.yml`의 MySQL·OpenAI 설정이 로드되지 않습니다.

### 주요 설정 키

| 키 | 설명 |
|----|------|
| `spring.datasource.*` | MySQL 연결 |
| `spring.data.redis.*` | Redis 연결 |
| `spring.ai.openai.*` | OpenAI API |
| `jwt.*` | JWT secret, 만료 시간 |
| `public.api.key` | 서울시 Open API Key |
| `message.exchange` / `message.queue.*` | RabbitMQ 이름 |

---

## 7. 코드 읽는 순서 (추천)

처음 프로젝트를 볼 때 아래 순서를 권장합니다.

### AI 기능 이해

1. `templates/auth/doro/doro.html` — 사용자 입력 UI
2. `ApiController.recommend()` — 요청 진입점
3. `RecommendationService.recommend()` — 캐시 + AI 호출
4. `MobilityAgentService.recommend()` — 프롬프트 + Tool
5. `SeoulCityDataTool` → `ApiService` — 외부 API
6. `MobilityDecision.java` — 응답 구조
7. `templates/auth/doro/result.html` — 결과 UI

### 인증 이해

1. `MemberController`
2. `UserServiceImpl`
3. `TokenProvider`
4. `securityConfig`

### 인프라 이해

1. `application-local.yml`
2. `UserQueueConfig`
3. `AiConfig`

---

## 8. 개발 시 참고사항

### 해야 할 것

- 실행 시 `local` 프로필 활성화
- API Key는 환경 변수로 분리
- AI 응답은 JSON 형식인지 검증 후 파싱

### 알아두면 좋은 점

| 항목 | 설명 |
|------|------|
| JWT 필터 | `JwtFilter`/`JwtSecurityConfig`가 Security 체인에 미연결 |
| Redis 키 | 로그인·재발급·로그아웃 시 사용하는 키 prefix가 다름 |
| RabbitMQ | `spring.data.rabbitmq`가 아닌 `spring.rabbitmq`가 정식 경로 |
| 포인트 API | README 구버전에만 존재, 현재 코드에는 없음 |
| chatroom | UI만 있고 WebSocket 서버 없음 |

### 패키지 네이밍 참고

| 현재 | 의미 |
|------|------|
| `data.apiController` | AI + 화면 컨트롤러 |
| `data.apiService` | AI + 외부 API 서비스 |
| `users` | 회원 도메인 |
| `redis` | JWT·Redis 관련 |
| `common` | 공통 설정·예외 |

---

## 관련 문서

- [README.md](../README.md) — 프로젝트 소개·빠른 시작
- [TECH_STACK.md](TECH_STACK.md) — **사용 기술 정리**
- [RABBITMQ_AND_TIMEUNIT_GUIDE.md](RABBITMQ_AND_TIMEUNIT_GUIDE.md) — RabbitMQ 이벤트 설계
