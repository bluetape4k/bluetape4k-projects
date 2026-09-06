# Issue #1641 — Fory 1.7 Kotlin JSON 및 증분 decoder 평가

## 결정

**2.1.0에는 도입하지 않는다.**

Fory JSON 1.7.1의 Kotlin 모델 지원과 증분 NDJSON/array decoder는 동작했고,
짧은 직렬화 측정에서도 검토를 계속할 만한 결과가 나왔다. 그러나 현재
`JsonSerializer`의 `Class<T>` 계약은 `jsonTypeRef<T>()`가 보존하는
Kotlin nullability, value class, generic 인자를 표현하지 못한다. 증분 decoder도
호출자가 `ByteBuffer` 공급, 단일 stream lifecycle, terminal failure를 직접
관리해야 한다.

따라서 이번 이슈에서는 production dependency, module, public API를 추가하지
않는다. `JsonSerializer`에 구조적 타입 토큰을 도입하거나 실제 NDJSON
consumer가 생기면 별도 Type A 설계로 다시 평가한다.

## 범위

- Apache Fory `fory-json-kotlin:1.7.1`
- Kotlin `2.4.10`, JDK `25`
- Kotlin data class의 default/nullability/generic/value class/sealed hierarchy
- UTF-8 chunk를 3바이트 또는 5바이트로 분할한 NDJSON/array decoder
- final newline이 없는 NDJSON, CRLF, malformed/truncated input, `maxValueBytes`
- `Long` 문자열 정책과 `ByteArray` Base64 표현
- Jackson 2.22.2 및 Fastjson2 2.0.65와 동일한 flat model의 JSON text 직렬화 비교

현재 `FastjsonSerializer.serialize`는 JSONB를 사용하므로 아래 JSON text
측정은 `JsonSerializer.serialize` backend 순위가 아니다. allocation,
deserialization, concurrency, 장시간 warmup도 측정하지 않았다.

## 재현 방법

독립 PoC는 main Gradle project에 등록되지 않는다.

```bash
./gradlew -p docs/evidence/issue-1641/poc run --no-daemon
```

JDK 25에서는 Fory가 사용하는 `java.lang.invoke` 경로를 위해 PoC의
`JavaExec`에 다음 옵션을 한정했다.

```text
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED
```

## 기능 검증 결과

| 항목 | 결과 | 관찰 |
| --- | --- | --- |
| Kotlin data class | PASS | 생성자 기반 왕복 |
| default parameter | PASS | JSON에서 `name`을 제거하면 `anonymous` 사용 |
| nullability | PASS | nullable `note=null` 왕복 |
| value class | PASS | `AccountId(Long)` 왕복 |
| generic root | PASS/제약 | `jsonTypeRef<List<Account>>()`는 복원, raw `List::class.java`는 `JsonObject` 반환 |
| sealed hierarchy | PASS/명시 필요 | `@JsonSubTypes(property = "type")`로 폐쇄 subtype table 지정 |
| NDJSON decoder | PASS | 3바이트 chunk, CRLF, final newline 없음, 2건 복원 |
| array decoder | PASS | 5바이트 chunk, 2건 복원 |
| malformed/truncated | PASS | `finish()` 실패 후 decoder가 terminal 상태 유지 |
| `maxValueBytes` | PASS | 16바이트 제한에서 `JsonStreamValueLimitException` |
| `Long` | PASS | `writeLongAsString(true)`에서 `Long.MAX_VALUE`를 문자열로 출력 |
| `ByteArray` | PASS | `AAECf/8=` Base64 문자열로 출력 |

공식 문서는 Kotlin module이 binary protocol을 바꾸지 않는 별도 JSON module이며,
`jsonTypeRef<T>()`를 재사용해야 한다고 설명한다.

- <https://fory.apache.org/docs/next/json/kotlin/>
- <https://fory.apache.org/blog/fory_1_7_1_release/>
- Maven coordinate: `org.apache.fory:fory-json-kotlin:1.7.1`

