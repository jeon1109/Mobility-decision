# TimeUnit IDE 오류 & RabbitMQ 이벤트 설계 가이드

> **상위 문서:** [README.md](../README.md) · [PROJECT_GUIDE.md](PROJECT_GUIDE.md)
> 대상: Spring Boot 3.5.7 / Java 21 / `spring-boot-starter-amqp`  
> 작성 목적: IDE에서 `TimeUnit` 오류가 나도 실행은 되는 현상 해결, RabbitMQ로 인증·가입·AI 분석 이벤트 발행 전략 정리

---

## 목차

1. [TimeUnit IDE 오류](#1-timeunit-ide-오류)
2. [RabbitMQ 도입 개요](#2-rabbitmq-도입-개요)
3. [이벤트 설계 전략](#3-이벤트-설계-전략)
4. [패키지·코드 구조](#4-패키지코드-구조)
5. [설정 방법](#5-설정-방법)
6. [발행 시점 매핑](#6-발행-시점-매핑)
7. [구현 체크리스트](#7-구현-체크리스트)
8. [알려진 코드 이슈](#8-알려진-코드-이슈)

---

## 1. TimeUnit IDE 오류

### 1.1 현상

- `java.util.concurrent.TimeUnit` import 또는 `TimeUnit.MILLISECONDS` 사용 줄에 IDE(빨간 줄) 오류 표시
- `./gradlew build` 또는 애플리케이션 실행은 **정상 동작**

### 1.2 원인

`TimeUnit`은 **JDK 표준 라이브러리**입니다. 컴파일·실행이 되면 코드 자체는 문제가 없고, **IDE가 JDK 클래스 경로를 제대로 잡지 못한 경우**가 대부분입니다.

이 프로젝트 `.idea/misc.xml` 예시:

```xml
languageLevel="JDK_X"
project-jdk-name="ms-21"
```

`JDK_X`는 유효한 언어 레벨이 아니어서, IntelliJ가 `java.util.concurrent` 패키지를 인식하지 못할 수 있습니다.

### 1.3 사용 위치 (현재 코드)

| 파일 | 용도 |
|------|------|
| `RedisUtil.java` | `setValue(key, value, timeout, TimeUnit unit)` |
| `UserServiceImpl.java` | refresh token 재발급 시 Redis TTL (`TimeUnit.MILLISECONDS`) |

올바른 import:

```java
import java.util.concurrent.TimeUnit;
```

### 1.4 해결 방법 (IntelliJ IDEA)

1. **File → Project Structure → Project**
   - **SDK**: JDK 21 (`ms-21` 또는 로컬 JDK 21)
   - **Language level**: **21** (JDK_X 아님)
2. **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**
   - **Gradle JVM**: JDK 21
   - **Reload All Gradle Projects** 실행
3. `misc.xml`의 `languageLevel`이 **JDK_21**로 바뀌었는지 확인
4. 여전히 오류면 **File → Invalidate Caches → Invalidate and Restart**
5. import가 `java.util.concurrent.TimeUnit`인지 재확인 (오타·다른 클래스와 혼동 없음)

### 1.5 해결 방법 (VS Code / Cursor)

1. **Extension Pack for Java** 설치
2. Command Palette → **Java: Configure Java Runtime** → **JavaSE-21** 선택
3. 프로젝트 루트에서 Gradle import 완료 대기
4. **Java: Clean Java Language Server Workspace** 후 창 재시작

### 1.6 런타임 주의 (IDE와 별개)

`UserServiceImpl.resolveRefreshToken()`에서 Redis TTL에 `newTokenDto.getRefreshTokenExpiresIn()`을 쓰는데, `TokenProvider.generateTokenDto()`가 **`refreshTokenExpiresIn`을 builder에 넣지 않으면** 값이 `null`이 되어 NPE가 날 수 있습니다.

reissue API를 사용할 계획이면:

- refresh 만료 시각을 **밀리초 단위 long**으로 DTO에 설정하거나
- `jwt.refresh-token-days` 등 설정값으로 TTL을 계산해 Redis에 저장

---

## 2. RabbitMQ 도입 개요

### 2.1 현재 상태

| 항목 | 상태 |
|------|------|
| Gradle 의존성 | `spring-boot-starter-amqp` **추가됨** (`build.gradle`) |
| `application-*.yml` | RabbitMQ 연결 설정 **없음** |
| Java Config / Publisher / Listener | **없음** |

### 2.2 도입 목적

다음 **6가지 비즈니스 이벤트**를 메시지 큐로 발행하고, 이후 로그·알림·통계 등은 **Consumer**가 구독하도록 분리합니다.

| 구분 | 성공 | 실패 |
|------|------|------|
| 로그인 | `auth.login.success` | `auth.login.failure` |
| 회원가입 | `auth.signup.success` | `auth.signup.failure` |
| AI 분석 | `ai.analysis.success` | `ai.analysis.failure` |

### 2.3 왜 RabbitMQ인가

- HTTP 응답과 **부가 처리(알림, 감사 로그)** 분리 → API 응답 지연 감소
- 실패 이벤트를 다른 서비스가 **비동기**로 처리 가능
- Topic Exchange + routing key로 **새 이벤트 추가 시 기존 코드 수정 최소화** (OCP)

---

## 3. 이벤트 설계 전략

### 3.1 Exchange / Queue / Routing Key

**권장: Topic Exchange 1개 + 큐 6개**

```
┌─────────────────────────────────────────────────────────┐
│  Exchange: musinsa.event  (topic, durable)            │
└─────────────────────────────────────────────────────────┘
         │ routing key              │ binding
         ▼                          ▼
  auth.login.success      →  queue.auth.login.success
  auth.login.failure      →  queue.auth.login.failure
  auth.signup.success     →  queue.auth.signup.success
  auth.signup.failure     →  queue.auth.signup.failure
  ai.analysis.success     →  queue.ai.analysis.success
  ai.analysis.failure     →  queue.ai.analysis.failure
```

- **Exchange 이름·routing key·queue 이름**은 코드에 문자열을 직접 쓰지 않고 `RabbitMqConstants` 한 곳에 정의 (하드코딩 금지 규칙).
- 나중에 `notification.email` 같은 routing key를 추가해도 Publisher 인터페이스는 유지 가능.

### 3.2 공통 메시지 형식 (Envelope)

모든 이벤트가 **동일한 JSON 구조**를 사용합니다 (DRY).

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "eventType": "AUTH_LOGIN_SUCCESS",
  "occurredAt": "2026-05-25T14:30:00+09:00",
  "userEmail": "user@example.com",
  "traceId": "optional-correlation-id",
  "payload": {
    "uid": 1,
    "reason": "비밀번호 불일치",
    "requestKey": "abc123"
  }
}
```

| 필드 | 설명 |
|------|------|
| `eventId` | UUID, 멱등·추적용 |
| `eventType` | enum 상수 (`AUTH_LOGIN_SUCCESS` 등) |
| `occurredAt` | ISO-8601 시각 |
| `userEmail` | 가능하면 이메일(식별자). 없으면 null |
| `traceId` | HTTP 요청 추적 ID (선택) |
| `payload` | 이벤트별 추가 데이터 (Map 또는 타입별 객체) |

**메시지에 넣지 말 것**

- 비밀번호, JWT 전체 문자열, OpenAI API Key, DB 비밀번호

### 3.3 EventType enum 예시

```java
public enum EventType {
    AUTH_LOGIN_SUCCESS("auth.login.success"),
    AUTH_LOGIN_FAILURE("auth.login.failure"),
    AUTH_SIGNUP_SUCCESS("auth.signup.success"),
    AUTH_SIGNUP_FAILURE("auth.signup.failure"),
    AI_ANALYSIS_SUCCESS("ai.analysis.success"),
    AI_ANALYSIS_FAILURE("ai.analysis.failure");

    private final String routingKey;
    // getter, constructor
}
```

### 3.4 Publisher 추상화 (OCP)

```
[ UserServiceImpl / RecommendationService ]
              │
              ▼
      EventPublisher (interface)
              │
              ▼
      RabbitEventPublisher (구현체)
              │
              ▼
         RabbitTemplate → Exchange + routing key
```

- 비즈니스 계층은 `EventPublisher`만 의존.
- 테스트 시 NoOp Publisher로 교체 가능.
- RabbitMQ를 Kafka 등으로 바꿀 때 **서비스 코드 변경 최소화**.

### 3.5 발행 실패 처리

| 단계 | 정책 |
|------|------|
| MVP | `try/catch` → 로그만 남기고 **본 요청(로그인·가입·AI)은 기존대로 성공/실패 처리** |
| 운영 강화 | Transactional Outbox 패턴 (DB에 이벤트 저장 후 별도 스케줄러가 MQ 전송) |

이벤트 발행 실패가 **로그인 성공 자체를 롤백하지 않도록** 하는 것이 일반적입니다.

### 3.6 Consumer (구독자)

1단계에서는 **Publisher만** 구현하고, Consumer는 예:

- 감사 로그 DB 적재
- Slack / 이메일 알림
- 관리자 대시보드 집계

를 `@RabbitListener`로 **별 클래스**에 두면 결합도가 낮아집니다.

---

## 4. 패키지·코드 구조

권장 디렉터리:

```
src/main/java/com/example/musinsaPointSystem/
└── common/
    └── messaging/
        ├── EventType.java
        ├── DomainEvent.java
        ├── EventPublisher.java          // interface
        ├── RabbitEventPublisher.java    // @Service
        └── RabbitMqConstants.java
└── config/
    └── RabbitMqConfig.java              // Exchange, Queue, Binding, Converter
```

선택 (Consumer):

```
└── common/messaging/
    └── listener/
        └── AuthEventLoggingListener.java  // @RabbitListener
```

---

## 5. 설정 방법

### 5.1 RabbitMQ 서버 (로컬 Docker)

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3-management
```

| 항목 | 값 |
|------|-----|
| AMQP 포트 | 5672 |
| 관리 UI | http://localhost:15672 |
| 기본 계정 | guest / guest |

### 5.2 `application-local.yml` 추가 예시

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    # 선택: 발행 확인
    publisher-confirm-type: correlated
    publisher-returns: true
```

운영(`application-prod.yml`)은 환경 변수로 분리 권장:

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

### 5.3 `RabbitMqConfig` 구성 요소

1. `@EnableRabbit`
2. `TopicExchange` — `musinsa.event`, durable=true
3. `Queue` 6개 — durable=true (필요 시 `x-message-ttl` 등 인자)
4. `Binding` — queue ↔ exchange ↔ routing key
5. `Jackson2JsonMessageConverter` — JSON 직렬화
6. `RabbitTemplate` — converter 설정

### 5.4 Publisher 코드 스케치

```java
@Service
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(EventType type, DomainEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMqConstants.EXCHANGE_NAME,
            type.getRoutingKey(),
            event
        );
    }
}
```

### 5.5 Consumer 코드 스케치 (선택)

```java
@Component
@RequiredArgsConstructor
public class AuthEventLoggingListener {

    @RabbitListener(queues = RabbitMqConstants.QUEUE_AUTH_LOGIN_SUCCESS)
    public void onLoginSuccess(DomainEvent event) {
        log.info("로그인 성공 이벤트: {}", event);
    }
}
```

### 5.6 RabbitMQ Management UI에서 확인

1. **Exchanges** 탭 → `musinsa.event` 생성 여부
2. **Queues** 탭 → 6개 큐 생성·binding 여부
3. 로그인 API 호출 후 **queue.auth.login.success**에 메시지 적재 여부

---

## 6. 발행 시점 매핑

현재 코드 기준으로 **어디에서 이벤트를 날릴지** 정리합니다.

### 6.1 인증 · 회원 (`UserServiceImpl`)

| 이벤트 | 발행 시점 | 현재 코드 위치 |
|--------|-----------|----------------|
| `AUTH_SIGNUP_SUCCESS` | DB `save` 성공 후 | `memberJoin()` return 직전 |
| `AUTH_SIGNUP_FAILURE` | 이메일 중복 | `DuplicateEmailException` throw 직전/후 |
| `AUTH_LOGIN_SUCCESS` | 토큰 발급·Redis 저장 완료 후 | `searchLogin()` return 직전 |
| `AUTH_LOGIN_FAILURE` | 이메일 없음, 비밀번호 불일치, `authenticate` 실패 | 각 `throw` 직전 |

**개선 권장**

- `IllegalAccessError` 대신 `LoginFailedException` 등 **RuntimeException** 사용
- REST API용 `@RestControllerAdvice`에서 실패 응답 + (선택) 실패 이벤트 발행

### 6.2 AI 분석 (`RecommendationService` / `ApiController`)

| 이벤트 | 발행 시점 | 현재 코드 위치 |
|--------|-----------|----------------|
| `AI_ANALYSIS_SUCCESS` | `MobilityDecision` 파싱·캐시 저장 후 | `recommend()` return 직전 |
| `AI_ANALYSIS_FAILURE` | AI 호출·JSON 파싱 예외 | `catch` 블록, `AiCommunicationException` 전 |

호출 API: `POST /api/recommend` (`ApiController`)

### 6.3 발행 예시 (로그인 성공)

```java
eventPublisher.publish(
    EventType.AUTH_LOGIN_SUCCESS,
    DomainEvent.builder()
        .eventType(EventType.AUTH_LOGIN_SUCCESS)
        .userEmail(inputEmail)
        .payload(Map.of("authorities", "..."))
        .build()
);
```

---

## 7. 구현 체크리스트

### 구조 · 설계

- [ ] Exchange / queue / routing key가 `RabbitMqConstants`에만 정의되어 있는가?
- [ ] `DomainEvent`, `EventType`이 한 곳에 정의되어 재사용되는가?
- [ ] 서비스가 `RabbitTemplate`이 아닌 `EventPublisher`에만 의존하는가?
- [ ] 새 이벤트 추가 시 Exchange는 유지하고 binding·enum만 추가 가능한가?

### 설정 · 운영

- [ ] `application-local.yml`에 `spring.rabbitmq` 설정 추가
- [ ] Docker RabbitMQ 기동 및 5672 포트 연결 확인
- [ ] 비밀번호·API 키가 메시지 payload에 포함되지 않는가?

### 기능 · 발행

- [ ] 로그인 성공/실패 2종 발행
- [ ] 회원가입 성공/실패 2종 발행
- [ ] AI 분석 성공/실패 2종 발행
- [ ] 발행 실패 시 본 트랜잭션은 정상 처리되는가?

### IDE · 품질

- [ ] JDK 21 + language level 21로 `TimeUnit` IDE 오류 해소
- [ ] `refreshTokenExpiresIn` null 이슈 점검 (reissue 사용 시)
- [ ] `UserServiceImpl` 생성자 `redisTemplate` 주입 정리

---

## 8. 알려진 코드 이슈

구현 RabbitMQ 전에 함께 점검하면 좋은 항목입니다.

| 이슈 | 파일 | 설명 |
|------|------|------|
| IDE `JDK_X` | `.idea/misc.xml` | `TimeUnit` 등 JDK 타입 인식 실패 가능 |
| `redisTemplate` 미주입 | `UserServiceImpl` | 생성자에 파라미터 없이 `this.redisTemplate = redisTemplate` 대입 |
| `refreshTokenExpiresIn` 미설정 | `TokenProvider` | reissue 시 Redis TTL null 위험 |
| `IllegalAccessError` | `UserServiceImpl` | 로그인 실패에 Error 계열 사용 → REST 처리·이벤트 발행에 부적합 |
| AI catch 블록 | `RecommendationService` | `throw new Exception()` 래핑 → `AiCommunicationException` 등으로 정리 권장 |

---

## 참고: 관련 파일 경로

| 주제 | 경로 |
|------|------|
| Gradle AMQP | `build.gradle` |
| Redis + TimeUnit | `redis/utils/RedisUtil.java` |
| 로그인/가입 | `users/service/impl/UserServiceImpl.java` |
| AI 추천 | `data/apiService/RecommendationService.java` |
| AI API | `data/apiController/ApiController.java` |
| 로컬 설정 | `src/main/resources/application-local.yml` |

---

## 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-05-25 | 최초 작성 (TimeUnit IDE, RabbitMQ 6종 이벤트 전략·설정) |
