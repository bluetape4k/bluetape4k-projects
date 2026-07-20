# Issue #757 Lettuce Protobuf Buffer Integration Design

- Issue: [#757 Add ByteBuffer-oriented Protobuf serializer and codec APIs](https://github.com/bluetape4k/bluetape4k-projects/issues/757)
- Parent delivery tracker: [#898 Epic: 1.12.0 delivery tracking](https://github.com/bluetape4k/bluetape4k-projects/issues/898)
- Related Redis umbrella: [#756 Reduce Redis codec allocation with ByteBuffer and ByteBuf paths](https://github.com/bluetape4k/bluetape4k-projects/issues/756)
- Milestone: `1.12.0`
- Branch: `feat/issue-757-lettuce-protobuf-buffer`
- Baseline authority: `origin/develop@4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88`
- Dependency authority: resolved `io.lettuce:lettuce-core:7.6.0.RELEASE`, Netty
  `4.2.16.Final` source, and the repository's current codec contracts
- Primary decision metric: normalized allocation, `gc.alloc.rate.norm` in B/op
- Delivery stop: exact-head pull request is merge-ready; merge requires fresh approval

## 1. 문제와 목표

#757의 core, serializer, Redisson, benchmark evidence 작업은 병합됐지만 Lettuce
integration은 후속 PR로 명시적으로 남아 있다. 현재
`LettuceProtobufCodecs.protobuf()`와 `trustedInternalProtobuf()`는 일반
`LettuceBinaryCodec`을 반환하며, Lettuce가 제공한 `ByteBuf`에 값을 쓸 때도 다음
경로를 사용한다.

```text
ProtoMessage
  -> ProtobufSerializer.serialize(value)
  -> payload-sized ByteArray
  -> ByteBuf.writeBytes(byteArray)
```

Lettuce 7.6.0의 `CommandArgs.encode`는 `ToByteBufEncoder.isEstimateExact()`가 false인
codec에 temporary `ByteBuf`를 만들고 codec이 그 buffer에 직접 쓰도록 한다. 현재
`LettuceBinaryCodec.estimateSize(customValue)`는 `-1`이므로 이 경로에서 직렬화가
두 번 실행되지는 않지만, Protobuf payload 전체 크기의 최종 `ByteArray` handoff는
남아 있다.

이 작업의 목표는 strict 및 trusted-internal Protobuf value의 Lettuce
`ToByteBufEncoder` 성공 경로에서 그 최종 `ByteArray` handoff를 제거하고, 기존
factory signature, wire bytes, 보안 경계, fallback, buffer ownership을 유지하는
것이다. Protobuf `Any.pack`과 `ByteString` 내부 allocation까지 제거하는 zero-copy는
목표가 아니다. 개선 주장은 두 fresh JMH GC-profiler run에서 재현된 normalized
allocation 감소에만 허용한다.

## 2. 권한과 범위

live issue #757, 병합된 core 설계와 구현, 현재 `develop`, Lettuce 7.6.0 source,
#1039의 allocation evidence 규칙이 권위다. 이 설계는 release, tag, publish,
repository settings, credential 작업을 승인하지 않는다.

### 포함

- uncompressed strict `LettuceProtobufCodecs.protobuf()`의 Protobuf-message
  `ToByteBufEncoder.encodeValue(value, target)` 경로.
- uncompressed `trustedInternalProtobuf()`의 Protobuf-message direct path와 기존
  non-Protobuf fallback compatibility.
- 기존 factory의 source/binary return type 및 호출 형태 보존.
- heap/direct/composite `ByteBuf`, non-zero writer index, capacity expansion,
  failure, fallback, wire compatibility, reference-count 테스트.
- 기존 `benchmark/protobuf-codec-benchmark`의 Lettuce copied baseline/direct-write
  candidate matrix, fail-closed validator, 두 fresh allocation run과 report.
- 관련 KDoc, locale-paired README/manual, `CHANGELOG.md`, issue DoD 갱신.

### 제외

- `LettuceBinaryCodec` 전체 serializer family의 buffer SPI 일반화. #756 범위다.
- gzip, deflate, LZ4, Snappy, Zstd Protobuf codec. 압축 전에 완성된 byte sequence가
  필요하므로 #755/#756에서 별도로 측정하고 설계한다.
- `encodeValue(value): ByteBuffer` compatibility method 최적화.
- Lettuce `decodeValue(ByteBuffer)` 저복사 변경. 병합된 #757 core에서 direct
  Protobuf decode가 allocation을 악화시켜 compatibility-copy로 되돌린 측정 결과를
  따른다.
- key encode/decode, `estimateSize`, Redis command framing, connection lifecycle 변경.
- 새 serializer SPI, 새 Gradle module, 새 external/production dependency, public factory
  name 변경. 기존 `:bluetape4k-lettuce`에 대한 benchmark-only project dependency는
  Lettuce comparison cell을 위해 허용한다.
- throughput 또는 zero-copy 보장.

## 3. 검토한 접근

### 3.1 선택: 내부 Protobuf subtype으로 `ToByteBufEncoder`만 특화

`LettuceBinaryCodec`을 상속 가능하게 만들고 `LettuceProtobufCodecs` 안에 private
nested subtype을 둔다. subtype은 nullable target을 받는 정확한
`encodeValue(value, target: ByteBuf?)` overload만 재정의한다. strict 및 trusted
uncompressed factory는 선언된 반환형 `LettuceBinaryCodec<V>`를 그대로 유지하면서
그 subtype instance를 만든다. subtype constructor와 packed writer seam은 private라서
새 외부 생성 또는 확장 surface가 되지 않는다.

이 접근은 기존 public factory의 source/binary descriptor, 한 인자 constructor,
일반 codec의 동작을 보존한다. Protobuf 지식은 `io/protobuf` module에 남고
`infra/lettuce`가 Protobuf dependency를 얻지 않는다. specialization은 packed
`Any`를 exact-size bounded absolute-index `ByteBuf` output에 기록한다. Protobuf의
`CodedOutputStream(OutputStream, 0)`은 구현상 최소 20-byte internal buffer를 만들지만,
payload-sized `ByteArray`는 만들지 않는다. 따라서 이 설계는 allocation-free나
zero-copy가 아니라 payload-sized handoff 제거를 측정하는 설계다.

`LettuceBinaryCodec`을 `open`으로 바꾸면 상속 가능성이 public ABI에 추가되고 이후
외부 subclass compatibility를 유지해야 하는 장기 비용이 생긴다. 이 비용은 measured
Lettuce specialization을 기존 factory 반환형 안에서 제공하기 위해 의도적으로
수용한다.
변경 범위를 제한하기 위해 public one-argument constructor, `serializer` property,
key encode/decode, single-argument value encode, decode, estimate method는 기존
access/finality를 유지하고 target을 받는 value encode만 `open`으로 둔다. 기존
subclass가 없으므로 기존 동작 변경은 없지만, baseline/candidate `javap -p -s` diff,
Kotlin/Java compile fixture, Java reflection 검토로 descriptor와 access flag drift 및
externally accessible Protobuf codec class가 없는지 증명한다. 허용되는 flag 변화는
`LettuceBinaryCodec` class와 target-taking value encode overload의 `final` 제거뿐이다.
해당 overload KDoc에는 null no-op, index commit, ownership, failure propagation을 포함한
public override contract와 subclass compatibility 부담을 명시한다.

### 3.2 기각: 모든 `LettuceBinaryCodec`에서 `BinarySerializer.serializeTo` 사용

코드는 작지만 Kryo, Java, compression serializer를 포함한 전체 Lettuce family의
error, sizing, direct-buffer 특성을 바꾼다. 이는 #757의 Protobuf 후속 범위를 넘어
#756의 공통 SPI 결정과 benchmark를 선점한다.

### 3.3 기각: 새 opt-in public factory 또는 새 public codec type

기존 호출자는 계속 allocating path를 사용하므로 #757의 남은 integration을
완료하지 못한다. public API surface와 migration 부담도 불필요하게 늘어난다.

### 3.4 기각: `ProtoMessage.serializedSize`를 `estimateSize`로 반환

Lettuce의 exact estimate contract는 실제 Redis wire에 쓰는 Protobuf `Any` envelope
크기와 일치해야 한다. message 자체 크기는 envelope의 type URL과 field framing을
포함하지 않으며, exact size를 얻기 위해 미리 `Any.pack`하면 command encode에서
다시 pack되어 작업과 allocation이 중복된다. 현재 `-1` 동적 확장 계약을 유지한다.

### 3.5 기각: `ByteBuf.nioBuffer()`에 직접 기록

Netty 4.2의 `nioBuffer(index, length)` 계약은 원본과 공유하거나 복사된 content를
반환할 수 있다. `nioBufferCount() == 1` 또는 `isContiguous()`만으로 arbitrary custom
buffer의 aliasing을 증명할 수 없고, detached view에 쓴 뒤 원본 `writerIndex`만
전진시키면 wire corruption과 stale-data 노출이 생긴다. 구체 구현 class allowlist나
runtime alias probe도 취약하고 hot-path 비용이 크므로 사용하지 않는다. absolute
`ByteBuf.setByte/setBytes` writer는 원본 storage에 쓰는 Netty 계약을 직접 사용한다.

## 4. 구조와 dispatch

선택한 데이터 흐름은 다음과 같다.

```text
Lettuce CommandArgs
  -> ToByteBufEncoder.encodeValue(value, target)
  -> internal LettuceProtobufCodec
       target == null
         -> immediate return without value inspection or serialization
       ProtoMessage + non-null writable target
         -> ProtoAny.pack(value) once
         -> exact Any.serializedSize preflight
         -> target.ensureWritable(size)
         -> bounded absolute-index ByteBuf output
         -> packed-Any writer (fixed small protobuf buffer)
         -> target.writerIndex(start + size)
       otherwise
         -> LettuceBinaryCodec.encodeValue
         -> serializer.serialize(value)
         -> target.writeBytes(byteArray)
```

private nested subtype은 다음 순서로 dispatch한다.

1. target이 null이면 value type 검사, pack, serialize 없이 즉시 반환한다.
2. value가 `ProtoMessage`가 아니면 superclass compatibility path로 보낸다.
3. 시작 `writerIndex`와 `refCnt`를 읽고 `ProtoAny.pack(value)`를 정확히 한 번 수행한다.
   pack 자체가 실패하면 기존 `AbstractBinarySerializer.serialize`와 동일한
   `BinarySerializationException("Fail to serialize. graphType=...")` type/message/cause
   contract로 변환한다.
4. packed `Any` exact size를 계산하고 `ensureWritable(size)`로 capacity를 확보한다.
5. private bounded writer가 `[start, start + size)`에 absolute write하고 exact count를
   확인한다.
6. 모든 write와 flush가 성공한 경우에만 `writerIndex(start + size)`를 한 번 호출한다.

heap, direct, sliced/wrapped, zero/one/multi-component composite target은 동일한
absolute writer 계약을 사용한다. NIO capability probe나 broad catch/fallback은 없다.
strict non-Protobuf value는 superclass를 거쳐 기존 `ProtobufSerializer` 예외로
실패하고, trusted non-Protobuf value는 기존 fallback bytes를 유지한다. codec은
target을 교체하거나 retain/release하지 않는다.

## 5. API와 compatibility 계약

### 5.1 유지되는 public surface

다음 호출과 JVM descriptor는 바뀌지 않는다.

```kotlin
fun <V: Any> LettuceProtobufCodecs.protobuf(): LettuceBinaryCodec<V>
fun <V: Any> LettuceProtobufCodecs.trustedInternalProtobuf(): LettuceBinaryCodec<V>

LettuceBinaryCodec(serializer: BinarySerializer)
```

현재 `encodeValue(value, target: ByteBuf?)`의 nullable Kotlin signature와 null immediate
no-op도 source/behavior compatibility surface다. subtype은 null 확인을 첫 연산으로
유지하며 Java reflection 호출에서도 같은 동작을 보장한다.

`gzipProtobuf`, `deflateProtobuf`, `lz4Protobuf`, `snappyProtobuf`, `zstdProtobuf`와
trusted compressed factory는 계속 일반 `LettuceBinaryCodec`을 만든다. factory
반환 객체의 concrete class name이나 `javaClass`는 기존 API 계약이 아니며 문서에서
의존하도록 안내하지 않는다. `serializer` property와 `toString()`의 의미는 유지한다.

### 5.2 `ByteBuffer` compatibility method

Lettuce `RedisCodec.encodeValue(value): ByteBuffer`는 기존
`ByteBuffer.wrap(serializer.serialize(value))` 동작을 유지한다. Lettuce 7.6 source가
이 method를 compatibility surface로 정의하고 direct `ByteBuf` 쓰기를
`ToByteBufEncoder`에 별도로 제공하므로, 이번 allocation 주장은 target을 받는
overload에만 적용한다.

### 5.3 wire 및 security

- optimized bytes는 기존 `packMessage(value)` 및 `ProtobufSerializer.serialize`
  결과와 byte-for-byte 동일해야 한다.
- strict factory는 non-Protobuf value를 계속 거부한다.
- trusted factory는 `ProtoMessage`만 optimized path로 보내고 다른 value는 기존 Kryo
  fallback serialization을 그대로 사용한다.
- `Any.typeUrl`, allowlist, class-loading, trusted decode 정책은 변경하지 않는다.
- zero-argument factory의 기본 decode allowlist는 계속 `io.bluetape4k.*`와
  `com.google.protobuf.*`다. 이 범위 밖 application message는 encode할 수 있어도
  기본 factory로 decode할 수 없다.
- custom `allowedClassPrefixes`로 직접 만든 `ProtobufSerializer`는 일반
  `LettuceBinaryCodec` compatibility path를 사용하며 이번 optimized subtype 대상이
  아니다. 새 allowlist factory overload는 #757 범위를 넓히므로 추가하지 않는다.
- encoded Redis value는 이전 codec과 양방향 호환되어야 한다.

## 6. Buffer, failure, resource 계약

optimized encode는 시작 `writerIndex`를 저장하고 packed `Any`를 한 번 만든 뒤 exact
size를 계산한다. `ensureWritable(size)`가 성공하면 private bounded output이 현재
writer range에 absolute write한다. 이 output은 자체 cursor와 upper bound를 가지며
target index를 바꾸지 않는다. 성공 시 `writerIndex`를 정확히 `size`만큼 한 번
전진시킨다. production writer는 private function dependency로 주입되어 테스트가
부분 write 후 실패를 결정적으로 만들 수 있으나 public ABI에는 노출되지 않는다.
테스트는 factory가 반환한 private subtype의 private constructor에 reflection으로
접근해 writer dependency를 교체하며, 테스트 편의를 위한 `internal`/public seam은
추가하지 않는다.
private writer는 written count를 반환하며, 정상 반환하더라도 `written != size`이면
exact-count guard가 예외를 발생시키고 index commit을 금지한다.

| Condition | Result |
|---|---|
| exact/충분한 writable capacity | 기존 prefix 보존, exact byte count만큼 writer index 전진 |
| expandable capacity | target 자체가 확장된 뒤 동일 contract로 성공 |
| max capacity 부족 | 기존 Netty exception 전파, writer index 보존 |
| read-only target | 기존 Netty `ReadOnlyBufferException` 전파, indices/marks/refCnt 보존 |
| `ProtoAny.pack` 실패 | 기존 `BinarySerializationException` type/message/cause chain |
| writer 실패 | writer index 보존, 시도된 range bytes는 undefined |
| writer가 `size - 1` bytes 후 정상 반환 | exact-count failure, indices/marks/refCnt 보존 |
| released target | 기존 Netty reference-count exception 전파 |
| null target | value 검사/pack/serialize 없이 immediate no-op |
| heap/direct/composite/wrapped target | 동일한 absolute-write contract |
| strict non-Protobuf value | 기존 serializer exception type/message/cause chain |
| trusted non-Protobuf value | 기존 fallback bytes 및 decode compatibility |

codec은 input target의 `readerIndex`, marks, `refCnt`, ownership을 변경하지 않는다.
성공과 실패 모두 retain/release하지 않는다. `ensureWritable`로 증가한 capacity는
후속 writer 실패 시 축소하지 않으며, 실패한 attempted range의 contents도 rollback을
보장하지 않는다. caller는 실패 후 range를 clear/reinitialize하거나 buffer를
폐기해야 한다.

Lettuce가 생성한 temporary target의 최종 release는 Lettuce가 소유한다. codec이
해당 buffer 또는 bounded writer를 반환값, field, coroutine, 다른 thread로 escape시키지
않는다. 호출 동안 target은 thread-confined여야 한다.

## 7. Benchmark 및 evidence gate

기존 `benchmark/protobuf-codec-benchmark`에 existing project
`:bluetape4k-lettuce`를 benchmark-only dependency로 추가하고 Lettuce encode cells를
추가한다. production module dependency graph, 새 benchmark module, external library,
profiler dependency는 만들지 않는다.

### 7.1 비교 matrix

동일한 canonical `BenchmarkMessage`, packed payload, target start/headroom을 사용해
다음 네 method를 추가한다.

| Target | Baseline method | Candidate method | Claim eligibility |
|---|---|---|---|
| heap `ByteBuf` | `ProtobufCodecBenchmark.lettuceEncodeHeapCopied` | `ProtobufCodecBenchmark.lettuceEncodeHeapOptimized` | Candidate only |
| direct `ByteBuf` | `ProtobufCodecBenchmark.lettuceEncodeDirectCopied` | `ProtobufCodecBenchmark.lettuceEncodeDirectOptimized` | Candidate only |

fully qualified prefix는
`io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmark`다. baseline은 generic
`LettuceBinaryCodec(ProtobufSerializer())`, candidate는 production
`LettuceProtobufCodecs.protobuf()`를 사용한다. 두 쌍은 동일 allocator, final capacity,
max capacity, canonical prefix, initial reader/writer index, payload를 사용한다.

trial setup은 heap/direct buffer를 timed region 밖에서 동일 final capacity까지 미리
확장하고 canonical prefix와 initial indices를 설정한다. invocation setup은
`setIndex(0, prefixSize)`, prefix rewrite, `readerIndex(canonicalReaderIndex)` 순서로
복원한 뒤 prefix를 검증한다. timed method는 target allocation, capacity growth,
duplicate, payload 생성 없이 codec을 호출하고, allocation 없는 방식으로 resulting
writer index와 encoded range의 deterministic first/last-byte checksum을 `Blackhole`에
소비한다. checksum 관측은 index를 바꾸거나 decode하지 않는다. trial teardown에서
codec이 소유하지 않은 benchmark buffer를 정확히 한 번 release한다.

fixture validation은 모든 cell의 output length, prefix 보존, wire bytes, decoded
message, writer-index delta, post-call `refCnt`를 검증한다. composite direct-write와
trusted non-Protobuf fallback은 unit/control test로 검증하되 positive allocation
matrix에는 넣지 않는다.

### 7.2 fail-closed 규칙

기존 validator와 metadata matrix를 확장하여 다음을 non-zero failure로 처리한다.

- 새 Lettuce method 하나라도 missing, unexpected, duplicate 상태다.
- baseline/candidate pair 또는 heap/direct pair가 불완전하다.
- `secondaryMetrics["gc.alloc.rate.norm"]`, 그 metric의 `score`, `scoreError`, `B/op`
  unit 또는 run identity가 누락되거나 invalid다. `score`는 finite이고 `> 0`,
  `scoreError`는 finite이고 `>= 0`이어야 한다. JMH `primaryMetric` throughput 값은
  allocation 판정식에 사용하지 않는다.
- 두 run의 method set, exact commit, tracked-tree hash, built JAR SHA-256, JDK, JVM args,
  JMH mode, forks, warmup iterations/time, measurement iterations/time, threads, profiler
  configuration, allocator class, target capacities, benchmark parameters 또는
  payload/config fingerprint가 다르다.
- raw run ID가 같거나 evidence를 덮어쓴다.

validator fixture test는 complete, missing, unexpected, wrong-unit, missing score/error,
NaN/Infinity/negative score or error, identity mismatch, wrong method name, measurement
configuration mismatch와 Lettuce pair mismatch를 포함한다.

### 7.3 측정 및 주장

- existing canonical JMH GC profile과 환경 manifest를 그대로 사용한다.
- clean implementation exact head에서 다른 heavy work와 병렬 실행하지 않고 서로
  다른 run ID로 두 번 순차 실행한다.
- `gc.alloc.rate.norm` B/op가 primary metric이며 throughput은 diagnostic이다.
- 아래 식의 `candidateScore`, `candidateError`, `baselineScore`, `baselineError`는 모두
  `secondaryMetrics["gc.alloc.rate.norm"]`의 `score`와 `scoreError`다.
- positive claim은 두 run 각각에서 `candidate <= baseline * 0.95`,
  `baseline - candidate >= 8 B/op`, 그리고
  `candidateScore + candidateError < baselineScore - baselineError`를 모두 만족할 때만
  허용한다. validator metadata에 이 exact formula와 8 B/op floor를 기록한다.
- mixed direction, relative/absolute floor 미달 또는 uncertainty overlap은
  `inconclusive`로 기록하고 긍정 문구를 쓰지 않는다.
- optimized subtype 제거는 heap 또는 direct cell 중 하나라도 두 run 각각에서 위
  공식을 대칭 적용한 `candidate >= baseline * 1.05`,
  `candidate - baseline >= 8 B/op`,
  `candidateScore - candidateError > baselineScore + baselineError`를 모두 만족할 때
  실행한다. 그 외에는 cell별 positive claim을 판정하고 positive cell이 하나도 없으면
  `retained-inconclusive`로 처리한다. validator와 neutral evidence는 유지할 수 있다.
- delivery terminal precedence는 다음과 같다. eligible cell 하나라도 confirmed
  regression이면 `rejected-after-regression`, 그렇지 않고 eligible cell 하나라도 두 run
  모두 acceptance formula를 만족하면 `retained-accepted`, 나머지는
  `retained-inconclusive`다. `retained-accepted`에서도 positive claim은 acceptance를
  만족한 개별 cell에만 허용한다.
- 기능 및 compatibility 테스트가 통과해도 invalid/missing evidence로 성능 주장을
  대신할 수 없다.

측정 결과의 delivery terminal은 다음 세 가지다.

| Terminal | Production dispatch | Claim | Issue/PR disposition |
|---|---|---|---|
| `retained-accepted` | 유지 | 두 run 모두 acceptance formula를 만족한 cell만 positive | direct-write AC/DoD를 적용하고 merge-ready 진행 |
| `retained-inconclusive` | 유지 | neutral only | direct-write 기능 AC/DoD는 적용하되 성능 개선 주장은 금지하고 inconclusive evidence를 기록 |
| `rejected-after-regression` | subtype과 `open` ABI 변화 제거 | negative/neutral only | non-closing benchmark/evidence-only PR로 merge-ready 진행하되 direct-write AC/DoD 완료나 #757 종료를 주장하지 않음 |

기존 promoted evidence는 덮어쓰지 않는다. archive-aware replacement command가 먼저
기존 promoted generation을
`docs/benchmarks/raw/issue-757/archive/<old-delivery-commit>/` 아래 regular files로
복사하고, 새 delivery manifest의 `superseded_evidence`에 generation, relative path,
각 file SHA-256과 archive file-set hash를 기록한다. 이 committed archive는 replacement
backup cleanup 대상이 아니며 전용 validator를 통과하지 않으면 promotion을 거부한다.
기존 cleanup 가능 backup만 만드는 command는 이번 replacement에 사용할 수 없다. 새
raw JSON, derived CSV, environment metadata는 서로 다른 run ID, exact commit,
tracked-tree hash, built JAR SHA-256과 함께
`docs/benchmarks/raw/issue-757/<run-id>/` lifecycle을 따른다. verified delivery
manifest만으로 report를 재생성하고 promoted tree validator가 immutable archive, raw, derived,
metadata, report의 연결을 검증해야 한다. 결론은 기존 issue #757 allocation report에
Lettuce section으로 추가하고 README는 숫자를 복제하지 않고 report를 링크한다.
반복 replacement는 기존 `archive/` subtree를 다시 복사하지 않는다. 이전 manifest의
`superseded_evidence` entries는 동일 path/hash로 carry-forward하고, 바로 직전 active
generation의 non-archive file set만 새 `<old-delivery-commit>` archive에 한 번 복사한다.
기존 rollback bundle이 참조하는 legacy archive는 해당 generation file set에 정확히 한
번 포함하며 nested archive duplication을 validator가 거부한다.

## 8. 실패 모드와 처리

1. **잘못된 size estimate:** message 자체 크기 대신 packed `Any.serializedSize`를
   사용한다. 실제 write count가 exact size와 다르면 writer index를 commit하지 않고
   실패한다.
2. **분리된 NIO view:** NIO view를 사용하지 않는다. bounded writer는 target의
   absolute `setByte/setBytes`만 사용하므로 heap/direct/composite 모두 원본 storage에
   기록한다. detached `nioBuffer()`를 반환하는 hostile custom buffer도 NIO method가
   호출되지 않아 stale bytes를 commit할 수 없다.
3. **max-capacity 또는 writer 실패:** writer index와 `refCnt`를 보존한다. capacity
   expansion과 attempted bytes는 rollback하지 않으며 문서화된 Netty exception을
   전파한다.
4. **pack failure compatibility:** `ProtoAny.pack` failure는 기존 serializer와 동일한
   `BinarySerializationException` type/message/cause로 변환하고 target state는 보존한다.
5. **trusted fallback 우회:** only `ProtoMessage` type check가 direct dispatch를
   활성화한다. non-Protobuf value는 기존 serializer가 처리하므로 fallback wire와
   신뢰 경계를 바꾸지 않는다.
6. **strict profile 완화:** non-Protobuf value가 superclass path에서 성공하거나 다른
   예외로 바뀌면 delivery blocker다.
7. **buffer lifetime 오류:** codec이 retain/release하거나 bounded writer를 escape해
   `refCnt`, lifetime, thread confinement가 달라지면 delivery blocker다.
8. **compressed codec의 우발적 변경:** compressed factory가 specialized subtype을
   만들거나 diff/benchmark claim에 포함되면 범위 위반으로 되돌린다.
9. **allocation 개선 미재현:** 두 fresh run이 relative 5%, absolute 8 B/op,
   non-overlapping uncertainty를 모두 증명하지 못하면 긍정 성능 문구를 제거한다.
   두 run 모두 대칭 regression formula를 만족하면 optimized dispatch와 `open` ABI
   변화를 제거하고 `rejected-after-regression` terminal로 끝낸다.
10. **ABI drift:** factory descriptor, constructor, method descriptor, serializer property
   또는 existing Kotlin/Java caller compile이 바뀌면 delivery를 중단한다.

## 9. 테스트 전략

### `LettuceBinaryCodec`와 factory compatibility

- 기존 generic codec의 key/value encode/decode, estimate, toString 회귀.
- Kotlin 및 Java에서 기존 한 인자 constructor와 factory 반환형 compile proof.
- 변경 전후 `./gradlew :bluetape4k-lettuce:compileKotlin` 산출물에
  `javap -p -s io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec`를 실행해 기존
  descriptor 제거/변경 없음과 class/target-overload의 허용된 `final` 변화만 확인함.
- Kotlin 및 Java compile fixture와 reflection assertion을 targeted Gradle test로 실행함.
- Java reflection에서 private nested Protobuf codec의 public/protected constructor와
  externally accessible codec class가 없음.
- strict/trusted, Protobuf/non-Protobuf 조합 모두 null target 호출 시 value 검사와
  serialization 없는 no-op이며 Kotlin 및 reflective Java 호출 결과가 동일함.
- compressed factory가 계속 generic copied codec을 사용함.

### Protobuf direct encode

이 절의 direct-writer 전용 테스트는 candidate measurement commit과 retained terminal의
final head에 적용한다. `rejected-after-regression` final head에서는 subtype, private seam,
그 구현에만 존재하는 테스트를 제거해 N/A로 처리하고, delivery manifest가 모든 기능
테스트를 통과한 immutable candidate measurement commit, tracked-tree hash, JAR hash와
negative JMH evidence를 가리키는지 검증한다.

- heap/direct `ByteBuf`의 exact-capacity 및 reusable oversized target.
- non-zero reader/writer index와 prefix 보존, exact writer-index advancement.
- `packMessage` 및 이전 generic codec과 wire-byte equality, 양방향 decode.
- initial capacity가 작지만 max capacity 안인 target 확장.
- max-capacity 부족과 private writer seam으로 partial absolute write 후 발생시킨
  failure에서 reader/writer indices, observable marks, `refCnt` 보존. capacity와 attempted
  bytes는 변경될 수 있음.
- injected writer가 `size - 1` bytes만 쓰고 정상 반환하는 경우 exact-count guard가
  실패시키며 reader/writer indices, observable marks, `refCnt`를 보존함.
- private writer seam은 reflection으로만 주입하고 public/internal JVM surface가 추가되지
  않았음을 reflection/API diff로 함께 검증함.
- read-only target에서 `ReadOnlyBufferException`, reader/writer indices, observable marks,
  `refCnt` 보존.
- `ProtoAny.pack` failure에서 기존 `BinarySerializationException` type/message/cause와
  target state 보존.
- released target의 기존 Netty failure.
- zero/one/multi-component composite, sliced/wrapped target에서 정확한 direct bytes 생성.
- `nioBufferCount() == 1`이면서 detached NIO copy를 반환하는 hostile custom target도
  NIO API 호출 없이 정확한 bytes를 생성함.
- strict non-Protobuf rejection의 exception type/message/cause chain.
- trusted `ProtoMessage` direct path와 trusted non-Protobuf fallback fixture 호환.
- `encodeValue(value): ByteBuffer` 및 `decodeValue(ByteBuffer)` compatibility path 불변.

### Lettuce integration

- 기존 `LettuceProtobufCodecsTest` strict/trusted Redis round trip.
- 이전 codec으로 쓴 fixture를 새 codec이 읽고 새 codec bytes를 이전 codec이 읽음.
- Redis command encode 반복 호출에서 stale bytes, index drift, ownership 변화 없음.
- Testcontainers-backed Lettuce 검증은 다른 module/worktree의 container test와
  병렬 실행하지 않는다.

### Benchmark gate

- fixture의 heap/direct copied/optimized semantic equality와 invocation reset.
- exact expected method set complete/missing/unexpected/duplicate/wrong-name.
- Lettuce baseline/candidate 및 heap/direct matrix pair validation.
- GC metric의 `score`/`scoreError`/unit/range, execution-parameter identity,
  relative/absolute/uncertainty formula mismatch failure.
- heap/direct mixed-cell accepted/inconclusive/regression 조합과 terminal precedence.
- compile/smoke 후 두 fresh canonical GC-profiler run.

## 10. 문서와 전달

- public KDoc는 optimized `ToByteBufEncoder` 범위, compatibility path, target ownership,
  failure 후 range 처리, zero-copy 비보장을 설명한다.
- `io/protobuf` README locale pair와 manual은 기본 allowlist에 이미 포함된 기존
  factory 호출자만 migration 없이 direct path를 사용한다고 설명한다. 기본 allowlist,
  일반 application package decode 제한, custom-prefix serializer는 generic codec path라는
  사실과 compressed/decode/fallback 경로가 별도임을 명시한다. allowlist 밖의
  `MyMessage`가 곧바로 round-trip하는 예시는 쓰지 않는다.
- benchmark README locale pair와 manual은 새 method matrix, 실행/검증 command,
  B/op primary metric, two-run claim rule를 함께 갱신한다.
- allocation 수치의 단일 source of truth는 issue #757 benchmark report다.
- `CHANGELOG.md`는 measured direct-write 범위만 요약하고 throughput/zero-copy를
  주장하지 않는다.
- merge 전 #757은 open 상태로 유지하고 implementation, tests, promoted-tree validation,
  exact-head evidence 및 PR을 연결한다.
- PR은 `bluetape4k/bluetape4k-projects`, base `develop`, head
  `feat/issue-757-lettuce-protobuf-buffer`로 만들며 body의 마지막 `##` heading은
  `## DoD Status`다.
- exact-head CI, automated review, applicable human review artifact와 unresolved thread
  상태가 모두 통과하면 merge-ready로 보고하고 fresh merge approval을 기다린다.

### 10.1 Operational handoff와 rollback

issue assignee 또는 지정 maintainer가 운영 owner다. release 이후 wire mismatch,
unexpected encode exception 증가, writer-index/ref-count violation, Redis round-trip
regression 중 하나가 확인되면 첫 복구는 uncompressed strict/trusted factory를 generic
`LettuceBinaryCodec`으로 되돌리는 source revert다. revert 후 targeted compatibility
tests, 관련 module build, fresh allocation evidence와 promoted-tree validation을 다시
수행한다. release/tag/publish/재배포는 계속 별도 승인 경계다.

hot path에 새 telemetry를 넣지 않는다. consumer handoff에는 배포 전 baseline, 관측
기간, escalation owner와 함께 Redis command encode/SET 실패율, Netty reference-count
exception, JVM allocation/GC 변화, serialization/decode compatibility error를 기록한다.

`retained-accepted` 또는 `retained-inconclusive` terminal이 fresh 승인으로 merge된 뒤
같은 owner가 merge commit과 최종 evidence/report를 #757에 기록하고 issue를 close한다.
이어 #898의 #757 항목을 완료 처리하고 #756에는 Lettuce slice 완료와
compressed/custom-prefix/generic SPI 제외 범위를 링크한다. `rejected-after-regression`
terminal은 #757을 close하거나 #898을 완료 처리하지 않고 negative evidence와 남은
scope를 issue에 기록한다. evidence-only PR 자체는 동일한 exact-head 검증과 fresh merge
approval을 거쳐 merge할 수 있지만 `Closes #757` metadata를 사용하지 않는다. 이 post-merge closure는 merge-ready 검증과 별도 단계이며
milestone, labels, assignee를 다시 확인한다.

## 11. Acceptance Criteria

- uncompressed strict/trusted Protobuf factory의 public signature와 기존 호출 형태가
  유지된다.
- `retained-accepted`/`retained-inconclusive`에서는 target을 받는 production Lettuce
  Protobuf encode success path에 최종 payload-sized `ByteArray` handoff가 없다.
- `rejected-after-regression`에서는 optimized subtype과 이를 위해 추가한 `open` ABI
  변화가 최종 diff에 없고 direct-write 완료나 #757 종료를 주장하지 않는다.
- output은 기존 `Any` wire bytes와 byte-for-byte 호환된다.
- heap/direct/composite/wrapped target은 prefix, reader/writer indices, `refCnt`, ownership
  계약을 지키고 read-only target은 기존 Netty 예외와 target state를 보존한다.
- null target은 serialization 없는 no-op다. compressed, single-argument `ByteBuffer`
  encode, decode, trusted non-Protobuf, custom-prefix serializer는 명시적 compatibility
  path다.
- Protobuf pack failure, strict rejection, trusted fallback의 exception/security/wire
  semantics가 유지된다.
- max-capacity, writer failure, released buffer, repeated invocation 테스트가 통과한다.
- retained terminal에서는 injected writer의 exception 및 short-success 양쪽에서
  exact-count guard와 indices/observable marks/`refCnt` 보존이 검증된다. rejected
  terminal의 final head에서는 이 구현 전용 검사를 N/A로 처리하고 candidate measurement
  commit의 통과 기록과 manifest-bound hash를 검증한다.
- generic `LettuceBinaryCodec` 기존 behavior와 public descriptors가 유지된다.
- benchmark validator가 Lettuce method/pair/metric/run drift를 fail-closed로 차단한다.
- 두 fresh exact-head GC-profiler run과 raw/derived/environment evidence가 검증된다.
- positive allocation claim은 두 run 모두 relative 5%, absolute 8 B/op,
  non-overlapping uncertainty를 만족한 candidate에만 존재한다.
- heap 또는 direct cell 중 하나라도 두 run 모두 대칭 regression formula를 만족하면
  optimized subtype dispatch가 최종 diff에 없고, negative evidence는 기능 검증을 통과한
  immutable candidate measurement commit과 그 tracked-tree/JAR hash에 귀속된다.
- KDoc, locale-paired docs, benchmark report, `CHANGELOG.md`, issue DoD가 source와
  일치한다.
- 새 module, external/production dependency, generic serializer SPI, compressed
  optimization, decode optimization, release/tag/publish/settings 변경이 diff에 없다.
  existing `:bluetape4k-lettuce` benchmark-only project dependency만 허용한다.
- final exact-head review는 P0=0, P1=0이며 PR은 merge approval 경계에서 멈춘다.

## 12. Definition Of Done

- 승인된 Lettuce narrow slice와 세 measurement terminal 중 하나가 spec, plan, tests,
  implementation/evidence, docs에 추적 가능하다.
- public/source/binary compatibility, Protobuf wire/security, Netty ownership 계약이
  유지된다.
- allocation claim 또는 neutral/negative verdict가 production factory와 generic copied
  control의 동일-payload 비교 및 두-run fail-closed evidence로 검증된다.
- targeted unit/integration/benchmark tests, compatibility check, Detekt/static checks,
  `git diff --check`, 관련 module build가 fresh evidence로 통과한다.
- promoted evidence archive, new run identities, verified delivery manifest와 regenerated
  report가 promoted-tree validator를 통과한다.
- rejected terminal은 final head에서 direct-writer 구현/전용 테스트를 요구하지 않으며,
  candidate measurement commit의 기능-test 통과 기록, ancestry, tracked-tree/JAR hash와
  negative evidence 연결을 검증한다.
- issue-linked PR의 repo/base/head, milestone, labels, assignee, final DoD heading,
  exact head가 live 상태와 일치한다.
- merge, release, publish, tag, destructive cleanup은 각각 별도 승인 경계에 남는다.

## 13. Spec review convergence

독립된 caller, API/developer, stability, security, performance, operator 여섯 관점 검토와
추가 code/architecture adversarial 검토를 수행했다. 추가 검토에서 발견된 중복 P1/P2
findings를 아래 결정으로 통합했다. 모두 설계에 반영했으며 수정 영향 범위를 같은
code/architecture lane으로 재검토했다.

| Review concern | Disposition |
|---|---|
| nullable target compatibility | null-first no-op와 Kotlin/Java regression으로 반영 |
| detached/custom NIO view와 composite 안정성 | NIO path를 기각하고 absolute `ByteBuf` writer로 교체 |
| subtype 및 writer seam 노출 | private nested subtype/constructor/dependency와 ABI/reflection proof로 제한 |
| default allowlist와 custom-prefix caller | zero-migration 문구를 제한하고 custom path를 명시 |
| failure injection과 resource contract | partial-write private seam, commit-after-success, marks/refCnt 검증 반영 |
| JMH reset, observation, threshold, identity | exact method/reset/Blackhole, 5%+8 B/op+uncertainty, full metadata 반영 |
| evidence promotion과 운영 rollback | immutable archive/manifest/tree validation, owner/trigger/revert 절차 반영 |
| issue lifecycle | merge-ready와 post-merge #757/#898/#756 closure를 분리 |
| regression terminal과 unconditional DoD 충돌 | accepted/inconclusive/rejected terminal별 dispatch, claim, issue disposition을 분리 |
| benchmark project dependency와 no-dependency 충돌 | existing Lettuce benchmark-only dependency만 허용하고 external/production dependency는 금지 |
| allocation uncertainty input | `gc.alloc.rate.norm.score/scoreError`와 finite/range fixture를 고정 |
| serialization failure compatibility | pack failure wrapping과 raw Netty target failure 경계를 분리 |
| ABI/seam/read-only/archive 반복성 | javap+compile+reflection, private reflection seam, read-only case, non-recursive archive를 명시 |

최신 통합 결과와 수정 영향 재검토는 P0=0, P1=0, P2=0, P3=0으로 승인됐다. 이 판정은
설계 승인에 한정되며 구현 검증을 대신하지 않는다.