## 표현 크기와 짧은 측정

동일한 `FlatAccount(name, Long.MAX_VALUE, ByteArray(5))`를 JSON text로
직렬화했다.

| backend | UTF-8 크기 |
| --- | ---: |
| Fory JSON | 70 bytes |
| Jackson | 68 bytes |
| Fastjson2 | 72 bytes |

각 process에서 2,000회 warmup 뒤 20,000회 직렬화했다.

| 실행 | Fory | Jackson | Fastjson2 |
| --- | ---: | ---: | ---: |
| 1 | 281 ns/op | 725 ns/op | 332 ns/op |
| 2 | 199 ns/op | 596 ns/op | 298 ns/op |
| 3 | 286 ns/op | 663 ns/op | 351 ns/op |
| 추적 fixture 재실행 | 237 ns/op | 1,418 ns/op | 332 ns/op |

이 값은 로컬 GraalVM JDK 25에서 실행한 짧은 `measureNanoTime` 결과다.
JMH benchmark, allocation profile, parser 비용, 실제 payload 분포를 대신하지
않으며 production 성능 순위를 주장하지 않는다. Jackson 수치의 실행 간 변동도
이 측정이 채택 근거가 될 수 없음을 보여준다.

## 도입 선택지

| 선택지 | 판정 | 이유 |
| --- | --- | --- |
| 기존 `io/json`에 `ForyJsonSerializer` 추가 | 거부 | raw `Class<T>`가 generic/nullability/value-class 계약을 잃고 기존 interface 의미와 불일치 |
| 새 public Fory JSON module 및 구조적 타입 API | 보류 | module/dependency/BOM/API/호환성 정책을 함께 결정해야 하는 Type A 범위 |
| 증분 decoder를 공용 NDJSON API로 즉시 감싸기 | 보류 | stream ownership, buffer 공급, cancellation/backpressure, terminal failure 계약과 실제 consumer가 필요 |
| 독립 PoC와 평가 기록만 유지 | 선택 | production surface를 바꾸지 않고 기능·제약·재검토 조건을 재현 가능하게 보존 |

## 실패 모드와 후속 게이트

1. raw `Class<T>`로 parameterized root를 읽으면 typed model 대신 동적
   `JsonObject`가 나온다.
2. `@JsonSubTypes`가 없는 interface/abstract sealed property는 자동 schema로
   처리되지 않는다.
3. malformed input이나 limit 초과 뒤 decoder를 재사용하면
   `IllegalStateException`으로 실패한다.
4. 1.7.1의 기본 `ByteArray` Base64는 1.7.0 numeric-array 출력과 읽기 호환되지
   않으므로 wire policy를 암묵적으로 채택할 수 없다.
5. 짧은 직렬화 수치만으로 dependency와 public API 비용을 정당화할 수 없다.

다음 조건을 모두 충족할 때 별도 도입 이슈를 만든다.

- `jsonTypeRef<T>()`와 동등한 구조적 타입 계약을 노출할 API 설계
- JSON text와 JSONB를 구분한 backend compatibility suite
- 실제 NDJSON consumer의 ownership, cancellation, backpressure 요구사항
- JMH 기반 serialize/deserialize/allocation 비교
- 중앙 catalog alias, BOM, module 등록, README locale, CI/Nightly 범위

## DoD

- [x] Kotlin 모델의 default/nullability/generic/value class/sealed 계약을 검증했다.
- [x] NDJSON/array decoder의 chunk, CRLF, final record, malformed, limit,
  terminal lifecycle을 검증했다.
- [x] `Long`과 `ByteArray` 표현 정책을 확인했다.
- [x] 재현 가능한 독립 PoC와 제한된 측정 결과를 남겼다.
- [x] 현재 `JsonSerializer`와의 계약 차이를 기록했다.
- [x] 2.1.0 production 도입을 보류하고 재검토 게이트를 명시했다.
- [ ] public module/API 도입 — 이번 이슈 범위에서 제외한다.
