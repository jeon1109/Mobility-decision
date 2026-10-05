# 지역 상황 기반 이동 안내 — 백엔드 인수인계

> 상태: 단일 Mobility API 통합 완료.

## 1. 목표와 제품 경계

서울 CityData의 관측 사실과 AI 안내를 구조적으로 분리한다. CityData만으로는
경로·거리·소요시간·환승·요금을 계산할 수 없으므로 이를 생성하거나 단정하지 않는다.

권장 처리 흐름:

```text
CityData 조회 → 영역별 파싱·신선도 판정 → Evidence 생성
→ 규칙 기반 strategy 결정 → 검증 → 통합 응답
```

AI 실패·시간 초과 시에도 규칙 기반 결과를 즉시 반환할 수 있어야 한다.

## 2. 제안 API

```http
POST /v1/mobility-decisions
```

```json
{
  "areaCode": "POI009",
  "purpose": "GO_HOME",
  "userStates": ["TIRED"],
  "preferences": ["AVOID_CROWD", "AVOID_ROAD_DELAY"]
}
```

- `areaName`은 받지 않고 서버가 `areaCode`로 확정한다.
- 목록은 enum 검증, 중복 제거, canonical sort 후 캐시 키에 사용한다.
- 기존 `condition`은 통합 계약에서 제거한다.
- 레거시 `비`는 실제 날씨가 아니라 `MINIMIZE_WEATHER_EXPOSURE`로만 매핑한다.

MVP enum과 빈 값 의미:

- `purpose`: `COMMUTE_TO_WORK | GO_HOME | OUTING`
- `userStates`: `TIRED | CARRYING_LUGGAGE | LIMITED_WALKING | WITH_CHILD | LATE_NIGHT` 중 정확히 1개
- `preferences`: `AVOID_CROWD | MINIMIZE_WEATHER_EXPOSURE | AVOID_ROAD_DELAY`
- `userStates`는 필수 배열이며 빈 배열과 2개 이상은 validation error
- `preferences`는 필수 배열이며 `[]` 또는 복수 선택 허용
- 이동수단은 사용자 입력에서 제거했으며 서버 정책이 전체 후보를 검토
- 경로 API가 필요한 `FASTEST/FEWER_TRANSFERS/LOW_COST/SAFETY_FIRST/MINIMIZE_WALKING`은 MVP 제외

응답의 핵심 구조:

```json
{
  "schemaVersion": "1.0",
  "generatedAt": "2026-08-20T15:30:00+09:00",
  "area": { "code": "POI009", "name": "광화문·덕수궁" },
  "guidance": {
    "strategyCode": "CONSIDER_PUBLIC_TRANSIT",
    "recommendedMode": "PUBLIC_TRANSIT",
    "headline": "대중교통 이용을 우선 고려해보세요.",
    "summary": "도로가 서행 상태이며 주변 대중교통 시설이 확인됩니다.",
    "evidence": [
      {
        "type": "OBSERVATION",
        "factIds": ["roadTraffic.current"],
        "message": "도로가 평균 20km/h로 서행 상태였습니다."
      }
    ],
    "limitations": [
      {
        "code": "ROUTE_DATA_NOT_AVAILABLE",
        "message": "실제 노선과 소요시간은 별도 길찾기 서비스에서 확인해야 합니다."
      }
    ]
  },
  "situation": {
    "congestion": {
      "status": "STALE",
      "level": "여유",
      "populationMin": 14000,
      "populationMax": 16000,
      "observedAt": "2026-08-17T20:30:00+09:00",
      "source": "SEOUL_CITY_DATA"
    },
    "weather": {
      "status": "STALE",
      "temperatureCelsius": 25.4,
      "condition": null,
      "precipitationType": "없음",
      "humidityPercent": 90,
      "observedAt": "2026-08-17T20:50:00+09:00",
      "source": "SEOUL_CITY_DATA"
    },
    "roadTraffic": {
      "status": "STALE",
      "level": "서행",
      "averageSpeedKph": 20,
      "observedAt": "2026-08-17T20:55:00+09:00",
      "source": "SEOUL_CITY_DATA"
    }
  },
  "forecasts": { "weather": [], "congestion": [] },
  "alerts": [],
  "nearbyMobility": {
    "subwayStationCount": 3,
    "busStopCount": 32,
    "bikeStationCount": 10,
    "parkingLotCount": 33,
    "evChargingStationCount": 44
  }
}
```

계약 불변 조건:

- `strategyCode`: `CONSIDER_PUBLIC_TRANSIT | CONSIDER_WALK | CONSIDER_BIKE |
  CONSIDER_CAR | USE_CAUTION | INSUFFICIENT_DATA`
