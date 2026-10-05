# Logout / JWT 현행 구조 분석

## 분석 결과

- JWT 생성: 실제 회원 로그인은 `TokenProvider`가 HS256 Access/Refresh Token을 생성한다. 별도로
  `JwtService`도 존재하지만 회원 로그인 흐름에서는 사용되지 않는다.
- JWT 검증: `TokenProvider`가 서명과 만료를 검증한다. 기존 `JwtFilter`는 구현돼 있었으나
  `SecurityFilterChain`에 연결되지 않았다.
- Refresh Token 저장: Redis를 사용하지만 로그인은 이메일 원문 키, 재발급은 `refresh:{userId}`,
  기존 로그아웃은 `JWT_TOKEN:{userId}`를 사용하여 서로 불일치했다.
- Redis 사용: `RedisTemplate<String, String>`을 감싼 `RedisUtil`의 단순 String key/value 방식이다.
  Redis Streams 인증 코드는 없으며 RabbitMQ는 이번 흐름에 사용하지 않는다.
- Security Filter: `JwtFilter`, `JwtAuthFilter` 두 구현이 존재한다. 실제 로그인 발급자인
  `TokenProvider`와 맞는 `JwtFilter`를 표준 필터로 선택한다.
- AuthenticationProvider: 커스텀 `AuthenticationProvider`는 없고 Spring의
  `AuthenticationManager`와 `UserDetailsService` 기반 provider를 사용한다.
- Logout API: `GET /v1/member/logout`가 있었지만 blacklist 저장 없이 잘못된 Redis 키만 삭제한다.
- Token 만료: `TokenProvider`에 Access 30분, Refresh 7일이 하드코딩되어 있으며
  `application.yml`의 15분/30일 설정과 불일치한다.
- RedisTemplate: `RedisUtil`과 `UserServiceImpl`이 직접 사용한다.

## 결정

`blacklist:{jti}`를 사용한다. 신규 Access Token에 UUID `jti`를 넣으면 JWT 원문이나 그 파생값을
Redis 키에 노출하지 않고, 고정 길이 식별자로 조회할 수 있다. 로그아웃된 기존 발급 토큰에는 jti가
없으므로 배포 이전 토큰 호환을 위해 SHA-256 token hash를 fallback 식별자로 사용한다.

키는 `refresh:{userId}`, `blacklist:{jti}`, `idempotency:{key}` 형태로 통일한다. 현재 JWT subject가
이메일이므로 여기서 `userId`는 인증 주체(subject)인 이메일을 뜻한다.

Blacklist TTL은 JWT의 `exp - 현재시각`으로 계산한다. 0 이하인 토큰은 저장하지 않는다.

Redis 장애 시 인증을 차단하는 fail-closed 정책을 사용한다. 개인 데이터와 AI 실행 권한을 다루는
Personal AI OS에서 Redis 장애를 이유로 로그아웃된 토큰을 다시 허용하는 것보다 일시적인 503이
안전하다. 로그인 또한 Refresh Token을 안전하게 저장할 수 없으므로 실패시킨다.
