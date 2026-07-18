# Issue #758 Kafka ByteBuffer Codec 설계

- Issue: [#758 Add allocation-aware ByteBuffer helpers for Kafka codecs](https://github.com/bluetape4k/bluetape4k-projects/issues/758)
- Milestone: `1.12.0`
- Branch: `perf/issue-758-kafka-bytebuffer-codecs`
- Baseline authority: `origin/develop@784e74c182ae97a6f2b89991bdf068832ecdbb4d`
- 분류: Type B Fast Track
- 성능 판단 기준: codec-only `gc.alloc.rate.norm`(B/op); 처리량 개선은 주장하지 않음
- 전달 중단점: exact-head PR과 CI/리뷰가 merge-ready인 상태. 병합은 별도 승인을 받음

## 1. 목표

표준 Kafka `Serializer`와 `Deserializer`의 `ByteArray` 경계는 그대로
유지한다. 그 위에 caller-owned `ByteBuffer`를 명시적으로 사용하는
`BufferAwareKafkaCodec<T>` 계약을 추가하고, `BinaryKafkaCodec`이 기존
`BinarySerializer.serializeTo`/`deserializeFrom` API로 이를 구현한다.

이 변경의 목표는 Kafka 경계를 zero-copy라고 표현하는 것이 아니다.
호출자가 이미 재사용 버퍼를 소유한 경우 codec 내부의 피할 수 있는
중간 `ByteArray`를 줄일 수 있는 경로를 제공하고, 실제 allocation 감소는
측정된 backend와 방향에 대해서만 문서화하는 것이다.

## 2. 권한과 범위

live issue 본문과 현재 `develop`의 `BinarySerializer` 버퍼 계약이 이 설계의
권한이다. 이 설계는 release, tag, publication, repository setting, credential
변경을 승인하지 않는다.

### 포함

- 새 public `BufferAwareKafkaCodec<T>` 계약
- `BinaryKafkaCodec`의 buffer-aware 구현
- 표준 Kafka 직렬화와 같은 value-type header 동작
- 기존 poison-pill WARN logging과 `CancellationException`/`Error` 전파 정책
- heap/direct/sliced/read-only 입력과 bounded 출력의 position/limit 계약 테스트
- `ByteArrayKafkaCodec`의 raw binary passthrough 회귀 테스트
- 기존 `benchmark/serializer-benchmark` 모듈의 codec-only allocation benchmark
- `infra/kafka4/README.md`와 `README.ko.md`의 동등한 사용법 및 제약 설명
- `benchmark/serializer-benchmark/README.md`와 `README.ko.md`의 matrix 및 scope 갱신
- 새 public API의 English KDoc와 비자명한 제약에 대한 English rationale comment

### 제외

- 표준 Kafka `Serializer`/`Deserializer` signature 또는 `ByteArray` 동작 변경
- Kafka broker send/receive, network, batching, compression을 포함한 성능 비교
- zero-copy Kafka 또는 보편적인 throughput 개선 주장
- 모든 `KafkaCodec`에 적용되는 generic `ByteBuffer` extension
- `org.apache.kafka.common.utils.Bytes` 전용 overload
- `ByteArrayKafkaCodec`를 buffer-aware contract로 승격하는 변경
- wire format, serializer registration, type allowlist, security 기본값 변경
- 새 외부 dependency, 새 Gradle module, Testcontainers 기반 검증

`Bytes`는 내부적으로 `ByteArray`를 보유하므로 현재 표준 Kafka 경계보다
낮은 allocation 경로를 추가로 만들지 않는다. `Bytes.get()`을 직접 표준
codec에 넘길 수 있어 전용 overload의 이점도 작다. 측정 가능한 이점이나
반복되는 caller ergonomics 문제가 확인되기 전에는 추가하지 않는다.

`ByteArrayKafkaCodec`의 ByteBuffer decode는 결과 `ByteArray` 생성을 피할 수
없고, encode는 단순 복사 편의만 제공한다. 이를 같은 contract로 노출하면
allocation-aware라는 의미를 흐리므로 기존 raw passthrough API와 회귀 테스트만
유지한다.

## 3. 선택한 public contract

`KafkaCodec.kt`에 다음 형태의 opt-in interface를 추가한다.

```kotlin
interface BufferAwareKafkaCodec<T>: KafkaCodec<T> {

    fun serializeTo(
        topic: String?,
        data: T & Any,
        target: ByteBuffer,
    ): Int = serializeTo(topic, null, data, target)

    fun serializeTo(
        topic: String?,
        headers: Headers?,
        data: T & Any,
        target: ByteBuffer,
    ): Int

    fun deserializeFrom(
        topic: String?,
        source: ByteBuffer,
    ): T? = deserializeFrom(topic, null, source)

    fun deserializeFrom(
        topic: String?,
        headers: Headers?,
        source: ByteBuffer,
    ): T?
}
```

`serialize`/`deserialize` 이름을 overload하지 않고 `serializeTo`와
`deserializeFrom`을 사용한다. 이는 Kafka의 nullable `ByteArray` API와의
호출 모호성을 피하고 `BinarySerializer`의 기존 용어와 일치한다.

buffer output은 실제 payload가 존재할 때만 의미가 있으므로 `data`는
`T & Any`로 non-null을 명시한다. Kafka tombstone/null value는 기존
`KafkaCodec.serialize(..., data = null)`을 사용하며 결과 `null`과 header
미변경이라는 기존 의미를 유지한다. 새 API에서 `0`으로 null과 빈 payload를
혼동시키지 않는다.

새 interface는 opt-in 계약이며 기존 `KafkaCodec` 구현체에는 member를
추가하지 않는다. 따라서 기존 third-party `KafkaCodec` 구현체는 새 method를
구현할 필요가 없다. `BinaryKafkaCodec`이 모든 새 method를 concrete하게
구현하므로 기존 binary codec subclass도 source/binary 사용 형태를 유지한다.

## 4. Buffer 계약

### 4.1 Output

`serializeTo(topic, headers, data, target)`은 다음을 보장한다.

- target의 최초 `position`부터 현재 `limit`까지만 쓴다.
- 성공 시 실제 기록한 byte 수를 반환하고 `position`만 그 수만큼 전진시킨다.
- `limit`, capacity, byte order, caller ownership은 유지한다.
- target을 교체하거나 확장하지 않는다.
- read-only target은 serializer 작업 전에 `ReadOnlyBufferException`으로 실패한다.
- 공간 부족은 원래 `BufferOverflowException`으로 실패한다.
- 실패 시 최초 position을 복원하고 같은 예외 또는 `Error`를 재전파한다.
- 실패 전에 덮어쓴 byte는 rollback하지 않으며 내용은 unspecified이다.
- 성공 시 JVM의 일반적인 mark invalidation 규칙을 따른다.
- 호출 동안 버퍼는 caller가 thread-confined 상태로 유지해야 한다.

이 계약은 `BinarySerializer.serializeTo`를 그대로 상속한다. 구현에서
추가 staging buffer나 `ByteArray` 변환을 삽입하지 않는다. 다만 실제
`BinarySerializer` 구현이 interface default fallback을 사용하는 경우에는
내부 `ByteArray` allocation이 남을 수 있으며, KDoc와 문서에서 이를 명시한다.

### 4.2 Input

`deserializeFrom(topic, headers, source)`는 다음을 보장한다.

- 최초 `[source.position(), source.limit())` 범위만 읽는다.
- 성공과 실패 모두 caller의 position, limit, mark, byte order를 보존한다.
- heap, direct, sliced, read-only source를 지원한다.
- 빈 remaining 범위는 underlying `BinarySerializer` 정책에 따라 `null`을 반환한다.
- caller가 신뢰할 수 없는 입력의 최대 크기를 호출 전에 제한해야 한다.
- 호출 동안 버퍼를 동시에 변경하거나 다른 thread와 공유하지 않는다.

구현은 `BinarySerializer.deserializeFrom`에 직접 위임한다. source를
`ByteArray`로 먼저 복사하는 Kafka-layer fallback을 새로 만들지 않는다.
underlying serializer의 compatibility default가 복사하는 경우에는 그 경로를
optimized라고 문서화하지 않는다.

## 5. Header, 예외, logging 계약

### 5.1 Header

- `writeValueTypeHeader == true`이면 표준 `serialize`와 같은
  `bluetape4k.kafka.codec.value.type` header를 serialization 전에 추가한다.
- `false`이면 새 header를 추가하지 않는다.
- 기존 구현과 마찬가지로 serialization이 이후 실패해도 이미 추가된 header는
  제거하지 않는다. target position rollback과 header side effect는 별도 계약이다.
- buffer deserialization은 기존 binary codec처럼 type header를 역직렬화 입력으로
  사용하지 않으며 allowlist나 class loading 범위를 넓히지 않는다.

### 5.2 Poison-pill과 fatal signal

buffer deserialization은 기존 `AbstractKafkaCodec.deserialize(ByteArray?)`와
동일한 정책을 사용한다.

- `CancellationException`은 동일 instance를 재전파한다.
- `Error`는 catch하지 않고 동일 instance를 전파한다.
- 그 밖의 `Exception`은 WARN 로그를 남기고 `null`을 반환한다.
- 로그에는 topic, header key 목록, 최초 remaining byte 수만 포함한다.
- payload 내용, 전체 header value, serializer 객체는 기록하지 않는다.
- hot-path 성공 로그는 추가하지 않는다.

`AbstractKafkaCodec`의 기존 `KLogging`을 재사용해 ByteArray와 ByteBuffer 경로의
경고 메시지 및 예외 분류가 drift하지 않게 한다. 구현은 명확한 공통 helper나
동등한 allocation-free control flow를 사용하되, deserialization hot path에
불필요한 capturing lambda allocation을 추가하지 않는다. 공개 API에는 English
KDoc를 작성하고, header failure side effect와 fatal-signal 보존처럼 코드만으로
드러나지 않는 이유에만 English comment를 둔다.

serialization 예외는 기존 표준 경로처럼 로그로 흡수하지 않고 그대로
전파한다. 새 성공 로그나 serialization 실패 중복 로그를 추가하지 않는다.

## 6. 구현 구조

### 6.1 `KafkaCodec.kt`

- `BufferAwareKafkaCodec<T>`와 English KDoc를 추가한다.
- `AbstractKafkaCodec`의 ByteArray/ByteBuffer poison-pill 처리가 같은
  KLogging 정책을 사용하도록 최소한의 reusable boundary를 둔다.
- 기존 `KafkaCodec` default method, null policy, close/configure 동작은 변경하지 않는다.

### 6.2 `BinaryKafkaCodecs.kt`

- `BinaryKafkaCodec`이 `BufferAwareKafkaCodec<Any?>`를 구현한다.
- output은 `serializer.serializeTo(data, target)`에 직접 위임한다.
- input은 `serializer.deserializeFrom<Any>(source)`에 직접 위임한다.
- `KryoKafkaCodec` 등 concrete codec과 wire format은 변경하지 않는다.
- serializer별 optimized/fallback 차이를 숨기지 않도록 class KDoc에 일반 계약과
  제한을 설명한다.

## 7. 테스트 설계

제품 코드 전에 실패하는 contract test를 작성하고 다음을 고정한다.

### 7.1 Public dispatch와 header

- receiver를 `BufferAwareKafkaCodec<Any?>`로 선언해 interface dispatch를 검증한다.
- header 포함 overload는 runtime type header를 표준 `serialize`와 같은 값으로 쓴다.
- header 없는 overload와 `writeValueTypeHeader = false`는 불필요한 header를 만들지 않는다.
- serialization failure 뒤 target position은 복원되지만 이미 쓴 type header는 남는다.
- 기존 header를 제거하거나 다른 header/value를 변경하지 않는다.

### 7.2 Output buffer

- heap 및 direct target
- non-zero initial position과 restricted limit
- 반환 byte 수와 최종 position
- limit, capacity, byte order 보존
- read-only target의 원래 `ReadOnlyBufferException`
- too-small target의 원래 `BufferOverflowException`
- ordinary exception과 `Error` identity 보존
- 실패 position rollback 및 다음 호출에서 codec 재사용 가능

### 7.3 Input buffer

- heap, direct, sliced, read-only source
- non-zero position과 restricted limit의 remaining 범위만 decode
- 성공/실패 시 position, limit, byte order, mark 보존
- 빈 remaining 범위의 `null`
- ordinary `Exception`은 WARN 후 `null`
- `CancellationException`과 `Error` identity 재전파
- 로그에 topic/header keys/data size가 있고 payload/header value가 없는지 검증

### 7.4 기존 동작 회귀

- `ByteArrayKafkaCodec`은 전달받은 raw array를 추가 변환 없이 그대로 반환한다.
- 표준 `KafkaCodec.serialize`/`deserialize`의 null 및 header 동작은 유지한다.
- 기존 `AbstractKafkaCodecPoisonPillTest`는 그대로 통과한다.
- broker나 Testcontainers 없이 codec 단위 테스트로 실행한다.

mock serializer는 invocation count와 전달받은 동일 `ByteBuffer` instance를
기록해 Kafka layer가 `ByteArray` API로 우회하지 않았음을 증명한다. 실제
`KryoKafkaCodec` round-trip test는 public integration proof로 추가한다.

## 8. Benchmark와 allocation 판단

기존 `benchmark/serializer-benchmark`에 `:bluetape4k-kafka4`의 internal
benchmark dependency를 추가하고 Kafka codec 전용 benchmark class를 둔다.
새 module이나 broker dependency는 만들지 않는다.

### 8.1 비교 cell

대표 codec은 serialize/deserialize 양쪽에 실제 buffer override가 있는
`KryoKafkaCodec`이다.

- serialization: 표준 `codec.serialize(...): ByteArray` 대 재사용 heap target의
  `codec.serializeTo(...)`
- deserialization: 사전 생성한 `ByteArray` 입력 대 동일 bytes를 담은 bounded
  caller-owned `ByteBuffer`의 `codec.deserializeFrom(...)`
- header 비용을 비교하려는 benchmark가 아니므로 양쪽 모두 headers가 `null`인
  overload를 사용한다. 이를 통해 timed method에서 `RecordHeader` 생성이나 header
  reset이 allocation 수치에 섞이지 않게 한다.

payload, codec, serialized input, output buffer는 timed method 밖에서 준비한다.
invocation setup은 position/limit과 필요한 header 상태만 allocation 없이
복원한다. timed method 안에서 buffer 생성, resize, 복사 준비, correctness assertion을
하지 않는다. JMH `Blackhole` 또는 반환값으로 결과를 관측한다.

### 8.2 실행과 증거

- 먼저 `./gradlew :serializer-benchmark:tasks --all`로 실제 생성 task를 확인한다.
- wiring smoke는 1 fork, 1 warmup, 1 measurement, 1 thread로 실행한다.
- 정식 evidence는 동일 commit/environment에서 순차적으로 두 번 실행한다.
- JMH GC profiler `-prof gc`와 JSON 결과를 사용한다.
- primary metric은 `gc.alloc.rate.norm`(B/op)이다.
- throughput은 진단 정보일 뿐 개선 주장의 근거로 사용하지 않는다.
- raw JSON과 environment metadata를
  `docs/benchmarks/raw/issue-758/run-<UTC>/`에 보존한다.
- 결과 표와 literal command, 환경, 한계는
  `docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md`에 기록한다.

같은 direction과 payload의 buffer path가 두 fresh run에서 모두 ByteArray
baseline보다 최소 5% 낮은 B/op를 보일 때만 해당 cell에 대해 allocation을
줄였다고 표현한다. 방향 불일치, 5% 미만, profiler 누락은 `inconclusive`로
기록한다. 숫자가 낮더라도 underlying serializer가 compatibility fallback을
사용하는 cell에는 allocation 감소 주장을 하지 않는다.

broker 비용은 측정하지 않으며 이 결과로 producer/consumer throughput 또는
end-to-end latency를 추론하지 않는다. chart는 필수가 아니며 raw JSON과 표를
수치 source of truth로 사용한다.

## 9. 문서 계약

`infra/kafka4/README.md`와 `README.ko.md`는 의미상 동등하게 다음을 설명한다.

- 표준 Kafka API의 unavoidable `ByteArray` 경계
- `BufferAwareKafkaCodec`이 opt-in caller-owned buffer API라는 점
- Kotlin과 Java의 output/input 예제
- position/limit, read-only, overflow, failure 계약
- serializer별 optimized path와 compatibility fallback의 차이
- caller buffer lifecycle과 thread confinement
- poison-pill WARN logging, cancellation/fatal signal 정책
- benchmark report 링크와 측정 환경 한계
- zero-copy 및 throughput 개선을 주장하지 않는다는 명시

수치의 source of truth는 benchmark report 한 곳으로 유지한다. locale README에는
결론을 요약하고 같은 report를 링크해 숫자 drift를 방지한다.

`benchmark/serializer-benchmark/README.md`와 `README.ko.md`도 함께 갱신한다.
현재 serializer-only 40-cell matrix와 “#758은 범위 밖”이라는 문구를 그대로
두지 않고, 새 Kafka codec cell 수와 literal task/JMH 명령, codec-only 경계,
issue #758 report 링크를 양쪽 문서에 동일하게 반영한다.

## 10. 호환성과 보안

- 변경은 새 interface와 `BinaryKafkaCodec`의 additive 구현이다.
- 기존 `KafkaCodec`, Kafka `Serializer`/`Deserializer`, `ByteArray` 호출자는
  migration 없이 동작한다.
- wire bytes와 serializer configuration/registration은 underlying
  `BinarySerializer`가 계속 결정한다.
- type header key/value와 `allowedTypePackages` 기본값은 변경하지 않는다.
- Fory/Kryo trusted-input 및 class registration 관련 기존 보안 경고를 유지한다.
- buffer API가 input size 제한이나 broker trust boundary를 대신하지 않는다.
- logging은 payload나 header value를 노출하지 않는다.

## 11. 예상 변경 범위와 승격 조건

예상 production 변경은 `KafkaCodec.kt`와 `BinaryKafkaCodecs.kt` 두 파일이다.
나머지는 focused contract test, 기존 benchmark module, EN/KO README, benchmark
evidence, 설계/계획 문서다.

다음 중 하나가 필요해지면 구현 전에 Type A Full Feature로 재분류하고 계획을
다시 검토한다.

- `KafkaCodec` 자체의 기존 public method 변경
- 세 개 이상의 codec family에 서로 다른 public buffer 계약 추가
- 새 module 또는 외부 dependency
- wire/header/security 정책 변경
- 다섯 개 이상의 의미 있게 결합된 production 파일 변경
- broker integration benchmark나 Testcontainers 도입

## 12. 검증 순서와 완료 조건

1. RED: 새 interface dispatch, header, buffer, poison-pill contract test가 구현 전 실패
2. GREEN: `:bluetape4k-kafka4:test` targeted test 통과
3. regression: 전체 Kafka codec 단위 테스트 통과
4. static: `detekt`의 실제 지원 task를 확인한 뒤 repo-level relevant gate 통과
5. benchmark wiring: project/task discovery, compile, smoke 통과
6. evidence: 동일 commit의 fresh allocation run 두 번과 raw JSON/report 보존
7. docs: EN/KO 의미 동등성, Kotlin/Java sample, report link 검증
8. repository: `git diff --check`, clean generated state, exact-head 확인
9. review: written artifact와 code의 material 7-Tier review에서 P0/P1 0건
10. delivery: PR head/local head/remote head 일치, CI와 review/thread 상태 통과

완료 시에도 병합은 자동으로 수행하지 않는다. exact PR/head, CI, review evidence를
보고한 뒤 새 승인을 받고, history를 선형으로 유지하는 것이 적합하면 rebase merge를
사용한다.
