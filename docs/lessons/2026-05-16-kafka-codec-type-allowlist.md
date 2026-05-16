# KafkaCodec 타입 허용 목록(allowlist) 보안 강화

## 날짜

2026-05-16

## 관련 이슈

- #481 (KafkaCodec type header allowlist)
- #471 (Release 1.8.0 hard blockers)

## 근본 원인

`AbstractKafkaCodec.getValueType()`은 Kafka 메시지 헤더의 `bluetape4k.kafka.codec.value.type` 값을 기반으로
`Class.forName()`을 호출해 클래스를 동적으로 로드한다.

기존 구현에서는 `allowedTypePackages = emptySet()`가 **모든 클래스를 허용**하는 동작으로 설계되었다.
이는 신뢰할 수 없는 Kafka 브로커나 외부 네트워크에서 수신된 메시지에 임의 클래스 로딩(RCE) 취약점을 유발할 수 있다.

## 결정

`emptySet()` 기본값의 의미를 **deny all (모든 클래스 차단)** 으로 변경한다.

| 설정 | 1.8.0 이전 | 1.8.0+ |
|------|-----------|--------|
| `emptySet()` | 모든 클래스 허용 (unsafe) | 모든 클래스 차단 (secure default) |
| 특정 패키지 집합 | 허용 목록만 허용 | 동일 |
| `ALLOW_ALL_TYPES_UNSAFE` | — | 구 동작 복원 (opt-in) |

## 구현 변경

### `AbstractKafkaCodec`

- `ALLOW_ALL_TYPES_UNSAFE = setOf("*")` companion 상수 추가 — 구 동작 opt-in 시 사용
- `getValueType()` 로직 수정: `"*"` → bypass, `emptySet()` → deny all, 비어 있지 않은 집합 → 패키지 접두사 검사

### `JacksonKafkaCodec`

- `allowedTypePackages: Set<String> = emptySet()` 생성자 파라미터로 노출
- `final` 클래스 유지 (서브클래싱 대신 생성자 파라미터로 구성)

## 발견된 설계 실수

초기 테스트 작성 시 `JacksonKafkaCodec`을 anonymous object로 서브클래싱하려 했으나
Kotlin의 기본 `final` 클래스 제한으로 컴파일 오류 발생.
생성자 파라미터를 통한 구성 방식이 더 명확하고 관용적인 패턴이다.

## 검증

- `JacksonKafkaCodecSecurityTest` (kafka3, kafka4) 신규 추가: 4개 보안 테스트
  1. emptySet 기본값 → 헤더 클래스 거부 → null 반환
  2. 명시적 패키지 허용 → 정상 역직렬화
  3. ALLOW_ALL_TYPES_UNSAFE opt-in → 구 동작 복원
  4. 허용 목록 외 패키지 → 거부 → null 반환
- `KafkaCodecTest.JacksonCodecTest` 업데이트: `allowedTypePackages = setOf("io.bluetape4k.kafka.codec")`
- `KafkaCodecAllowlistTest` (kafka4) 업데이트: 첫 번째 테스트 기대값 수정 (deny-all 반영)
- kafka3: 112 passing, kafka4: 117 passing

## 향후 지침

- `JacksonKafkaCodec` 사용 시 `allowedTypePackages`를 명시적으로 지정하는 것을 권장
- Spring Bean 등록 시 `KafkaCodecs.Jackson` 싱글톤 대신 직접 생성하여 패키지 목록을 지정하라
- 신뢰할 수 있는 내부 환경에서만 `ALLOW_ALL_TYPES_UNSAFE`를 사용하라
