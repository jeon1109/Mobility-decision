# 사용 기술 정리 (Tech Stack)

> **MOBIUS AI** 프로젝트에서 실제로 사용한 기술·라이브러리·외부 서비스를 정리한 문서입니다.  
> 기준: `build.gradle`, `application-local.yml`, 소스 코드 (2026-06 기준)

**관련 문서:** [README.md](../README.md) · [PROJECT_GUIDE.md](PROJECT_GUIDE.md)

---

## 목차

1. [기술 스택 한눈에 보기](#1-기술-스택-한눈에-보기)
2. [언어 & 빌드](#2-언어--빌드)
3. [백엔드 프레임워크](#3-백엔드-프레임워크)
4. [AI / LLM](#4-ai--llm)
5. [데이터베이스 & 영속성](#5-데이터베이스--영속성)
6. [캐시 & 메시지 큐](#6-캐시--메시지-큐)
7. [인증 & 보안](#7-인증--보안)
8. [프론트엔드 & UI](#8-프론트엔드--ui)
9. [외부 API & 연동](#9-외부-api--연동)
10. [모니터링 & 개발 도구](#10-모니터링--개발-도구)
11. [테스트](#11-테스트)
12. [기술별 사용 위치 매핑](#12-기술별-사용-위치-매핑)

---

## 1. 기술 스택 한눈에 보기

```
┌─────────────────────────────────────────────────────────────┐
│  Presentation    Thymeleaf · Tailwind CDN · REST API        │
├─────────────────────────────────────────────────────────────┤
│  Application     Spring Boot 3.5.7 · Spring MVC · Security  │
├─────────────────────────────────────────────────────────────┤
│  AI              Spring AI 1.0 · OpenAI · Tool Calling · RAG│
├─────────────────────────────────────────────────────────────┤
│  Data            MySQL · JPA · QueryDSL · Redis             │
├─────────────────────────────────────────────────────────────┤
│  Messaging       RabbitMQ (Spring AMQP)                     │
├─────────────────────────────────────────────────────────────┤
│  Infra           Java 21 · Gradle · Actuator · p6spy      │
└─────────────────────────────────────────────────────────────┘
```

| 분류 | 사용 기술 |
|------|-----------|
| Language | **Java 21** |
| Framework | **Spring Boot 3.5.7** |
| AI | **Spring AI 1.0**, **OpenAI GPT-4o-mini**, **Embedding (text-embedding-3-small)** |
| DB | **MySQL 8**, **Spring Data JPA**, **Hibernate**, **QueryDSL 5.0** |
| Cache | **Redis** (Spring Data Redis) |
| MQ | **RabbitMQ** (Spring AMQP) |
| Auth | **Spring Security**, **JWT (jjwt 0.13)** |
| View | **Thymeleaf** |
| API 문서 | **springdoc-openapi (Swagger UI)** |
| HTTP Client | **WebClient** (Spring WebFlux) |
| Build | **Gradle 8.x** |
| Utils | **Lombok**, **Jackson**, **Apache Commons Codec** |

---

## 2. 언어 & 빌드

| 기술 | 버전 | 프로젝트에서의 역할 |
|------|------|---------------------|
| **Java** | 21 | 메인 개발 언어 (Records, 최신 문법 활용) |
| **Gradle** | 8.14+ (Wrapper) | 빌드·의존성 관리 |
| **Spring Boot Gradle Plugin** | 3.5.7 | 실행 JAR, bootRun, 의존성 BOM |
| **Lombok** | 1.18.x | `@Slf4j`, `@RequiredArgsConstructor`, `@Builder` 등 보일러플레이트 제거 |

---

## 3. 백엔드 프레임워크

| 기술 | 라이브러리 | 프로젝트에서의 역할 |
|------|-----------|---------------------|
| **Spring Boot Web** | `spring-boot-starter-web` | REST API (`MemberController`), MVC (`ApiController`) |
| **Spring MVC** | (Web 내장) | `@Controller`, `@RestController`, `@RequestMapping` |
| **Spring Validation** | `spring-boot-starter-validation` | 요청 데이터 검증 (`@Valid` 등) |
| **Spring WebFlux** | `spring-boot-starter-webflux` | `WebClient` 기반 비동기 HTTP 호출 |
| **Spring Boot Actuator** | `spring-boot-starter-actuator` | `/actuator/health`, `/actuator/metrics` |
| **Jackson** | `jackson-databind` | AI JSON 응답 파싱 (`MobilityDecision`), API 직렬화 |

### 아키텍처 패턴

- **Layered Architecture**: Controller → Service → Repository
- **REST + MVC 혼합**: API는 JSON, AI 화면은 Thymeleaf
- **도메인 분리**: `users`, `data`, `redis`, `common`, `config`

---

## 4. AI / LLM

| 기술 | 버전 / 모델 | 프로젝트에서의 역할 |
|------|-------------|---------------------|
| **Spring AI** | BOM 1.0.0 | AI 통합 프레임워크 |
| **spring-ai-starter-model-openai** | 1.0.0 | OpenAI Chat 모델 연동 |
| **spring-ai-advisors-vector-store** | 1.0.0 | RAG (`QuestionAnswerAdvisor`) |
| **OpenAI Chat** | `gpt-4o-mini` | 이동 추천 AI Agent |
| **OpenAI Embedding** | `text-embedding-3-small` | RAG 문서 벡터 임베딩 |
| **ChatClient** | Spring AI | 프롬프트 구성·AI 호출 (`MobilityAgentService`) |
| **Tool Calling** | `@Tool` | AI가 서울시 API를 직접 호출 (`SeoulCityDataTool`) |
| **VectorStore** | `SimpleVectorStore` | 인메모리 RAG 저장소 (`AiConfig`) |
| **Micrometer Observation** | (Actuator 연동) | AI 호출 시간·성공/실패 관측 |

### AI 기능 3가지

| 기능 | 기술 조합 | 엔드포인트 |
|------|-----------|-----------|
| **이동 추천 Agent** | ChatClient + Tool Calling + JSON 출력 | `POST /api/recommend` |
| **RAG 문서 인덱싱** | VectorStore + EmbeddingModel | `POST /api/vector` |
| **RAG Q&A** | ChatClient + QuestionAnswerAdvisor | `GET /api/ask` |

### AI 관련 핵심 클래스

| 클래스 | 사용 기술 |
|--------|-----------|
| `MobilityAgentService` | ChatClient, Tool Calling, Observation |
| `SeoulCityDataTool` | `@Tool` (Spring AI Function Calling) |
| `RecommendationService` | Jackson ObjectMapper (JSON → DTO) |
| `RecommendationCacheService` | `ConcurrentHashMap` (인메모리 캐시) |
| `AiConfig` | SimpleVectorStore, EmbeddingModel |
| `IdempotencyUtil` | Apache Commons Codec (SHA-256 캐시 키) |

---

## 5. 데이터베이스 & 영속성

| 기술 | 버전 | 프로젝트에서의 역할 |
|------|------|---------------------|
| **MySQL** | 8.x (포트 3308) | 회원 정보 저장 (`users_login` 테이블) |
| **MySQL Connector/J** | 9.4.x | JDBC 드라이버 |
| **H2 Database** | (runtime) | 테스트·로컬 대체용 (의존성만 포함) |
| **Spring Data JPA** | 3.5.x | ORM, Repository 패턴 |
| **Hibernate** | 6.6.x | JPA 구현체, SQL 생성 |
| **QueryDSL** | 5.0.0 (Jakarta) | 타입 안전 쿼리 (Q클래스 생성) |
| **JPA Auditing** | Spring Data | `BaseEntity` 생성·수정 시각 자동 기록 |
| **p6spy** | 1.5.8 | SQL 로깅·파라미터 바인딩 추적 |

### JPA 설정

| 설정 | 값 | 의미 |
|------|-----|------|
| `show-sql` | true | 콘솔 SQL 출력 |
| `format_sql` | true | SQL 포맷팅 |
| `open-in-view` | false | OSIV 비활성 (권장 패턴) |
| `dialect` | MySQL8Dialect | MySQL 8 방언 |

### 엔티티

| 엔티티 | 테이블 | 용도 |
|--------|--------|------|
| `Users` | `users_login` | 회원 (이메일, 비밀번호, 역할) |

---

## 6. 캐시 & 메시지 큐

### Redis

| 항목 | 내용 |
|------|------|
| **라이브러리** | `spring-boot-starter-data-redis` |
| **클라이언트** | Lettuce |
| **용도** | JWT Refresh Token 저장 |
| **핵심 클래스** | `RedisUtil`, `RedisTemplate`, `TokenProvider` |

### RabbitMQ

| 항목 | 내용 |
|------|------|
| **라이브러리** | `spring-boot-starter-amqp` |
| **프로토콜** | AMQP |
| **Exchange** | `members` (Topic) |
| **Queue** | `member.users` (가입), `member.joins` (로그인) |
| **핵심 클래스** | `RabbitConfig`, `UserQueueConfig`, `RabbitTemplate` |
| **현재 상태** | Publisher만 구현 (Consumer 미구현) |

---

## 7. 인증 & 보안

| 기술 | 버전 | 프로젝트에서의 역할 |
|------|------|---------------------|
| **Spring Security** | 6.5.x | 인증·인가 필터 체인 |
| **JWT (jjwt)** | 0.13.0 | Access / Refresh Token 생성·검증 |
| **BCrypt** | (Security 내장) | 비밀번호 해싱 (`PasswordEncoder`) |
| **Redis** | — | Refresh Token 저장소 |
| **CustomUserDetailsService** | — | 이메일 기반 사용자 로드 |

### 인증 흐름에 쓰인 기술

```
회원가입  → BCrypt 해싱 → JPA save → RabbitMQ 발행
로그인    → AuthenticationManager → JWT 발급 → Redis 저장
재발급    → Refresh Token 검증 → 새 JWT → Redis 갱신
로그아웃  → Redis 토큰 삭제
```

### Security 관련 클래스

| 클래스 | 역할 |
|--------|------|
| `securityConfig` | SecurityFilterChain, CSRF off, STATELESS |
| `TokenProvider` | JWT 생성·검증 (jjwt) |
| `JwtService` | 별도 JWT 구현 (HS256) |
| `JwtFilter` | 요청마다 JWT 검증 필터 |
| `JwtAuthenticationEntryPoint` | 401 응답 처리 |
| `JwtAccessDeniedHandler` | 403 응답 처리 |

---

## 8. 프론트엔드 & UI

| 기술 | 프로젝트에서의 역할 |
|------|---------------------|
| **Thymeleaf** | 서버 사이드 HTML 렌더링 |
| **Tailwind CSS** (CDN) | UI 스타일링 (로즈/핑크 MOBIUS AI 테마) |
| **HTML Form** | AI 추천 입력 → `POST /api/recommend` |
| **WebSocket** (의존성만) | `chatroom.html` UI (서버 핸들러 미구현) |

### Thymeleaf 템플릿

| 파일 | 화면 |
|------|------|
| `login.html` | 로그인 |
| `signup.html` | 회원가입 |
| `doro.html` | AI 추천 입력 |
| `result.html` | AI 추천 결과 |
| `error.html` | 에러 페이지 |
| `chatroom.html` | 채팅 UI (미완성) |

---

## 9. 외부 API & 연동

| 외부 서비스 | 연동 방식 | 프로젝트에서의 역할 |
|-------------|-----------|---------------------|
| **OpenAI API** | Spring AI (`spring.ai.openai`) | GPT 채팅·임베딩 |
| **서울시 Open API** | WebClient HTTP | 실시간 도시데이터 (혼잡도·교통·날씨) |

### 서울시 API 연동 구조

```
AI (Tool Calling)
  → SeoulCityDataTool.getSeoulCityData(areaCode)
    → ApiService.getCityDataSummary(areaCode)
      → WebClient → 서울시 실시간 도시데이터 Open API
```

| 클래스 | 역할 |
|--------|------|
| `ApiService` | WebClient로 공공 API 호출 |
| `SeoulCityDataTool` | AI Tool 인터페이스 |
| `SeoulAreaService` | 서울 10개 POI 목록 제공 |

---

## 10. 모니터링 & 개발 도구

| 기술 | 프로젝트에서의 역할 |
|------|---------------------|
| **Spring Boot Actuator** | Health check, Metrics |
| **Micrometer Observation** | AI 호출 관측 (`MobilityAgentService`) |
| **p6spy** | SQL 쿼리·파라미터 로깅 |
| **springdoc-openapi** | Swagger UI API 문서 (`/swagger-ui/index.html`) |
| **SLF4J + Logback** | 애플리케이션 로깅 |
| **IntelliJ IDEA** | IDE (`.idea/` 설정) |

### 로깅 레벨 (local)

| 패키지 | 레벨 |
|--------|------|
| `org.springframework.security` | DEBUG |
| `org.hibernate.SQL` | DEBUG |
| `org.hibernate.type.descriptor.sql.BasicBinder` | TRACE |

---

## 11. 테스트

| 기술 | 프로젝트에서의 역할 |
|------|---------------------|
| **JUnit 5** | 단위·통합 테스트 |
| **Spring Boot Test** | `@SpringBootTest` |
| **Spring Security Test** | Security 테스트 지원 |
| **Spring AMQP Test** | RabbitMQ 테스트 지원 |

> 현재 `cheeseTest.java`만 존재하며, 실질적인 테스트 코드는 미작성 상태입니다.

---

## 12. 기술별 사용 위치 매핑

### 기능 → 기술 매트릭스

| 기능 | Spring Boot | Spring AI | JPA | Redis | RabbitMQ | JWT | Thymeleaf | WebClient |
|------|:-----------:|:---------:|:---:|:-----:|:--------:|:---:|:---------:|:---------:|
| AI 이동 추천 | ✅ | ✅ | | | | | ✅ | |
| RAG Q&A | ✅ | ✅ | | | | | | |
| 서울시 API | ✅ | ✅ | | | | | | ✅ |
| 회원가입 | ✅ | | ✅ | | ✅ | | | |
| 로그인 | ✅ | | ✅ | ✅ | ✅ | ✅ | | |
| 토큰 재발급 | ✅ | | | ✅ | | ✅ | | |
| 로그아웃 | ✅ | | | ✅ | | ✅ | | |
| API 문서 | ✅ | | | | | | | |
| Health Check | ✅ | | | | | | | |

### 패키지 → 기술 매핑

| 패키지 | 주요 기술 |
|--------|-----------|
| `data.apiController` | Spring MVC, Spring AI (VectorStore, ChatClient) |
| `data.apiService` | Spring AI, WebClient, Jackson, ConcurrentHashMap |
| `config` | Spring AI (@Tool, VectorStore), RabbitMQ (@Bean) |
| `users` | JPA, Spring Security, RabbitMQ |
| `redis` | Redis, JWT (jjwt) |
| `common` | Spring Security, JWT, 예외 처리 |
| `dto` | Java Records, Jackson |

---

## 버전 요약표

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 3.5.7 |
| Spring AI | 1.0.0 |
| OpenAI Chat Model | gpt-4o-mini |
| OpenAI Embedding Model | text-embedding-3-small |
| MySQL | 8.x |
| QueryDSL | 5.0.0 |
| JWT (jjwt) | 0.13.0 |
| springdoc-openapi | 2.8.16 |
| Gradle | 8.14+ |
| Server Port | 9091 |

---

## 포트폴리오 / 이력서용 한 줄 정리

> **Java 21 · Spring Boot 3.5 · Spring AI · OpenAI GPT-4o-mini** 기반 상황 인식 이동 추천 시스템.  
> **Tool Calling**으로 서울시 실시간 API 연동, **RAG** 질의응답, **JWT + Redis** 인증, **RabbitMQ** 비동기 메시지 발행 구현.

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-06-12 | 최초 작성 |