- `recommendedMode`: `WALK | PUBLIC_TRANSIT | BIKE | CAR | TAXI | null`
- evidence type: `OBSERVATION | USER_PREFERENCE | SYSTEM_JUDGMENT`
- `OBSERVATION.factIds`는 Evidence Registry에 존재하는 LIVE/STALE fact만 참조한다.
- `situation.congestion/weather/roadTraffic`은 항상 존재한다.
- UNAVAILABLE이면 도메인 값과 `observedAt`은 null, `source`는 유지한다.
- 현재 snapshot status는 `LIVE | STALE | UNAVAILABLE`; FORECAST는 예보 배열에서만 사용한다.
- `airQuality`는 nullable, `forecasts.weather/congestion`과 `alerts`는 항상 배열이다.
- limitation은 `{code,message}` 객체 배열이다.
- alert는 `{id,type,severity,title,message,startsAt,endsAt,source,status}`다.
- severity는 `INFO | WARNING | CRITICAL`, alert status는 `ACTIVE | EXPIRED`다.
- nearby 항목은 `{status,count,observedAt,source}`로 실제 0건과 조회 실패를 구분한다.
- 표준 오류는 `{code,message,fieldErrors,correlationId}`이며 validation은 HTTP 400,
  미지원 area는 404, 부분 provider/AI 실패는 정상 통합 응답과 status/limitation으로 표현한다.

## 3. 파싱 우선순위

### P0

- 장소/응답: `RESULT.CODE`, `AREA_CD`, `AREA_NM`
- 혼잡: `AREA_CONGEST_LVL`, `AREA_CONGEST_MSG`, `AREA_PPLTN_MIN/MAX`,
  `PPLTN_TIME`, `FCST_PPLTN`
- 도로: `AVG_ROAD_DATA.ROAD_TRAFFIC_IDX`, `ROAD_TRAFFIC_SPD`, `ROAD_MSG`,
  `ROAD_TRAFFIC_TIME`
- 날씨: `TEMP`, `HUMIDITY`, `PRECPT_TYPE`, `PRECIPITATION`, `PCP_MSG`,
  `WIND_DIRCT`, `WIND_SPD`, `WEATHER_TIME`
- 예보: `FCST24HOURS.FCST_DT`, `SKY_STTS`, `PRECPT_TYPE`, `RAIN_CHANCE`, `TEMP`
- 대기질/자외선: `PM10/PM25`, 각 INDEX, `AIR_IDX`, `UV_INDEX`
- 경고: `ACDNT_CNTRL_STTS`, `LIVE_DST_MESSAGE`, `LIVE_YNA_NEWS`

### P1 조건부

- 주변 지하철·버스 및 도착 정보
- 승하차량, 따릉이, 주차장, 충전소, 행사

도로 링크 전체, 성별·연령별 인구, 상권 결제정보는 MVP 응답과 AI 입력에서 제외한다.

## 4. 신선도와 부분 성공

현재 관측 영역은 독립적으로 `LIVE | STALE | UNAVAILABLE`을 판정한다.
`FORECAST`는 예보 배열 항목에만 사용한다.

초기 설정 후보:

- 혼잡·도로: 30분
- 날씨·대기질: 90분
- 대중교통 도착: 2분
- 따릉이·주차·충전기: 10분

공식 갱신 주기를 확인한 후 설정값으로 조정한다. 같은 날짜여도 허용 시간을
초과하면 STALE이고, 미래 시각이 clock-skew를 넘으면 UNAVAILABLE이다.

중요 규칙:

- 현재 `SKY_STTS`가 없으면 `condition=null`이다.
- `FCST24HOURS.SKY_STTS`는 현재 날씨에 복사하지 않고 FORECAST에만 넣는다.
- TEMP 누락이 혼잡·도로 파싱 실패로 전파되지 않게 영역별로 파싱한다.
- 시각은 문자열 대신 `OffsetDateTime(+09:00)`으로 정규화한다.

## 5. AI 경계와 검증

AI는 온도·속도·인구·관측시각·시설 개수·현재 날씨·경로·거리·소요시간을
자유 생성하지 않는다. 가장 안전한 출력은 코드와 evidence ID다.

```json
{
  "strategyCode": "CONSIDER_PUBLIC_TRANSIT",
  "evidenceIds": ["roadTraffic.current", "user.preferences.AVOID_CROWD"],
  "warningCodes": ["ROUTE_DATA_NOT_AVAILABLE"]
}
```

서버는 enum, evidence 존재 여부, stale 사용 방식, 이용 가능 수단, 경로 관련
금지 strategy를 검증한다. 실패 시 규칙 기반 fallback으로 교체하고 사용자 문장은
가능하면 서버 템플릿으로 생성한다.

## 6. 호환·캐시·성능

1. v1 유지
2. 공통 `CitySituation` 정규화 모델 도입
3. 단일 Mobility mapper 유지
4. 프론트 단일 Mobility 계약 사용
5. 폐기된 /api/v2 경로가 노출되지 않는지 검증

