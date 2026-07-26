# Issue #756 Lettuce Buffer Codec 설계 명세

## 1. 문서 상태

- 대상 이슈: [#756 Reduce Redis codec allocation with ByteBuffer and ByteBuf paths](https://github.com/bluetape4k/bluetape4k-projects/issues/756)
- 대상 저장소: `bluetape4k-projects`
- 대상 브랜치: `feat/issue-756-lettuce-buffer-codecs`
- 기준 브랜치: `origin/develop`
- 기준 커밋: `b00cc5440e47ad803e5aac21528b560fdd3b0474`
- 작업 유형: Type A Full Feature
- 문서 범위: #756의 첫 번째 Lettuce 전용 delivery slice
- 구현 상태: 미시작

## 2. 문제와 목표

`LettuceBinaryCodec`과 `LettuceJsonCodec`은 이미 Lettuce의 `ToByteBufEncoder`를 구현하지만, value encode 시 serializer가 만든 `ByteArray`를 다시 caller-owned `ByteBuf`로 복사한다. decode도 Lettuce가 제공한 `ByteBuffer`를 `ByteArray`로 복사한 뒤 serializer에 전달한다.

이 slice의 목표는 다음과 같다.

1. serializer가 지원할 때 기존 wire format을 caller-owned output으로 직접 기록한다.
2. Lettuce value encode에서 payload-sized `ByteArray` handoff를 제거할 수 있는 안전한 경로를 제공한다.
3. decode에서 codec 계층이 추가하는 `ByteBuffer -> ByteArray` 복사를 제거한다.
4. writer index, source state, reference ownership, 기존 예외 type/cause 계약과 보안 정책을 보존한다.
5. allocation 개선 주장을 두 번의 동일 조건 측정으로 제한한다.

이 작업은 throughput 개선, 모든 serializer의 zero-copy, Redis command semantics 변경을 목표로 하지 않는다.

## 3. 현재 근거

### 3.1 저장소 구현

- `BinarySerializer`와 `JsonSerializer`는 caller-owned `ByteBuffer` API와 allocating compatibility default를 이미 제공한다.
- JDK, Kryo, Jackson 2, Jackson 3 serializer는 `ByteBuffer` 직접 기록 경로를 가진다.
- Fory serialize와 Fastjson2 serialize는 현재 allocation evidence에서 fallback으로 분류된다.
- `LettuceBinaryCodec.encodeValue(value, target)`과
  `LettuceJsonCodec.encodeValue(value, target)`은 `serializer.serialize(value)` 결과를
  `ByteBuf.writeBytes`로 복사한다.
- 두 codec의 decode는 `ByteBuffer.getAllBytes()`를 먼저 실행한다.
- issue #757의 uncompressed Lettuce Protobuf codec은 실패 시 `writerIndex`를 commit하지 않는 bounded absolute-index writer를 이미 사용한다.
- issue #755 core slice는 caller-owned compressor API의 compatibility default만 제공한다. backend별 저할당 override는 아직 완료되지 않았다.

### 3.2 resolve된 외부 API

현재 `bluetape4k-lettuce` runtime classpath는 다음 버전을 resolve한다.

- Lettuce `7.6.0.RELEASE`
- Netty Buffer `4.2.16.Final`

Lettuce `ToByteBufEncoder` source는 다음을 명시한다.

- 구현체는 기존 `ByteBuf`에 직접 encode한다.
- 일반 `RedisCodec.encodeValue` 경로도 호환성을 위해 계속 구현해야 한다.
- `estimateSize`는 임시 buffer allocation의 추정치이며 exact 여부를 별도로 표현할 수 있다.

Netty `ByteBufOutputStream` source는 write할 때 underlying `ByteBuf.writerIndex`를 즉시 증가시킨다. 따라서 serialization 중간 실패에서도 index가 이동해 이 명세의 commit-on-success 계약을 만족하지 못한다. 이 클래스는 이번 Lettuce value encode 구현에 사용하지 않는다.

### 3.3 기존 allocation evidence

`docs/benchmarks/2026-07-18-bytebuffer-serializer-allocation.md`는 다음 output 후보만 두 번의 측정에서 allocation 감소를 인정했다.

- JDK serialize
- Kryo serialize
- Jackson 2 serialize
- Jackson 3 serialize

이 결과는 serializer 단독 `ByteBuffer` 측정이며 Lettuce `ByteBuf` dispatch를 자동으로 증명하지 않는다. 이번 slice는 Lettuce 경로를 다시 측정해야 한다.

## 4. 범위

### 4.1 포함

- `BinarySerializer` caller-owned `OutputStream` API와 compatibility default
- `JsonSerializer` caller-owned `OutputStream` API와 compatibility default
- `BinarySerializerDecorator.serializeBinaryToStream`의 allocating semantic-preserving override
- JDK, Kryo, Jackson 2, Jackson 3의 direct-output 후보 override
- `CompressableBinarySerializer.serializeBinaryToStream`의 allocating wire-preserving override
- `LettuceBinaryCodec`과 `LettuceJsonCodec`의 bounded absolute-index target encode
- 두 Lettuce codec의 bounded `ByteBuffer` decode dispatch
- Kotlin/Java source 및 binary compatibility proof
- heap/direct/bounded/hostile target ownership·index·failure tests
- Lettuce allocation benchmark와 fail-closed evidence
- `README.md`, `README.ko.md`, 공개 KDoc parity

### 4.2 제외

- Redisson `ForyCodec`, `FastForyCodec`, GZip/LZ4/Zstd wrapper 변경
- #755의 LZ4, Deflate, Snappy, Zstd 저할당 backend override
- Redis key encoding 변경
- wire format 또는 serializer 보안 policy 변경
- 신규 dependency 또는 신규 Gradle module
- serializer가 반환하는 객체 graph의 lifecycle 변경
- zero-copy 보장

## 5. 선택한 아키텍처

### 5.1 serializer output capability

두 serializer interface에 서로 다른 API를 추가한다.

```kotlin
interface BinarySerializer {
    @Throws(IOException::class)
    fun serializeBinaryToStream(graph: Any?, target: OutputStream): Int
}

interface JsonSerializer {
    @Throws(IOException::class)
    fun serializeJsonToStream(graph: Any?, target: OutputStream): Int
}
```

interface default는 기존 `serialize(graph)` 결과를 `target.write(bytes)`로 기록하고 byte count를 반환한다. 이 default는 source 및 binary compatibility를 위한 allocating fallback이다.

기존 `serializeTo(graph, ByteBuffer)`와 이름을 분리한다. `OutputStream` overload를 같은 이름으로 추가하면 기존 Java의 `serializeTo(value, null)` 호출이 모호해져 source compatibility를 깨뜨린다. 두 interface에도 서로 다른 JVM-visible 이름을 사용한다. 같은 `serializeToStream(Object, OutputStream)`
default를 양쪽에 추가하면 두 interface를 함께 구현한 기존 class에 default-method diamond가 생기기 때문이다. interface와 모든 concrete override는 `@Throws(IOException::class)`를 선언해 Java caller가 checked stream failure를 catch/declare할 수 있게 한다.

backend direct output 후보는 capability가 있을 때만 override한다. semantic decorator는 자체 wire behavior를 보존하기 위해 allocating override를 둘 수 있으므로 override 유무만으로 allocation 개선을 뜻하지 않는다. allocation 개선 표시는 benchmark evidence가 별도로 결정한다.

public open `BinarySerializerDecorator`는 Kotlin delegation이 새 default를 wrapped serializer로 직접 forward해 외부 subclass의 `serialize(graph)` override semantics를 우회하지 않도록
`serializeBinaryToStream`을 명시적으로 override한다. 이 path는 자신의 virtual `serialize(graph)` 결과를 기록하는 allocating semantic-preserving fallback이다. `CompressableBinarySerializer`는 control-failure 복원과 compressed wire 의도를 명시하기 위해 별도 override를 유지한다.

### 5.2 caller-owned stream 계약

- `target`은 caller-owned이며 serializer가 close하지 않는다.
- serializer는 `target` 참조를 보관하거나 호출 종료 뒤 접근하지 않는다.
- 완전한 wire 기록에 필요한 encoder 내부 drain은 수행하지만 caller의 `target.flush()`는 호출하지 않는다.
- default와 direct implementation 모두 caller target의 flush/close count를 증가시키지 않는다.
- 성공 시 정확한 기록량을 반환한다.
- 일반 `OutputStream`에는 rollback이 없으므로 실패 시 부분 기록은 unspecified다.
- null graph와 zero-byte wire는 기존 serializer 정책을 유지한다.
- 성공 가능한 최대 기록량은 `Int.MAX_VALUE`다. 이를 넘는 출력은 첫 초과 시
  `IllegalStateException("Serialized output exceeds Int.MAX_VALUE bytes.")`으로 실패한다. backend가 기존 serialization exception으로 분류하면 이 failure를 cause로 보존한다.
- interface default에서 기존 `serialize(graph)`가 실패하면 그 API의 기존 type/cause/identity를 그대로 전파하고, 이후 `target.write`가 실패하면 원 throwable을 그대로 전파한다.
- backend direct override는 기존 ByteBuffer direct 경로의 serialization exception type/cause 분류와 control-failure 정책을 backend별로 그대로 유지한다. JDK/Kryo가 raw cancellation을 복원하는 경로는 identity를 보존하고, Jackson 2/3가 cancellation을 `JsonSerializationException`으로 분류하는 현재 경로는 동일 wrapper type/cause를 유지한다.
- compressed compatibility override는 자체 `serialize(graph)`로 압축 wire를 만든 뒤 기록하며 nested
  `Error`와 cancellation을 기존 `BufferFailurePolicy`로 복원한다.

direct serializer가 close-on-completion API를 사용해야 하면 non-closing/non-flushing wrapper를 사용한다. wrapper의 `close()`와 `flush()`는 underlying stream을 닫거나 flush하지 않는다. encoder가 wrapper로 drain한 bytes만 target에 기록된다.

### 5.3 Lettuce target encode

Lettuce codec은 Netty `ByteBufOutputStream`이나 writable NIO view 대신 custom
`BoundedByteBufOutputStream`을 사용한다.

adapter는 다음 속성을 갖는다.

- construction 시 `start = target.writerIndex()`를 고정한다.
- `write`는 `setByte`/`setBytes` absolute operation만 사용한다.
- 각 write는 `nextWritten = Math.addExact(written, length)`와
  `requiredEnd = Math.addExact(start, nextWritten)`를 계산한다.
- `requiredEnd <= target.maxCapacity()`와 construction snapshot을 확인한 뒤
  `target.ensureWritable(nextWritten)`으로 누적 capacity를 확보하고 다시 snapshot을 확인한다.
- capacity 확보 뒤 requested high-water를 `nextWritten`으로 먼저 올리고 `set*`를 호출한다.
- `set*`가 성공한 뒤에만 `written = nextWritten`으로 갱신한다.
- write 중에는 원본 `writerIndex`를 변경하지 않는다.
- 실제 기록량을 내부에서 추적한다.
- `close()`와 `flush()`는 target lifecycle 및 index를 변경하지 않는다.
- target을 retain/release하지 않는다.
- codec은 serializer 호출의 성공/실패와 무관하게 `finally`에서 adapter를 seal한다.
- seal 이후 `write`는 target을 변경하지 않고 deterministic `IOException`으로 실패하며,
  `flush`와 `close`는 target을 변경하지 않는다.

built-in `LettuceBinaryCodec`/`LettuceJsonCodec` base implementation의 성공 commit 순서는 다음과 같다.

1. serializer 반환량을 받는다.
2. 반환량이 adapter의 실제 기록량과 같은지 검사한다.
3. target의 writer/reader/reference 상태가 construction snapshot과 같은지 검사한다.
4. `writerIndex(start + written)`을 한 번만 호출한다.

adapter는 reader/writer mark를 읽거나 변경하는 API를 호출하지 않는다. mark 보존은 mark/reset 행동 fixture로 검증하되, 관찰 불가능한 mark 값을 runtime drift 검사 대상으로 삼지 않는다.

null target은 serializer 호출 전 no-op이다. 이는 현재 `LettuceBinaryCodec` extension seam의 계약을 보존한다.

외부 `LettuceBinaryCodec` subclass는 target overload를 완전히 우회할 수 있다. 호환성을 위해 이 extension seam을 닫지 않으며, subclass override가 null no-op, wire/security parity, success-only writer-index commit, no retain/release를 직접 책임진다. built-in base 보장을 subclass에 자동으로 일반화하지 않는다.

정상 보장은 호출 동안 thread-confined인 Netty 계약 준수 `ByteBuf`를 대상으로 한다. `nioBuffer()`를 거부하는 target, bounds/capacity failure와 snapshot drift는 hostile matrix에 포함한다. 반면
`writerIndex(newValue)`가 상태를 변경한 뒤 예외를 던지는 등 Netty 계약을 위반하는 subclass는 지원 대상이 아니다.

### 5.4 Lettuce decode

decode는 null이 아닌 Lettuce `ByteBuffer`의 `[position, limit)`를
`source.duplicate().slice().asReadOnlyBuffer().order(source.order())`로 분리하고 serializer의 `deserializeFrom`에 전달한다. 이 view의 capacity 자체가 원본 remaining으로 제한되어 serializer가 `clear()` 또는
`limit(capacity)`를 호출해도 prefix/suffix가 노출되지 않는다. heap source에서도 read-only view는
`hasArray == false`이고 `array()`/`arrayOffset()` 접근과 content mutation을 차단해 backing array를 통한 bounds 우회를 허용하지 않는다.

- 원본 position, limit, mark, byte order를 성공과 실패 모두 보존한다.
- codec 계층은 추가 `ByteArray`를 만들지 않는다.
- serializer default가 copy를 필요로 하면 그 copy는 compatibility fallback으로 남는다.
- untrusted input bound와 deserialization security policy는 기존 serializer가 소유한다.
- view는 호출 동안만 유효한 synchronous borrow다. serializer는 source, 파생 view 또는 stream을 보관하거나 호출 종료 뒤 접근하지 않는다.
- `deserializeFrom`을 override하는 custom serializer도 이 synchronous-borrow 계약을 의무적으로 지킨다. codec은 보관 여부를 runtime에 탐지하거나 자동 fallback하지 않는다. contract를 지킬 수 없는 custom 구현은 override하지 않고 interface allocating default를 사용해야 한다.

## 6. backend capability matrix

| Serializer            | OutputStream 후보           | Lettuce encode 판정 전제                                                | Decode dispatch                                 | 초기 문서 표현   |
|-----------------------|-----------------------------|-------------------------------------------------------------------------|-------------------------------------------------|------------------|
| JDK                   | `ObjectOutputStream` direct | wire parity, filter/config parity, 두 run allocation 통과               | `deserializeFrom`                               | evidence pending |
| Kryo                  | Kryo `Output` direct        | pool/config/security parity, 두 run allocation 통과                     | `deserializeFrom`                               | evidence pending |
| Jackson 2             | generator/output direct     | mapper configuration 및 target lifecycle parity, 두 run allocation 통과 | `deserializeFrom`                               | evidence pending |
| Jackson 3             | generator/output direct     | mapper configuration 및 target lifecycle parity, 두 run allocation 통과 | `deserializeFrom`                               | evidence pending |
| Fory                  | default fallback            | 직접 output capability 없음                                             | `deserializeFrom`                               | ergonomic-only   |
| Fastjson2             | default fallback            | 직접 output capability 없음                                             | backend 조건부                                  | ergonomic-only   |
| compressed serializer | 명시적 allocating override  | 자체 압축 wire 기록, 내부 direct 우회 금지                              | compatibility path                              | ergonomic-only   |
| custom serializer     | default 또는 override       | 구현체 contract와 독립 evidence 필요                                    | override는 borrow contract 의무, 아니면 default | no generic claim |

구현 중 실제 backend API가 기존 configuration, security, wire format을 보존하지 못하면 해당 override를 추가하지 않고 default fallback으로 남긴다. 이 결정은 실패가 아니라 capability matrix의 정상적인 `ineligible` 판정이다.

## 7. 상태와 소유권 계약

### 7.1 성공

- target prefix와 reader state를 보존한다.
- target `writerIndex`만 `written`만큼 증가한다.
- target `maxCapacity`, byte order, allocator ownership을 변경하지 않는다.
- target capacity는 필요한 범위에서 Netty 정책에 따라 증가할 수 있다.
- source decode 상태는 완전히 보존한다.
- target encode 전후 `ByteBuf.refCnt()`는 동일하다.

### 7.2 실패

- thread-confined하고 Netty 계약을 지키는 target에서 codec/serializer가 발생시킨 실패는 target
  `writerIndex`, reader index, mark, `refCnt()`를 원래 값으로 유지한다.
- capacity 증가와 `[start, start + min(requestedHighWater, capacity - start))` 영역의 bytes는 rollback하지 않는다. `requestedHighWater`는 `set*` 호출 전에 갱신한 요청 범위의 상한이다.
- source position, limit, mark, byte order를 보존한다.
- writer index를 commit하지 않으므로 dirty range는 현재 readable range나 Redis 전송 범위에 포함되지 않는다. 같은 target을 재사용하면 후속 payload가 commit한 writer index까지만 readable하다.
- codec은 failed range를 wipe하지 않는다. allocator reuse와 별도 memory sanitization은 Netty/caller의 기존 buffer lifecycle 정책을 따른다.

### 7.3 외부 drift

serializer 호출 중 target index/reference state가 외부에서 변경되면 codec은 commit하지 않고 fail-closed한다. 외부 drift에서는 retain/release 또는 index 복구를 시도하지 않는다. serializer가 정상 반환했으면 stable `IllegalStateException`을 던지고, serializer failure와 drift가 함께 있으면 원 serializer failure를 우선 전파한다. 이 경로는 정상 concurrent usage가 아니며 caller-owned buffer는 호출 동안 thread-confined여야 한다.

## 8. 예외 계약

| 조건                                                   | 결과                                                                                                      |
|--------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| null target                                            | no-op, serializer 미호출                                                                                  |
| interface default의 `serialize(graph)` failure         | 기존 serialize type/cause/identity 그대로 전파                                                            |
| interface default의 stream write failure               | 원 throwable identity 보존                                                                                |
| direct backend의 bounds/capacity/read-only failure     | 기존 backend serialization exception 분류와 cause identity 보존                                           |
| serializer failure                                     | 기존 serializer exception type/cause 계약 보존                                                            |
| direct backend의 `Error`                               | 기존 ByteBuffer direct 경로처럼 raw identity 보존                                                         |
| direct backend의 cancellation                          | 기존 ByteBuffer backend별 type/cause 계약 보존; Jackson 2/3는 기존 `JsonSerializationException` 분류 유지 |
| compressed compatibility의 nested `Error`/cancellation | `BufferFailurePolicy`로 raw identity 복원                                                                 |
| serialized count `Int.MAX_VALUE` 초과                  | stable `IllegalStateException`, direct backend는 기존 wrapper/cause 정책 적용                             |
| 반환량과 실제 기록량 불일치                            | stable message의 cause 없는 `IllegalStateException`                                                       |
| 정상 반환 뒤 index/refCnt drift                        | stable message의 cause 없는 `IllegalStateException`                                                       |
| serializer failure와 drift 동시 발생                   | 원 serializer failure 우선, no commit/no recovery                                                         |
| corrupt/untrusted decode                               | 기존 serializer security/serialization exception 보존                                                     |

adapter는 target overflow를 `BufferOverflowException` 등 다른 계층의 예외로 임의 정규화하지 않는다.

## 9. wire, security, compatibility

### 9.1 wire compatibility

각 optimized 후보는 기존 `serialize(graph)`와 새 `serializeBinaryToStream` 또는
`serializeJsonToStream` 결과가 byte-for-byte 동일해야 한다. Lettuce one-argument encode와 target encode도 동일한 wire를 생성해야 한다.

### 9.2 security

- serializer instance와 configuration을 그대로 재사용한다.
- JDK object filter, Kryo registration/pool policy, Jackson mapper configuration을 우회하지 않는다.
- fallback 허용 범위나 allowed package policy를 넓히지 않는다.
- bounded read-only decode view는 caller가 제공한 `[position, limit)` 밖을 capacity, backing array 또는 mutation API로 노출하지 않는다.
- raw payload, secret, key/value content를 logging하지 않는다.

### 9.3 source 및 ABI

- interface method는 JVM default로 추가한다.
- 기존 `serializeTo(graph, ByteBuffer)`와 `serialize(graph)` signature를 변경하지 않는다.
- 기존 Kotlin/Java `serializeTo(value, null)` compile fixture를 그대로 유지하고 새 stream API 추가 뒤에도 source compatibility를 검증한다.
- `1.11.0` artifact로 compile한 Kotlin/Java caller를 candidate artifact로 실행하는 binary compatibility fixture와, candidate artifact를 대상으로 재compile하는 source compatibility fixture를 모두 검증한다.
- `1.11.0` implementation을 candidate interface와 함께 사용하는 consumer fixture로 JVM default dispatch를 검증한다.
- Java reflection `Method.isDefault`와 실행 fixture로 두 stream API가 실제 JVM default method로 emit됐는지 검증한다.
- `BinarySerializer`와 `JsonSerializer`를 함께 구현한 `1.11.0`/기준-commit dual-interface fixture를 candidate에서 로드·실행·재compile해 default-method diamond가 없음을 증명한다.
- 직전 기준 commit `b00cc5440e47ad803e5aac21528b560fdd3b0474`의 caller/implementation fixture도 별도로 유지해 아직 release되지 않은 ByteBuffer API와의 source/binary compatibility를 검증한다.
- `LettuceBinaryCodec`의 target overload만 extension seam으로 유지한다.
- ordinary one-argument methods와 bridge methods의 final/unsupported extension contract를 바꾸지 않는다.

## 10. 실패 모드와 방어

### 10.1 serializer가 underlying stream을 닫음

- 신호: 후속 write/flush 실패 또는 caller stream close count 증가
- 방어: non-closing wrapper, close-count fixture, codec `finally` seal
- rollback: 해당 backend override 제거, default fallback 유지

### 10.2 write 도중 writerIndex가 이동함

- 신호: injected mid-write failure 뒤 target writer index drift
- 방어: absolute `set*` adapter, Netty `ByteBufOutputStream` 사용 금지
- rollback: Lettuce optimized encode dispatch 비활성화

### 10.3 serializer 반환량과 실제 기록량 불일치

- 신호: count mismatch fixture
- 방어: commit 전 exact equality 검사와 fail-closed exception
- rollback: 잘못된 override만 fallback으로 복귀

### 10.4 allocation은 줄지 않고 adapter overhead만 증가함

- 신호: 두 canonical run 중 하나라도 `gc.alloc.rate.norm` 감소가 5% 미만
- 방어: backend별 accepted/inconclusive/ineligible 판정
- rollback: 구현을 ergonomic-only로 문서화하거나 override 제거

### 10.5 throughput regression

- 신호: matched baseline 대비 20% 이상 감소
- 방어: throughput diagnostic guardrail과 profiler 확인
- rollback: 해당 backend의 direct path 제거

### 10.6 wire 또는 security drift

- 신호: byte parity, old-data decode, filter/registration/mapper fixture 실패
- 방어: direct path보다 compatibility와 security를 우선
- rollback: 해당 override 제거, public default 유지

### 10.7 hostile 또는 concurrent target drift

- 신호: commit 전 reader/writer/refCnt snapshot 불일치
- 방어: stable fail-closed 검사, no commit
- rollback: target encode 전체를 compatibility path로 제한

### 10.8 serializer가 stream/view를 보관함

- 신호: 호출 종료 뒤 retained stream write 또는 retained decode view 접근
- 방어: encode adapter seal, interface synchronous-borrow contract, custom implementor fixture
- rollback: 해당 custom override 제거 후 interface allocating default 사용

### 10.9 운영 rollback과 관측성

- codec은 encode/decode 실패 뒤 runtime 자동 retry 또는 compatibility fallback을 수행하지 않는다. 같은 serializer 호출의 중복 side effect와 부분 output 재사용을 피하기 위해 원 failure를 전파한다.
- 배포 뒤 rollback은 `affected backend 식별 -> consumer를 이전 artifact로 downgrade하거나 direct
  dispatch 제거 patch 배포 -> wire/security/Redis round-trip 회귀 검증` 순서로 수행한다.
- rollback 완료 조건은 해당 backend의 failure 재현이 사라지고 compatibility fixture와 Redis round-trip이 통과하는 것이다.
- 신규 runtime metric/log는 추가하지 않는다. operator는 payload를 기록하지 않고 serializer class, codec configuration, exception type/cause와 library version만으로 direct-path 여부와 failure를 식별한다.

## 11. 테스트 설계

새 Kotlin 테스트는 JUnit 5와 `io.bluetape4k.assertions`를 사용한다. 예외 검증은
`io.bluetape4k.assertions.assertFailsWith`를 사용하며 JUnit `assertThrows`, AssertJ/Kluent,
`kotlin.test.assertFailsWith`, `!!`를 사용하지 않는다.

### 11.1 serializer contract

- default OutputStream fallback wire parity와 exact count
- null/zero-byte policy
- non-closing lifecycle
- direct implementation과 기존 ByteArray wire parity
- default serialize failure, target write failure, direct backend failure, compressed compatibility failure의 type/cause/identity matrix
- partial-write semantics
- `@Throws(IOException::class)` Java compile fixture
- caller stream 비보관 계약과 flush-count/close-count zero
- `Int.MAX_VALUE` count 경계와 stable overflow failure
- compressed compatibility wire parity, wrapped serializer direct method 미호출, nested cancellation/Error 복원
- old/new Java·Kotlin `BinarySerializerDecorator` subclass의 `serialize(graph)` override semantics 보존
- Kotlin/Java old caller source 및 ABI fixture

### 11.2 Lettuce encode

- heap/direct `ByteBuf`
- non-zero writer index와 prefix
- bounded maxCapacity와 target exhaustion
- read-only target
- null target serializer 미호출
- mid-write failure와 writer index/reader index/mark/refCnt 보존
- capacity growth 및 dirty range 문서 계약
- `nioBuffer()`를 throw하는 hostile `ByteBuf`에서도 성공
- 여러 small chunk의 누적 growth, exact maxCapacity 경계, integer overflow 방어
- 일부 bytes를 변경한 뒤 throw하는 hostile bulk-write target과 requested high-water bound
- retained adapter의 호출 종료 뒤 write/flush/close 차단
- serializer count mismatch와 target state drift fail-closed
- serializer failure와 drift 동시 발생 시 원 failure 우선
- release drift에서 retain/release/index recovery 미시도
- 실패 뒤 동일 target에 더 짧은 payload를 기록해 dirty suffix가 readable 범위에 들어오지 않음
- one-argument encode와 target encode의 byte-for-byte wire parity

### 11.3 Lettuce decode

- heap/direct/read-only/sliced `ByteBuffer`
- non-zero position과 bounded limit
- 성공/실패 시 position, limit, mark, byte order 보존
- little-endian order 전달 관찰
- serializer가 view `clear()`/limit 확장을 시도해도 prefix/suffix 비노출
- heap source에서도 derived view의 `hasArray == false`, `array()`/`arrayOffset()` 차단, content mutation 차단
- empty/null input
- corrupt/untrusted input exception parity
- direct backend와 compatibility backend dispatch
- custom override의 synchronous-borrow contract fixture와 default fallback dispatch
- JDK default/custom/global filter, Kryo secure registration과 custom-pool fallback, Jackson 2 polymorphic validator, Jackson 3 malicious `@class` 비활성 정책의 ByteArray/direct parity
- bounded secret sentinel이 exception message와 codec/adapter captured log에 노출되지 않음

### 11.4 기존 기능 회귀

- `estimateSize`의 non-serializing `-1` 정책
- target overload extension seam
- subclass override가 null/commit/ownership 책임을 유지하고 built-in adapter를 자동 상속하지 않는 fixture
- factory serializer 조합
- FastFory compatibility policy
- Redis round-trip smoke test

## 12. allocation evidence

기존 `infra/lettuce` benchmark source set과 `LettuceCodecBenchmark`를 확장한다. 신규 module이나 benchmark dependency를 추가하지 않는다.

### 12.1 측정 조건

- deterministic 동일 payload
- 주 evidence는 pre-sized reusable caller-owned target에서 payload handoff allocation만 격리한다.
- buffer allocation, value construction, buffer clear/reset은 timed method 밖에 둔다.
- initial capacity, current capacity, maxCapacity, writer index, headroom, allocator, pooled 여부를 고정한다.
- 변경 전 target encode의 `serialize(graph) -> ByteArray -> ByteBuf.writeBytes` 경로를 canonical frozen benchmark-only baseline으로 candidate artifact 안에 유지한다.
- caller-owned target encode를 candidate로 사용
- one-argument `ByteBuffer` encode는 secondary control이며 target baseline을 대체하지 않는다.
- exact promotion matrix는 `backend(JDK, Kryo, Jackson 2, Jackson 3) × target-kind(heap, direct) ×
  path(frozen copied baseline, caller-owned candidate)`이다.
- 각 candidate는 동일 serializer/config/payload와 동일 target factory/config의 paired baseline 하나에만 매핑한다. matrix 누락, 중복, 추가 cell은 validator가 실패시킨다.
- capacity growth는 별도 diagnostic cell로 측정하며 promotion 근거에 포함하지 않는다.
- thread 1, fork 2, warmup 3, measurement 5 protocol로 benchmark annotation을 고정한다.
- `gc.alloc.rate.norm`을 primary metric으로 사용
- throughput은 diagnostic metric으로 보존
- 한 번 build한 exact-HEAD pinned JAR로 canonical run 두 번을 수행한다.
- canonical run 전에 독립 Kotlin preflight가 16개 method의 backend/config/payload/target-kind identity, frozen copied baseline과 candidate의 distinct dispatch, wire/count/prefix parity를 실행 검증한다.
- retained backend별 read-only target preflight는 codec-visible exception type/cause와 writerIndex/readerIndex/marks/refCnt 보존을 기존 ByteBuffer backend contract와 비교한다.

### 12.2 판정

- 각 `backend × target-kind` cell은 mapped baseline과 canonical run별로 독립 판정한다.
- allocation delta는 `(baseline - candidate) / baseline × 100`이며 두 run 모두 `>= 5%`여야 한다.
- throughput delta는 `(candidate - baseline) / baseline × 100`이며 어느 run에서든 `<= -20%`이면 promotion을 차단한다.
- `accepted`: 두 run 모두 allocation 기준을 통과하고 throughput 차단 조건이 없음
- `inconclusive`: wire/security parity를 만족하고 두 run 모두 throughput 차단 조건은 없지만 allocation threshold를 두 run 모두 통과하지 못한 direct path
- `ineligible`: compatibility/default fallback, wire/security parity 실패, 또는 어느 run에서든 throughput 차단 조건을 충족해 direct override를 제거한 backend

evidence metadata는 backend별 stream method declaring class와 `declared-direct`/`inherited-default` dispatch를 기록한다. `inherited-default`는 측정 수치와 무관하게 terminal `ineligible`이며 validator가 재분류하지 않는다.

한 cell의 accepted 결과를 다른 serializer, payload, decode, compressed codec에 일반화하지 않는다.

### 12.3 evidence artifact

- environment와 exact commit
- raw JMH JSON
- run별 summary CSV
- two-run comparison CSV
- `backend × target-kind` accepted/inconclusive/ineligible 표
- benchmark command와 limitation
- exact measurement HEAD/JAR hash binding
- expected matrix, candidate-to-baseline 일대일 mapping, run metadata의 fail-closed validator 결과

validator는 `gc.alloc.rate.norm`의 unit이 정확히 `B/op`이고 score/scoreError가 finite·non-negative이며 baseline score가 positive인지 확인한다. throughput primary metric은 mode `thrpt`, unit `ops/ms`, finite positive score와 finite·non-negative scoreError를 요구한다. NaN, Infinity, 음수, 잘못된 unit은 evidence를 거부한다.

manifest는 OS/kernel, CPU model과 logical core count, JDK vendor/version, JVM options, Gradle/JMH 전체 명령, allocator/pooled 설정, payload hash, warmup/measurement/fork/thread 설정과 실행 시각을 포함한다. build 직전 clean `git status`, `HEAD` commit, `HEAD^{tree}` source tree hash와 built JAR SHA-256도 기록한다. staged/unstaged file 또는 untracked build input이 있거나 필수 field가 없으면 validator가 실패한다.

raw evidence는 `docs/benchmarks/raw/issue-756/` 아래 `canonical-a/`, `canonical-b/`, `comparison.csv`,
`delivery-manifest.json`, `validation.json`으로 저장하고 PR에 커밋한다. 각 canonical directory는
`jmh.json`, `summary.csv`, `argv.json`, `environment.json`, `metadata.json`, `validation.json`을 포함한다. 대용량 CI-only artifact를 두지 않으므로 외부 retention에 의존하지 않는다.

benchmark input SHA와 final delivery SHA는 구분한다. measurement artifact를 커밋하므로 두 SHA를 같게 만들지 않는다.

- `benchmark input SHA`는 clean source에서 pinned JAR를 build하고 두 canonical run을 수행한 commit이다.
- `final delivery SHA`는 `local HEAD = remote branch HEAD = PR head = CI tested SHA`를 만족한다.
- validator는 먼저
  `git merge-base --is-ancestor <benchmark-input-sha> <final-delivery-sha>`를 통과시킨다.
- validator는 `benchmark input SHA..final delivery SHA`의 추가·수정·삭제·rename을 다음 exact allowlist로만 제한하고 그 밖의 path가 하나라도 있으면 실패한다.
    - `docs/benchmarks/raw/issue-756/**`
    - `docs/benchmarks/2026-07-22-issue-756-lettuce-buffer-codec-allocation.md`
    - `io/io/README.md`, `io/io/README.ko.md`
    - `io/json/README.md`, `io/json/README.ko.md`
    - `io/jackson2/README.md`, `io/jackson2/README.ko.md`
    - `io/jackson3/README.md`, `io/jackson3/README.ko.md`
    - `infra/lettuce/README.md`, `infra/lettuce/README.ko.md`
- Kotlin production/test/KDoc, Gradle, benchmark source/script 또는 validator 입력이 measurement 뒤 변경되면 evidence를 무효화하고 clean pinned JAR build, canonical run 두 번, validator를 다시 수행한다.
- final delivery SHA 검사는 CI runtime output으로 남긴다. final SHA를 committed manifest에 다시 써서 자기참조 commit을 만들지 않는다.

## 13. 문서

- 공개 KDoc은 English로 작성한다.
- `io/io/README.md`, `io/io/README.ko.md`, `io/json/README.md`, `io/json/README.ko.md`를 동등하게 갱신한다.
- `io/jackson2/README.md`, `io/jackson2/README.ko.md`, `io/jackson3/README.md`,
  `io/jackson3/README.ko.md`를 동등하게 갱신한다.
- `infra/lettuce/README.md`와 `README.ko.md`를 동등하게 갱신한다.
- serializer API 문서에는 Kotlin/Java direct-call 예제, allocating default, caller ownership, partial failure,
  `Int.MAX_VALUE` count bound를 명시한다. Java 예제는 checked `IOException` catch/declare와 caller-owned stream의 close/flush 책임을 보이고, 두 예제 모두 실패한 partial destination 폐기 또는 staging을 설명한다.
- Lettuce 문서에는 commit-on-success, dirty attempted range, no retain/release를 명시한다.
- failed stream 결과는 유효 payload로 재사용하지 않고 destination을 reset할 수 없으면 폐기한다. transactional write가 필요하면 caller가 별도 staging을 제공해야 한다.
- failed ByteBuf는 잠재적으로 오염된 buffer로 취급한다. full-capacity dump/log를 금지하고, 보안 정책상 sanitization이 필요하면 재사용하지 말고 안전한 caller/allocator 폐기 정책을 적용한다.
  `release()`는 wipe를 보장하지 않는다.
- codec/adapter가 만든 exception과 log는 key/value, payload bytes, target content를 포함하지 않는다.
- 기존 serializer가 생성한 exception message/cause는 codec 밖의 기존 신뢰 경계로 그대로 전파한다.
- capability matrix는 measured verdict만 사용한다.
- allocation claim에는 exact measured payload/config, allocator/pooled 여부, pre-sized reusable target과 no-growth 조건을 함께 적고 그 밖의 payload/capacity/pooling에 일반화하지 않는다.
- throughput 또는 zero-copy 개선을 약속하지 않는다.
- Redisson 및 compression low-allocation backend optimization만 후속 slice로 명시한다.
- built-in serializer와 read-only/non-array-backed synchronous view 계약을 이미 지키는 custom override를 사용하는 기존 Lettuce factory caller는 설정·데이터 rewrite 없이 새 dispatch를 사용한다. `array()` 또는 content mutation이 필요한 custom override는 interface allocating default를 사용하도록 수정해야 한다. serializer 직접 caller의 `serializeBinaryToStream`/`serializeJsonToStream` 사용만 opt-in이며 fallback/inconclusive backend에는 allocation 개선을 보장하지 않는다.

## 14. 검토한 대안

### 14.1 writable NIO view를 serializer에 전달

거부한다. caller-owned `ByteBuf`가 반환하는 view의 alias/bound 동작과 실패 후 commit 의미를 codec이 충분히 통제하기 어렵고, issue #757에서 확립한 absolute writer 안전 경계를 약화한다.

### 14.2 Netty `ByteBufOutputStream` 사용

거부한다. resolved source에서 write마다 원본 `writerIndex`를 증가시켜 failure atomicity를 깨뜨린다.

### 14.3 serializer별 Lettuce codec subclass

거부한다. JDK/Kryo/Jackson마다 codec을 분기하면 fallback, index, ownership, failure contract가 중복된다. serializer가 output capability를 소유하고 Lettuce adapter가 target ownership을 소유한다.

### 14.4 Redisson까지 한 PR에 포함

거부한다. Redisson은 fallback decode, `ByteBuf` release, wrapped/composite ownership, compressor backend dependency를 함께 다뤄야 하므로 독립 failure surface다.

### 14.5 #755 완료 전 compressed codec 최적화

거부한다. compatibility default만으로는 payload-sized intermediate 제거를 증명할 수 없다.

## 15. staged delivery

1. 이 명세를 승인·커밋한다.
2. 구현 계획을 별도로 작성하고 승인·커밋한다.
3. serializer binary/json stream contract와 compatibility proof를 test-first로 전달한다.
4. JDK/Kryo/Jackson 2/Jackson 3 direct 후보를 backend별로 검증한다.
5. Lettuce bounded absolute writer와 decode dispatch를 전달한다.
6. module regression, allocation evidence, bilingual docs를 수렴한다.
7. 독립 code review와 exact-head CI 뒤 merge-ready에서 fresh approval을 기다린다.

`CompressableBinarySerializer.serializeBinaryToStream`의 allocating compatibility override는 이번 slice에 포함한다. Redisson Fory/FastFory와 compression wrapper의 저할당 backend 최적화만 #756의 후속 명세/계획으로 다룬다.

## 16. 수용 기준

1. 기존 serializer와 Lettuce caller는 source/binary compatible하다.
2. 기존 `ByteArray`, one-argument encode, target encode의 wire가 동일하다.
3. supported direct 후보는 caller-owned output에 기록하고 `Int.MAX_VALUE` 이하 exact byte count를 반환한다.
4. Lettuce target encode는 성공 시에만 writer index를 commit한다.
5. built-in codec의 정상 thread-confined target 실패 시 index/reference/source 상태와 기존 예외 type/cause 계약을 보존한다.
6. Lettuce decode는 codec 계층의 unconditional `getAllBytes()`를 제거한다.
7. security, registration, filter, mapper configuration은 기존 contract를 유지한다.
8. heap/direct/bounded/hostile target과 source matrix가 통과한다.
9. allocation 개선 문구는 two-run threshold를 통과한 backend에만 사용한다.
10. README locale, KDoc, benchmark artifact, compatibility evidence가 source와 일치한다.

## 17. DoD

- [ ] 승인된 명세와 계획이 branch에 커밋됨
- [ ] serializer binary/json stream contract와 compatibility tests 통과
- [ ] Lettuce target/source ownership 및 failure matrix 통과
- [ ] affected module tests와 Detekt 통과
- [ ] `git diff --check` 통과
- [ ] Kotlin checklist `P0=0`, `P1=0`
- [ ] spec/plan/code review 6관점과 main integration 수렴
- [ ] two-run allocation evidence와 fail-closed validator 통과
- [ ] `io/io`, `io/json`, `io/jackson2`, `io/jackson3`, `infra/lettuce` English/Korean README parity와 public KDoc 검증
- [ ] clean benchmark input SHA/tree/JAR hash와 two-run evidence binding 통과
- [ ] benchmark input SHA 이후 변경이 allowlisted evidence/docs path뿐임
- [ ] `local HEAD = remote branch HEAD = PR head = CI tested SHA`
- [ ] exact PR head의 CI/review/thread gate 통과
- [ ] fresh merge approval 대기

## 18. 승인 기록

사용자는 다음을 순차 승인했다.

- Lettuce-first staged delivery
- serializer OutputStream capability와 bounded absolute-index ByteBuf writer
- commit-on-success 및 source-state preservation 실패 계약
- strict Kotlin tests, two-run allocation evidence, bilingual documentation

written spec 승인 전에는 implementation plan과 production/test code를 작성하지 않는다.
