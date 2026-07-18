# Issue #757 Protobuf Buffer Core Design

- Issue: [#757 Add ByteBuffer-oriented Protobuf serializer and codec APIs](https://github.com/bluetape4k/bluetape4k-projects/issues/757)
- Parent delivery tracker: [#898 Epic: 1.12.0 delivery tracking](https://github.com/bluetape4k/bluetape4k-projects/issues/898)
- Related Redis umbrella: [#756 Reduce Redis codec allocation with ByteBuffer and ByteBuf paths](https://github.com/bluetape4k/bluetape4k-projects/issues/756)
- Milestone: `1.12.0`
- Branch: `feat/issue-757-protobuf-buffer-core`
- Baseline authority: `origin/develop@5187d1d2c4877dbf7002db1c4ae7be2295efa134`
- Primary decision metric: normalized allocation, `gc.alloc.rate.norm` in B/op
- Delivery stop: exact-head pull request is merge-ready; merge requires fresh approval

## 1. 문제

`BinarySerializer`에는 caller-owned `ByteBuffer` 계약이 병합됐지만
`ProtobufSerializer`는 여전히 Protobuf `Any`를 `ByteArray`로 만든 뒤 기본
호환 경로로 복사한다. `RedissonProtobufCodec`의 encode 경로는 #649에서
`ByteBuf` 직접 쓰기로 개선됐지만 decode 경로는 `buf.getBytes(copy = true)`로
전체 payload를 heap 배열에 복사한 뒤 `Any.parseFrom`을 호출한다.

기존 `benchmark/protobuf-codec-benchmark`도 신뢰할 수 있는 회귀 게이트가
아니다. strict `ProtobufSerializer()`로 fallback payload를 측정해 해당 benchmark
method가 실행 중 실패하지만 Gradle task는 성공하고 JSON 결과에서 그 method만
누락된다. 처리량 숫자만 보고서는 누락을 발견할 수 없으며 allocation/GC 증거도
없다.

이 작업의 목표는 zero-copy를 약속하는 것이 아니다. Protobuf 내부의 `Any.pack`
및 `ByteString` 구성은 그대로 남을 수 있다. 제거 대상은 public buffer API와
Redisson strict decode 앞에 존재하는 명시적 payload-sized `ByteArray` handoff다.

## 2. 권한과 범위

live issue #757, 현재 `develop`, #754 buffer 계약, #1039 allocation evidence 규칙,
그리고 #649의 기존 Redisson encode 결정이 권위다. 이 설계는 release, tag,
publish, repository settings, credential 작업을 승인하지 않는다.

### 포함

- `MessageSupport`의 Protobuf `Any`용 caller-owned `ByteBuffer` pack/unpack API.
- `ProtobufSerializer`의 Protobuf-message `serializeTo` 및 성공 decode
  `deserializeFrom` 저복사 override.
- `RedissonProtobufCodec` strict Protobuf decode의 `ByteBuf.nioBuffer` view 경로.
- strict 및 trusted-internal benchmark profile 분리.
- 기대한 benchmark method 집합, GC metric, 두 fresh run을 fail-closed로 검증하는
  기존 benchmark module 내부 도구와 테스트.
- heap/direct/sliced/read-only buffer, non-zero position/limit, overflow,
  allowlist, fallback, wire format, `ByteBuf` ownership 테스트.
- public KDoc, `io/protobuf` 및 benchmark module의 English/Korean README parity,
  allocation report, `CHANGELOG.md`.

### 제외

- Lettuce Protobuf codec. #757의 두 번째 PR에서 다룬다.
- compressor 및 Redisson compression wrapper. #755/#756 범위다.
- Kafka codec. #758 범위다.
- `BinarySerializer` 공통 계약 변경이나 새 serializer SPI.
- 새로운 Gradle module, dependency, profiler dependency, external service,
  Testcontainers, chart asset.
- zero-copy 또는 throughput 개선 보장.
- trusted fallback decode에서 현재 필요한 payload copy 제거.

## 3. 검토한 접근

### 3.1 선택: strict-first core + Redisson + fail-closed benchmark

공통 Protobuf buffer helper, `ProtobufSerializer`, Redisson strict decode, allocation
gate를 한 PR에 둔다. 실제 hot path와 public API가 같은 wire/security contract를
공유하며, 구현과 allocation claim을 같은 exact head에서 검증할 수 있다.

### 3.2 기각: core API만 먼저 제공

PR은 작아지지만 `ByteBuffer` API가 실제 Redis decode copy를 줄이는지 증명하지
못하고, benchmark 누락 결함도 그대로 남는다. API가 증거보다 먼저 고정되는
위험이 있다.

### 3.3 기각: Lettuce까지 한 PR에 포함

Lettuce의 `ToByteBufEncoder`와 Netty ownership까지 함께 다루면 두 Redis client의
resource/error semantics가 한 review unit에 섞인다. #898의 large-workstream split
원칙과 broad-backend narrow-slice 규칙에 따라 Lettuce는 후속 PR로 분리한다.

## 4. Public API 계약

### 4.1 MessageSupport output

```kotlin
fun <T: Message> packMessageTo(message: T, target: ByteBuffer): Int
```

- `ProtoAny.pack(message)`의 wire bytes를 target의 초기 position부터 쓴다.
- 성공하면 정확히 반환 byte 수만큼 position을 전진시키고 limit, capacity,
  byte order, ownership은 보존한다.
- 성공한 position 이동에는 기존 `BinarySerializer` KDoc와 동일한 normal JDK mark
  규칙이 적용된다.
- read-only target은 Protobuf 작업 전에 `ReadOnlyBufferException`으로 실패한다.
- remaining capacity가 `Any.serializedSize`보다 작으면 쓰기 전에
  `BufferOverflowException`으로 실패한다.
- 일반 실패 시 caller position을 보존한다. 이미 덮인 bytes는 rollback을
  보장하지 않으므로 시도된 range는 undefined다. 재시도 전 caller가 target을
  clear/reinitialize하거나 폐기해야 한다.
- target을 교체하거나 확장하거나 library-owned direct buffer를 만들지 않는다.
- target은 caller-owned이며 호출 동안 thread-confined여야 한다. caller가 position,
  limit, contents를 동시에 변경하면 동작을 보장하지 않는다.

기존 `packMessage(message): ByteArray`는 변경하지 않는다. 새 API와 기존 API는
동일한 logical Protobuf `Any` wire bytes를 생성해야 한다.

### 4.2 MessageSupport input

```kotlin
inline fun <reified T: Message> unpackMessage(source: ByteBuffer): T?
```

- source의 초기 `[position, limit)` 범위만 읽는다.
- duplicate/view를 사용하여 성공과 실패 모두 caller position, limit, mark,
  byte order를 보존한다.
- heap, direct, sliced, read-only input을 지원한다.
- 기존 `ByteArray` overload와 같이 실제 `Any` 타입이 `T`가 아니면 `null`, parse
  실패는 호출자에게 전파한다.
- 호출 동안 source를 변경하거나 다른 thread와 공유하지 않아야 한다. library는
  만든 view를 반환하거나 호출 이후 보관하지 않는다.

### 4.3 ProtobufSerializer output

`ProtobufSerializer.serializeTo`는 다음과 같이 dispatch한다.

- `null`: 기존 `BinarySerializer` null 정책을 유지한다.
- `ProtoMessage`: pre-sized caller target에 `ProtoAny`를 직접 쓴다.
- non-Protobuf strict profile: 기존 `BinarySerializationException` 정책과 message
  chain을 유지한다.
- trusted fallback profile: 기존 fallback serialization semantics를 유지한다.
  이번 PR은 fallback 경로를 allocation 최적화 대상으로 주장하지 않는다.

read-only, overflow, position commit/rollback은 #754의 `BinarySerializer` 계약과
동일하다. 기존 `serialize(graph): ByteArray`와 wire format은 변경하지 않는다.

관찰 가능한 예외/empty 계약은 다음과 같다.

| Condition | Result |
|---|---|
| read-only target | backend 호출 전 raw `ReadOnlyBufferException` |
| insufficient remaining | backend 호출 전 raw `BufferOverflowException` |
| empty source | 기존 기본 `deserializeFrom`과 동일하게 `null` |
| malformed/class-not-found/unpack 등 일반 encode/decode 실패 | 기존 `serialize`/`deserialize`와 동일한 `BinarySerializationException` message/cause chain |
| allowlist/non-Message terminal security failure | fallback 없이 `BinarySerializationException`, cause는 `SecurityException` |
| `Error` 계열 | 기존 buffer failure classification대로 raw propagation |

### 4.4 ProtobufSerializer input

`ProtobufSerializer.deserializeFrom`은 caller source의 duplicate로 `ProtoAny`를
파싱하고 allowlist 검증 및 class resolution에 성공한 경우에만 저복사 결과를
반환한다.

optimized probe가 실패하면 기존 `deserialize(ByteArray)` compatibility path로
재진입할 수 있는 범위는 trusted profile에서 기존 fallback이 허용한 malformed
payload, class-not-found, allowlisted `Message` unpack failure compatibility case뿐이다.
strict profile은 기존 예외 계약으로 종료한다. allowlist 위반, resolved class가
`Message`/`ProtoMessage`가 아닌 경우, linkage/security failure는 terminal failure이며
절대 fallback하지 않는다.
이 의도적인 허용 경로 copy는 다음을 보존한다.

- strict non-Protobuf payload의 기존 exception type/message/cause chain;
- allowlist 위반이 trusted fallback으로 우회되지 않는 보안 경계;
- trusted-internal historical fallback payload decode;
- class-loading 및 fallback logging semantics.

class resolution은 초기화 없이 수행하고, cache에 넣거나 unchecked cast하기 전에
resolved class가 `Message`/`ProtoMessage`를 구현하는지 검증한다. 위반은
`SecurityException`으로 종료한다. cache는 serializer/codec instance가 소유하며
실제 effective `ClassLoader` identity와 class name의 pair로 key를 구성한다. 같은
class name을 서로 다른 loader가 제공할 때 cache entry를 공유하지 않는다.

dispatch 결과는 다음 결정표로 고정한다. terminal security failure는 strict/trusted
모두 logging 후 fallback하는 catch 범위에 들어가지 않는다.

| Input/result | Strict | Trusted |
|---|---|---|
| valid allowlisted `Any` + `Message` class | unpack result | unpack result |
| empty source range | `null` | `null` |
| malformed/truncated `Any` | 기존 wrapped exception | fallback + 기존 debug logging |
| allowlist/prefix violation | terminal `BinarySerializationException(SecurityException cause)` | terminal `BinarySerializationException(SecurityException cause)` |
| class not found | 기존 wrapped exception | fallback + 기존 debug logging |
| resolved non-`Message` class | terminal `BinarySerializationException(SecurityException cause)` | terminal `BinarySerializationException(SecurityException cause)` |
| linkage/security failure | terminal existing security/error contract | terminal existing security/error contract |
| allowlisted `Message` unpack failure | 기존 wrapped exception | fallback + 기존 debug logging |

`MessageSupport.unpackMessage<T>`에서 valid `Any`의 실제 type이 요청한 `T`와 다르면
별도 fallback 없이 기존 overload와 동일하게 `null`을 반환한다.

위 표는 public `ProtobufSerializer` 결과다. `RedissonProtobufCodec`은 allowlist와
resolved non-`Message` 위반을 raw `SecurityException`으로 전파하며 fallback하지
않는다. `Error` 계열은 두 API 모두 기존 classification대로 terminal이다.

따라서 positive allocation claim은 valid allowlisted Protobuf success path에만
허용한다. malformed, blocked, fallback path는 기능/보안 control일 뿐 최적화
cell이 아니다.

## 5. Redisson decode 설계

strict Protobuf decode는 `nioBufferCount() == 1`인 contiguous input에서 다음 bounded
view를 사용한다.

```text
ByteBuf(readerIndex, readableBytes)
  -> nioBuffer(index, length)
  -> Protobuf Any parse
  -> allowlist validation
  -> class resolution without initialization
  -> unpack
```

- `ByteBuf` reader/writer index를 움직이지 않는다.
- decoder는 input `ByteBuf`를 retain/release하지 않으며 기존 Redisson ownership을
  바꾸지 않는다.
- allowlist 위반 `SecurityException`은 즉시 전파하고 fallback하지 않는다.
- malformed Protobuf 또는 trusted allow-all profile의 class-resolution 실패는 기존
  fallback branch로 간다.
- multi-component/composite input은 `nioBuffer()`가 병합 copy를 만들 수 있으므로
  compatibility-copy 경로를 사용하며 lower-copy claim 대상이 아니다.
- fallback codec에는 현재와 동일하게 독립된 copied buffer를 전달한다. 이 copy는
  fallback codec이 input index/lifetime을 변경해도 원래 Redisson buffer와 격리하기
  위한 compatibility boundary다. codec이 temporary buffer를 생성하고 소유하며
  fallback decoder 성공/예외 모두 `finally`에서 정확히 한 번 release한다. fallback
  decoder는 이 buffer를 retain하거나 반환값에 view를 보관할 수 없다.
- encode 경로는 #649 구현을 재사용하며 변경하지 않는다.
- 기존 encoded fixture와 새 decode 경로가 양방향 호환되어야 한다.

`ByteBuffer` parse가 Protobuf 내부 allocation을 모두 제거한다고 간주하지 않는다.
이 설계가 제거하는 것은 decode 직전의 명시적 `getBytes(copy = true)` 배열이다.

## 6. Benchmark 및 evidence gate

기존 `benchmark/protobuf-codec-benchmark`를 확장하고 새 module은 만들지 않는다.

### 6.1 Profile 분리

- strict state는 `ProtobufSerializer()`와 `RedissonProtobufCodec()`만 사용한다.
- trusted state는 `ProtobufSerializer.trustedInternalProtobuf()`를 사용해 fallback
  encode/decode를 명시적으로 측정한다.
- benchmark 이름은 strict, trusted, ByteArray baseline, buffer candidate를
  구분한다.

최소 비교 cell은 다음과 같다.

| Family | Baseline | Candidate | Claim eligibility |
|---|---|---|---|
| Serializer encode | `serialize(message): ByteArray` | `serializeTo(message, reused buffer)` | Candidate only |
| Serializer decode | `deserialize(bytes)` | `deserializeFrom(reused buffer view)` | Candidate only |
| Redisson decode | explicit copied-ByteArray control | production `nioBuffer` view | Candidate only |
| Trusted fallback | trusted ByteArray encode/decode | compatibility buffer path | Ineligible |

heap 및 direct candidate는 별도 method로 측정한다. setup은 payload, encoded input,
buffer allocation, capacity 검증, semantic equality를 완료하고 timed method는 API
호출과 결과 소비만 수행한다.

benchmark state는 thread-confined다. encode target은 `@Setup(Level.Invocation)`에서
allocation 없이 원래 position/limit으로 복원하며 timed method 안에서 `clear`,
`duplicate`, 새 buffer allocation을 하지 않는다. decode는 invocation setup에서
재사용 source의 bounded position/limit을 복원한다. 연속 invocation 회귀 테스트는
overflow, stale read, position drift가 없음을 검증한다.

Redisson baseline은 benchmark-only copied control이다. production candidate와 같은
bounded-input parse, allowlist 검증, warmed loader-scoped class resolution/cache, unpack,
result consumption을 수행하고, handoff만 `getBytes(copy = true)`와 contiguous
`nioBuffer()`로 다르다. composite compatibility cell은 별도 claim-ineligible control로
둔다.

canonical payload는 `BenchmarkMessage(id = 42, payload =
"protobuf-payload-".repeat(128))`로 고정한다. setup은 serialized length와 SHA-256
payload fingerprint를 metadata에 기록하고 모든 baseline/candidate cell의 semantic
equality와 동일 input을 검증한다.

### 6.2 Fail-closed completeness

module-local validator는 다음 조건에서 non-zero로 실패한다.

- 기대한 benchmark method가 JSON에 하나라도 없다.
- 예상하지 않은 method가 나타나 profile/matrix가 drift한다.
- `gc.alloc.rate.norm` 또는 primary score가 누락됐다.
- `gc.alloc.rate.norm` unit이 `B/op`가 아니거나 throughput mode/unit이 기대값과
  다르다.
- score가 numeric finite non-negative 값이 아니거나 malformed type이다.
- 두 run의 method 집합, JDK/commit/flags, payload/config identity가 다르다.
- run ID가 같거나 기존 evidence directory를 덮어쓴다.
- baseline/candidate pair가 불완전하다.

validator는 작은 fixture unit test로 missing, unexpected, duplicate, wrong unit,
NaN/infinity/negative/malformed metric, identity mismatch를 RED/GREEN으로 검증한다.
실패 시 evidence path, missing/unexpected method, 누락/invalid metric, mismatch field의
두 값, remediation command를 출력하며 raw evidence 옆에 machine-readable validation
summary를 저장한다. raw JMH failure가 task exit code만으로 숨겨지지 않도록 JSON
method-set proof가 delivery gate가 된다.

### 6.3 Measurement rule

- JMH GC profiler의 `gc.alloc.rate.norm` B/op가 primary metric이다.
- throughput은 diagnostic이며 승인 기준이 아니다.
- evidence profile은 throughput mode, one thread, two forks, warmup 3회, measurement
  5회, 각 1초, `-prof gc`, JSON output으로 고정한다. fork JVM은
  `-Xms1g -Xmx1g -XX:+UseG1GC`로 고정하고 그 밖의 appended JVM arg는 허용하지
  않는다. canonical command의 `-jvmArgsAppend`와 invocation 자체를 README와
  report에 기록하며 validator는 두 run이 이 exact arg list를 공유하는지 검사한다.
- 동일 exact head/JDK/flags/payload에서 서로 다른 run ID로 두 번 순차 실행하며
  다른 heavy work를 병렬로 실행하지 않는다.
- measurement는 clean implementation commit에서 실행한다. commit, tracked tree hash,
  clean status, OS/arch/CPU, JVM vendor/full version, Gradle/JMH version, heap/GC/JVM args,
  thread/fork/warmup/measurement 설정, CPU/power state, concurrent-heavy-work 부재,
  serialized length와 payload/config fingerprint를 environment manifest에 기록하고 두
  run identity를 검증한다.
- 두 run raw output은 ignored `build/issue-757-evidence/<run-id>/`에 먼저 staging한다.
  두 run 검증이 끝난 뒤에만 docs evidence 경로로 복사하여 run 1 artifact가 run 2의
  clean-tree gate를 오염시키지 않게 한다.
- candidate가 두 run 모두 대응 ByteArray baseline보다 최소 5% 낮을 때만
  allocation reduction을 주장한다.
- candidate가 두 run 모두 baseline보다 5% 이상 높으면 delivery blocker이며 해당
  optimized dispatch unit(serializer encode, serializer decode, Redisson contiguous
  decode)을 revert한다. benchmark validator와 evidence tooling은 유지한다.
- mixed direction 또는 절대값 5% 미만은 `inconclusive`이며 raw evidence만 보존하고
  긍정 문구를 쓰지 않는다. missing/invalid metric은 inconclusive가 아니라 gate
  failure다.
- fallback/compatibility cell은 수치와 무관하게 claim-ineligible이다.

raw JSON, derived CSV, environment metadata는
`docs/benchmarks/raw/issue-757/<run-id>/`에 두고, 결론은
`docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md`에 기록한다.

## 7. 보안, compatibility, resource 경계

- `Any.typeUrl` allowlist와 package-prefix spoofing 방지는 변경하지 않는다.
- `Class.forName(..., initialize = false, ...)` 동작을 유지한다.
- strict profile은 non-Protobuf value/bytes를 계속 거부한다.
- trusted fallback은 non-null public constructor argument 또는
  `trustedInternalProtobuf` factory로만 명시적으로 활성화하며 두 진입점은 동등한
  compatibility semantics를 유지한다. zero-argument 생성자는 strict다.
- `ALLOW_ALL_CLASSES_UNSAFE`는 기존 migration opt-in으로만 남고 기본값이 되지
  않는다.
- `ByteArray` API, JVM binary/source compatibility, null/empty policy, wire bytes를
  유지한다. cache implementation은 loader isolation을 강화하되 성공/실패 semantics는
  유지한다.
- caller buffer와 Redisson `ByteBuf`의 ownership 및 reference count를 바꾸지 않는다.
- new dependency, new module, direct-buffer-per-call allocation을 추가하지 않는다.

## 8. 실패 모드와 처리

1. **Target overflow 또는 read-only target:** backend 호출 전에 실패하고 caller
   position을 보존한다. capacity를 늘리거나 replacement buffer를 할당하지 않는다.
2. **Malformed 또는 truncated Protobuf:** strict serializer/codec은 기존 exception
   contract로 실패한다. trusted fallback만 기존 compatibility branch를 사용한다.
3. **Allowlist 위반 또는 prefix spoofing:** class loading 전에 차단하고 절대
   fallback하지 않는다.
4. **Class resolution 실패:** strict profile은 실패한다. 기존 unsafe/trusted profile의
   fallback semantics만 유지한다.
5. **ByteBuf view/ownership 오류:** reader index와 `refCnt`가 바뀌거나 use-after-release가
   발생하면 delivery blocker다. codec-owned copied fallback buffer는 decoder 호출
   범위에서 `finally`로 release하며 retain/escape를 허용하지 않는다.
6. **Benchmark method 누락:** Gradle/JMH process가 zero exit여도 validator가 실패한다.
   누락된 run은 evidence로 인정하지 않는다.
7. **Allocation delta 미재현 또는 회귀:** mixed/sub-threshold는 기능 변경을 유지할
   수 있지만 긍정 문구 없이 `inconclusive`로 기록한다. 두 run 모두 +5% 이상인
   optimized dispatch unit은 revert한다.
8. **문서 drift:** KDoc, English/Korean README, benchmark report, issue DoD가 실제
   dispatch와 다르면 PR delivery를 중단한다.

## 9. 테스트 전략

### MessageSupport 및 ProtobufSerializer

- heap/direct/sliced/read-only input의 non-zero position/limit.
- heap/direct target의 non-zero start position과 success commit.
- read-only target, exact capacity, insufficient capacity, failure rollback.
- failure-injection 후 position은 rollback되지만 attempted range는 undefined이며
  clear/reinitialize/discard가 필요함.
- ByteArray와 ByteBuffer의 wire-byte equality 및 cross-decode.
- empty input의 `null`, malformed input, wrong requested message type, 기존
  `BinarySerializationException` message/cause chain.
- strict non-Protobuf rejection과 trusted fallback compatibility.
- allowed custom prefix, blank prefix, prefix spoofing, untrusted `typeUrl`.
- allowlisted non-Message class 차단과 두 effective classloader의 같은 class-name cache
  isolation.
- non-null constructor fallback과 trusted factory의 동등성.

### Redisson

- 기존 ByteArray fixture와 `nioBuffer` decode의 wire compatibility.
- reader/writer index 및 `refCnt` 불변.
- contiguous direct/heap input의 bounded view와 multi-component/composite input의
  compatibility copy를 분리해 readable range만 decode.
- strict malformed payload와 allowlist failure가 fallback하지 않음.
- trusted fallback이 copied isolated buffer로 기존 value를 decode하고 성공/예외
  모두 temporary `refCnt`가 0이 됨.
- #649 encode 경로와 codec family round trip 회귀.

### Benchmark gate

- expected method set complete/missing/unexpected/duplicate fixture.
- strict/trusted profile separation.
- GC metric missing 및 run identity mismatch failure.
- wrong unit, NaN/infinity/negative/malformed score와 environment identity mismatch
  failure.
- two-run baseline/candidate comparison과 claim-ineligible fallback.
- allocation-free invocation reset 및 다회 연속 호출 상태 안정성.
- `:protobuf-codec-benchmark:tasks --all`, compile, smoke, 두 fresh GC run.

Testcontainers, Redis server, network service는 필요하지 않다. codec unit tests는
in-memory `ByteBuf` 및 codec contract로 충분하며 benchmark는 다른 worktree/agent의
heavy command와 병렬 실행하지 않는다.

## 10. 문서와 전달

- public KDoc는 buffer position/limit, overflow, failure, ownership, optimized versus
  fallback 경계를 설명한다.
- KDoc/README는 별도 size API를 추가하지 않는 이유를 설명한다. exact size를 얻기
  위한 선행 `Any.pack`은 실제 write의 pack을 중복하므로 caller는 known payload
  envelope에 맞춘 reusable oversized buffer를 우선 사용한다. 문서는 internal/test용
  `Any.serializedSize` exact-capacity 산정, non-zero position, oversized reuse,
  partial-write 실패 후 clear/reinitialize/discard 예제를 함께 제공한다.
- README locale pair에 기존 ByteArray 호출자는 migration이 필요 없고 caller-owned
  reusable buffer가 이미 있을 때만 새 API를 선택한다는 지침을 둔다. strict가
  기본이며 non-null fallback constructor/factory는 신뢰 저장소에만 사용한다.
- `io/protobuf/README.md`와 `README.ko.md`를 함께 갱신한다.
- benchmark module README locale pair는 method matrix, 실행/검증 command, raw report
  위치, throughput 비보장 문구를 공유한다.
- allocation 숫자의 단일 source of truth는 benchmark report다. README는 숫자를
  복제하지 않고 report를 링크한다.
- `CHANGELOG.md`는 public API와 measured lower-copy 범위만 요약한다.
- issue #757의 1차 PR DoD는 core/Redisson/evidence 항목을 갱신하고 Lettuce는 후속
  PR로 명시한다.
- PR은 `bluetape4k/bluetape4k-projects`, base `develop`, head
  `feat/issue-757-protobuf-buffer-core`로 만들며 body의 마지막 `##` heading은
  `## DoD Status`다.
- exact-head CI/review가 완료되면 merge-ready로 보고하고 fresh merge approval을
  기다린다.

## 11. Acceptance Criteria

- 새 MessageSupport buffer API가 기존 `Any` wire bytes와 호환된다.
- valid allowlisted Protobuf success path의 `ProtobufSerializer` buffer dispatch는
  명시적 final `ByteArray` handoff를 사용하지 않는다.
- Redisson strict contiguous decode는 `getBytes(copy = true)` 대신 bounded
  `nioBuffer` view를 사용하고 input indices 및 `refCnt`를 보존한다.
- multi-component/composite와 trusted fallback은 명시적 compatibility-copy,
  claim-ineligible 경로이며 codec-owned temporary buffer를 정확히 release한다.
- strict exception/allowlist 경계와 trusted fallback compatibility가 유지된다.
- class resolution은 Message assignability와 effective-loader cache isolation을
  검증하며 security/type 위반을 fallback하지 않는다.
- heap/direct/sliced/read-only, position/limit, overflow, malformed, allowlist,
  fallback, wire, ownership 테스트가 통과한다.
- empty heap/direct/sliced view는 `null`이며 exact-capacity와 reusable oversized
  non-zero-position 예제가 문서/테스트에 일치한다.
- expected benchmark method 누락은 task/report gate를 non-zero로 실패시킨다.
- 두 fresh GC-profiler run과 environment/raw/derived evidence가 커밋된다.
- measurement exact head의 clean tracked-tree hash와 canonical environment/payload
  identity가 두 run에서 일치한다.
- positive allocation claim은 두 run의 같은 방향 5% 이상 B/op 감소 cell에만
  존재한다.
- fallback/compatibility와 inconclusive cell에는 긍정 성능 문구가 없다.
- 두 run 모두 +5% 이상 allocation regression인 optimized dispatch unit은 diff에서
  제거된다.
- KDoc, README locale pairs, benchmark report, `CHANGELOG.md`, issue DoD가 source와
  일치한다.
- README migration guidance는 기존 ByteArray 무변경, reusable-buffer 선택 기준,
  strict default, trusted-store-only fallback opt-in을 명시한다.
- 새 module/dependency, Lettuce/Kafka/compressor, release/tag/publish/settings 변경이
  diff에 없다.
- final exact-head review는 P0=0, P1=0이며 PR은 merge approval 경계에서 멈춘다.

## 12. Definition Of Done

- 승인된 strict-first 범위가 spec, plan, tests, implementation, evidence, docs에
  추적 가능하다.
- public API와 Redisson hot path가 compatibility/security/resource contract를
  유지한다.
- benchmark completeness와 allocation claim이 fail-closed evidence로 검증된다.
- targeted tests, benchmark tests, Detekt/static checks, `git diff --check`, 관련
  module build가 fresh evidence로 통과한다.
- issue-linked PR의 repo/base/head, milestone, labels, assignee, final DoD heading,
  exact head가 live 상태와 일치한다.
- Lettuce 후속 PR과 merge/release/publish/cleanup은 각자의 별도 gate에 남는다.