추천 캐시 키는 `areaCode + canonical user input + evidence fingerprint`를 사용한다.
TTL·최대 크기·single-flight를 지원하는 Caffeine 또는 Redis로 기존 무제한
ConcurrentHashMap을 교체한다. AI는 2~3초 제한시간과 규칙 기반 fallback을 둔다.

## 7. 구현 순서와 파일 범위

1. OpenAPI/contract fixture 동결
2. enum, `DataStatus`, Mobility request/response DTO 추가
3. 영역별 snapshot과 `CitySituation`, `FreshnessPolicy` 추가
4. `ApiService`를 HTTP client/parser/normalizer로 분리
5. `EvidenceRegistry`, `MobilityDecisionPolicy` 추가
6. `MobilityAgentService` 출력을 코드/evidence 계약으로 축소
7. `AiDecisionValidator`와 fallback 추가
8. `RecommendationService`를 정규화→정책→AI 선택→검증 흐름으로 변경
9. `RecommendationCacheService` TTL/evidence fingerprint 적용
10. `MobilityRestController`에 통합 POST와 Bean Validation 적용
11. 통합 mapper, 계약·성능 테스트 추가
12. 실제 흐름과 다른 Tool Calling 코드·문서 정리

## 8. 필수 테스트와 관측성

- 예보 상태가 현재 날씨로 복사되지 않음
- 날씨 누락 시 혼잡·도로 유지
- 같은 날 오래된 관측도 STALE
- Asia/Seoul 날짜 경계와 미래 시각
- object/array wrapper 및 부분 응답 파싱
- 존재하지 않는 AI evidence ID 거부
- 관측 fingerprint 변경 시 추천 cache miss
- 목록 순서와 무관한 동일 캐시 키
- 통합 Mobility JSON contract snapshot

추가 metric:

- `citydata.provider.duration`
- `citydata.section.status{section,status}`
- `citydata.parse.errors{section}`
- `decision.policy.duration`, `decision.ai.duration`
- `decision.ai.validation.failures`, `decision.fallback.count`
- CityData/recommendation cache hit rate와 전체 P50/P95/P99

로그에는 키·원문·개인 입력을 남기지 않고 correlation ID, 영역별 status,
observedAt, duration만 기록한다.

## 9. 개발 완료 기준

- 프론트가 AI 문장 없이 구조화된 사실 카드 전체를 렌더할 수 있다.
- 부분 실패가 다른 정상 영역을 제거하지 않는다.
- 경로 API 없이 경로·시간·거리 주장이 응답에 포함되지 않는다.
- AI가 3초 내 완료되지 않아도 규칙 기반 응답이 반환된다.
- v1 사용자는 마이그레이션 기간 동안 기존 계약을 사용할 수 있다.

## 10. 현재 구현된 선택 반영 정책

MobilityRecommendationService는 프론트 요청의 userStates, preferences와 서울 실시간 도시데이터 관측을 함께 사용한다. API 요청/응답
구조는 변경하지 않았다.

- 혼잡 회피: 현재가 약간 붐빔 이상이고 더 낮은 혼잡도 예보가 있으면
  CONSIDER_DELAYED_DEPARTURE, 없으면 CAUTION_CROWD
- 실시간 사람 수: 단일 확정값이 아니라 populationMin~populationMax 추정 범위를
  관측 근거에 표시
- 보행 제한: LIMITED_WALKING이면 WALK, BIKE 제외
- 짐/아이 동반: CARRYING_LUGGAGE 또는 WITH_CHILD이면 BIKE 제외
- 피로 등 저노력 상태: 대중교통, 택시, 자동차, 전기차 순으로 이용 가능 수단 선택
- 심야: 택시, 자동차, 전기차를 우선하고 대중교통 운행 미검증 limitation 추가
- 날씨 노출 최소화: 비·눈 관측 시 지붕이 있는 수단을 우선
- 도로 지연 회피: 도로가 서행·정체이거나 사용자가 선택하면 대중교통 우선
- 단일 사용자 상태와 모든 선호는 evidence와 priorities에 한국어 표시명으로 기록
- 목적, 사용자 상태, 혼잡도, 도로, 날씨를 조합한 동적 summary 반환
- strategyCode, evidence type, limitation code, alert severity/status는 Java enum으로 관리

현재 구현에서 추가로 사용하는 strategyCode는 CONSIDER_DELAYED_DEPARTURE,
CAUTION_CROWD이며 recommendedMode에는 EV도 포함된다. evidence type에는
USER_STATE가 포함된다.

서울 실시간 인구는 분석된 추정 범위이므로 정확한 현장 인원으로 표현하지 않는다.
관측이 오래됐거나 없으면 각각 CROWD_DATA_STALE,
CROWD_DATA_UNAVAILABLE limitation을 반환한다.