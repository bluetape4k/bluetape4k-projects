# Issue #757 Lettuce Protobuf Buffer Integration Design

- Issue: [#757 Add ByteBuffer-oriented Protobuf serializer and codec APIs](https://github.com/bluetape4k/bluetape4k-projects/issues/757)
- Parent delivery tracker: [#898 Epic: 1.12.0 delivery tracking](https://github.com/bluetape4k/bluetape4k-projects/issues/898)
- Related Redis umbrella: [#756 Reduce Redis codec allocation with ByteBuffer and ByteBuf paths](https://github.com/bluetape4k/bluetape4k-projects/issues/756)
- Milestone: `1.12.0`
- Branch: `feat/issue-757-lettuce-protobuf-buffer`
- Baseline authority: `origin/develop@4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88`
- Baseline tree authority: `086f83baa7eec0cd68e68fff132542ef6db0f200`
- Candidate measurement authority: immutable clean detached measurement-source commit/tree
- Retained final delivery authority: externally resolved exact head commit/tree after commit; it must descend from the measurement source without production/build/test/benchmark input drift
- Rejected rollback authority: immutable clean rollback-source commit/tree created after candidate measurement, descended from that measurement source, and bound to the approved rollback contract
- Rejected final delivery authority: externally resolved exact head commit/tree descended from the rollback source without subsequent production/build/test/benchmark input drift
- Dependency authority: resolved `io.lettuce:lettuce-core:7.6.0.RELEASE`, Netty
  `4.2.16.Final` source, and the repository's current codec contracts
- Primary decision metric: normalized allocation, `gc.alloc.rate.norm` in B/op
- Delivery stop: exact-head pull request is merge-ready; merge requires fresh approval

## 1. 문제와 목표

#757의 core, serializer, Redisson, benchmark evidence 작업은 병합됐지만 Lettuce integration은 후속 PR로 명시적으로 남아 있다. 현재
`LettuceProtobufCodecs.protobuf()`와 `trustedInternalProtobuf()`는 일반
`LettuceBinaryCodec`을 반환하며, Lettuce가 제공한 `ByteBuf`에 값을 쓸 때도 다음 경로를 사용한다.

```text
ProtoMessage
  -> ProtobufSerializer.serialize(value)
  -> payload-sized ByteArray
  -> ByteBuf.writeBytes(byteArray)
```

Lettuce 7.6.0의 `CommandArgs.encode`는 `ToByteBufEncoder.isEstimateExact()`가 false인 codec에 temporary `ByteBuf`를 만들고 codec이 그 buffer에 직접 쓰도록 한다. 현재
`LettuceBinaryCodec.estimateSize(customValue)`는 `-1`이므로 이 경로에서 직렬화가 두 번 실행되지는 않지만, Protobuf payload 전체 크기의 최종 `ByteArray` handoff는 남아 있다.

이 작업의 목표는 strict 및 trusted-internal Protobuf value의 Lettuce
`ToByteBufEncoder` 성공 경로에서 그 최종 `ByteArray` handoff를 제거하고, 기존 factory signature, wire bytes, 보안 경계, fallback, buffer ownership을 유지하는 것이다. Protobuf `Any.pack`과 `ByteString` 내부 allocation까지 제거하는 zero-copy는 목표가 아니다. 개선 주장은 두 fresh JMH GC-profiler run에서 재현된 normalized allocation 감소에만 허용한다.

## 2. 권한과 범위

live issue #757, 병합된 core 설계와 구현, 현재 `develop`, Lettuce 7.6.0 source, #1039의 allocation evidence 규칙이 권위다. 이 설계는 release, tag, publish, repository settings, credential 작업을 승인하지 않는다.

### 포함

- uncompressed strict `LettuceProtobufCodecs.protobuf()`의 Protobuf-message
  `ToByteBufEncoder.encodeValue(value, target)` 경로.
- uncompressed `trustedInternalProtobuf()`의 Protobuf-message direct path와 기존 non-Protobuf fallback compatibility.
- 기존 factory의 source/binary return type 및 호출 형태 보존.
- heap/direct/composite `ByteBuf`, non-zero writer index, capacity expansion, failure, fallback, wire compatibility, reference-count 테스트.
- 기존 `benchmark/protobuf-codec-benchmark`의 Lettuce copied baseline/direct-write candidate matrix, fail-closed validator, 두 fresh allocation run과 report.
- 관련 KDoc, locale-paired README/manual, `CHANGELOG.md`, issue DoD 갱신.

### 제외

- `LettuceBinaryCodec` 전체 serializer family의 buffer SPI 일반화. #756 범위다.
- gzip, deflate, LZ4, Snappy, Zstd Protobuf codec. 압축 전에 완성된 byte sequence가 필요하므로 #755/#756에서 별도로 측정하고 설계한다.
- `encodeValue(value): ByteBuffer` compatibility method 최적화.
- Lettuce `decodeValue(ByteBuffer)` 저복사 변경. 병합된 #757 core에서 direct Protobuf decode가 allocation을 악화시켜 compatibility-copy로 되돌린 측정 결과를 따른다.
- key encode/decode의 source contract, descriptor, 구현 semantics, `estimateSize`, Redis command framing, connection lifecycle 변경. 단, class `ACC_FINAL` 제거가 raw
  `0x1041`인 정확히 세 key bridge의 effective overrideability를 활성화하는 JVM-level 비용은 5.4의 ABI 허용 목록으로 별도 관리한다.
- 새 serializer SPI, 새 Gradle module, 새 external/production dependency, public factory name 변경. 기존 `:bluetape4k-lettuce`에 대한 benchmark-only project dependency는 Lettuce comparison cell을 위해 허용한다.
- throughput 또는 zero-copy 보장.

## 3. 검토한 접근

### 3.1 선택: 내부 Protobuf subtype으로 `ToByteBufEncoder`만 특화

`LettuceBinaryCodec`을 상속 가능하게 만들고 `LettuceProtobufCodecs` 안에 private nested subtype을 둔다. subtype은 nullable target을 받는 정확한
`encodeValue(value, target: ByteBuf?)` overload만 재정의한다. strict 및 trusted uncompressed factory는 선언된 반환형 `LettuceBinaryCodec<V>`를 그대로 유지하면서 그 subtype instance를 만든다. subtype constructor와 packed writer seam은 private라서 새 외부 생성 또는 확장 surface가 되지 않는다.

이 접근은 기존 public factory의 source/binary descriptor, 한 인자 constructor, 일반 codec의 동작을 보존한다. Protobuf 지식은 `io/protobuf` module에 남고
`infra/lettuce`가 Protobuf dependency를 얻지 않는다. specialization은 packed
`Any`를 exact-size bounded absolute-index `ByteBuf` output에 기록한다. Protobuf의
`CodedOutputStream(OutputStream, 0)`은 구현상 최소 20-byte internal buffer를 만들지만, payload-sized `ByteArray`는 만들지 않는다. 따라서 이 설계는 allocation-free나 zero-copy가 아니라 payload-sized handoff 제거를 측정하는 설계다.

`LettuceBinaryCodec`을 `open`으로 바꾸면 상속 가능성이 public ABI에 추가되고 이후 외부 subclass compatibility를 유지해야 하는 장기 비용이 생긴다. 이 비용은 measured Lettuce specialization을 기존 factory 반환형 안에서 제공하기 위해 의도적으로 수용한다. 의도한 source-level extension seam은 오직
`LettuceBinaryCodec.encodeValue(V, ByteBuf?)` 하나다. public one-argument constructor, `serializer` property, key encode/decode, single-argument value encode, decode, estimate method는 Kotlin source에서 계속 `final`이며 descriptor와 access를 유지한다.

immutable authority와 candidate를 같은 toolchain으로 재빌드한 `javap -v` evidence의 exact raw flag transition은 다음과 같다.

| Class/member key                                            | Baseline flags                                 | Retained candidate flags                       | 의미                                                            |
|-------------------------------------------------------------|------------------------------------------------|------------------------------------------------|-----------------------------------------------------------------|
| class `LettuceBinaryCodec`                                  | `0x0031 ACC_PUBLIC, ACC_FINAL, ACC_SUPER`      | `0x0021 ACC_PUBLIC, ACC_SUPER`                 | class-level `ACC_FINAL` 제거                                    |
| `encodeKey(Ljava/lang/String;)Ljava/nio/ByteBuffer;`        | `0x0001 ACC_PUBLIC`                            | `0x0011 ACC_PUBLIC, ACC_FINAL`                 | source key method를 명시적으로 final 고정                       |
| `encodeKey(Ljava/lang/String;Lio/netty/buffer/ByteBuf;)V`   | `0x0001 ACC_PUBLIC`                            | `0x0011 ACC_PUBLIC, ACC_FINAL`                 | source key method를 명시적으로 final 고정                       |
| `encodeValue(Ljava/lang/Object;)Ljava/nio/ByteBuffer;`      | `0x0001 ACC_PUBLIC`                            | `0x0011 ACC_PUBLIC, ACC_FINAL`                 | compatibility method를 명시적으로 final 고정                    |
| `decodeKey(Ljava/nio/ByteBuffer;)Ljava/lang/String;`        | `0x0001 ACC_PUBLIC`                            | `0x0011 ACC_PUBLIC, ACC_FINAL`                 | source key method를 명시적으로 final 고정                       |
| `decodeValue(Ljava/nio/ByteBuffer;)Ljava/lang/Object;`      | `0x0001 ACC_PUBLIC`                            | `0x0011 ACC_PUBLIC, ACC_FINAL`                 | compatibility method를 명시적으로 final 고정                    |
| `estimateSize(Ljava/lang/Object;)I`                         | `0x0001 ACC_PUBLIC`                            | `0x0011 ACC_PUBLIC, ACC_FINAL`                 | estimate contract를 명시적으로 final 고정                       |
| `toString()Ljava/lang/String;`                              | `0x0001 ACC_PUBLIC`                            | `0x0011 ACC_PUBLIC, ACC_FINAL`                 | representation contract를 명시적으로 final 고정                 |
| `encodeValue(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V` | `0x0001 ACC_PUBLIC`                            | `0x0001 ACC_PUBLIC`                            | raw flag는 그대로지만 class open으로 source seam 활성화         |
| `encodeKey(Ljava/lang/Object;)Ljava/nio/ByteBuffer;`        | `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` | `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` | raw flag는 그대로지만 effective bytecode overrideability 활성화 |
| `encodeKey(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V`   | `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` | `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` | raw flag는 그대로지만 effective bytecode overrideability 활성화 |
| `decodeKey(Ljava/nio/ByteBuffer;)Ljava/lang/Object;`        | `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` | `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC` | raw flag는 그대로지만 effective bytecode overrideability 활성화 |

baseline final class에서는 raw `ACC_FINAL`이 없는 target과 bridge도 override할 수 없다. candidate에서는 class-level `ACC_FINAL` 제거가 target과 세 bridge를 override 가능하게 만든다. 일반 source method 일곱 개는 raw `0x0001 -> 0x0011`로 명시적으로 잠가 의도한 source-level seam을 target overload 하나로 제한한다. 세 bridge는 일반 Kotlin/Java source API나 의도한 extension seam이 아니지만 bytecode를 직접 생성하는 JVM consumer는 candidate에서 기술적으로 override할 수 있으므로 material long-term ABI cost로 수용한다. bridge 자체에서 `ACC_FINAL`이 제거됐다고 설명하거나 raw flag 변화로 normalize해서는 안 된다. 위 transition 외 모든 class/member key, descriptor, access와 numeric flags는 exact-equal이어야 한다.

ABI proof는 baseline/candidate의 paired `javap -p -s` structural comparison과 paired
`javap -p -v` flag comparison을 함께 사용한다. validator는 wrong class와 baseline 또는 candidate input 누락을 fail-closed로 거부하고, public constructor가 정확히
`(Lio/bluetape4k/io/serializer/BinarySerializer;)V` 하나인지와 target method가 정확히
`encodeValue(Ljava/lang/Object;Lio/netty/buffer/ByteBuf;)V`인지 검증한다. 또한 허용된 위 class, ordinary source method 일곱 개, unchanged target과 세 unchanged bridge의 exact numeric transition을 모두 검증한다. bridge에서 `ACC_PUBLIC`, `ACC_BRIDGE` 또는
`ACC_SYNTHETIC`이 빠지거나 flag가 달라진 경우, 추가로 바뀐 method와 descriptor/access drift는 모두 실패다. ordinary Kotlin/Java compile fixture와 Java reflection 검토는 계속 source compatibility와 private subtype 경계를 증명한다. 해당 overload KDoc에는 null no-op, index commit, ownership, failure propagation을 포함한 public override contract와 subclass compatibility 부담을 명시한다. `LettuceBinaryCodec` class KDoc에도 지원되는 source extension point가 이 overload 하나뿐이고 나머지 source method는 final이며, 세 compiler bridge는 source extension API가 아니라 class 개방에 따라 생기는 JVM-level compatibility cost라는 점을 함께 기록한다.

### 3.2 기각: 모든 `LettuceBinaryCodec`에서 `BinarySerializer.serializeTo` 사용

코드는 작지만 Kryo, Java, compression serializer를 포함한 전체 Lettuce family의 error, sizing, direct-buffer 특성을 바꾼다. 이는 #757의 Protobuf 후속 범위를 넘어 #756의 공통 SPI 결정과 benchmark를 선점한다.

### 3.3 기각: 새 opt-in public factory 또는 새 public codec type

기존 호출자는 계속 allocating path를 사용하므로 #757의 남은 integration을 완료하지 못한다. public API surface와 migration 부담도 불필요하게 늘어난다.

### 3.4 기각: `ProtoMessage.serializedSize`를 `estimateSize`로 반환

Lettuce의 exact estimate contract는 실제 Redis wire에 쓰는 Protobuf `Any` envelope 크기와 일치해야 한다. message 자체 크기는 envelope의 type URL과 field framing을 포함하지 않으며, exact size를 얻기 위해 미리 `Any.pack`하면 command encode에서 다시 pack되어 작업과 allocation이 중복된다. 현재 `-1` 동적 확장 계약을 유지한다.

### 3.5 기각: `ByteBuf.nioBuffer()`에 직접 기록

Netty 4.2의 `nioBuffer(index, length)` 계약은 원본과 공유하거나 복사된 content를 반환할 수 있다. `nioBufferCount() == 1` 또는 `isContiguous()`만으로 arbitrary custom buffer의 aliasing을 증명할 수 없고, detached view에 쓴 뒤 원본 `writerIndex`만 전진시키면 wire corruption과 stale-data 노출이 생긴다. 구체 구현 class allowlist나 runtime alias probe도 취약하고 hot-path 비용이 크므로 사용하지 않는다. absolute
`ByteBuf.setByte/setBytes` writer는 원본 storage에 쓰는 Netty 계약을 직접 사용한다.

### 3.6 기각: `LettuceBinaryCodec`을 `final`로 유지

class를 `final`로 유지하면서 기존 factory가 specialization을 반환하려면
`infra/lettuce`에 Protobuf 지식을 역방향으로 넣거나, 두 module 사이에 새 public strategy seam을 추가하거나, factory return type/API를 바꿔야 한다. 첫 선택은 module boundary를 깨고, 나머지는 이번에 수용한 제한된 상속 ABI보다 더 넓은 public contract와 migration 부담을 만든다. 따라서 source-level seam 하나와 명시된 compiler bridge 비용을 정확히 검증하는 현재 안을 선택한다.

### 3.7 기각: `LettuceBinaryCodec`을 Java로 변환

Java로 다시 작성하면 bridge generation/flags를 더 세밀하게 제어할 수 있지만 Kotlin metadata,
`Companion`, property/getter, nullability annotation과 Kotlin/Java source 호출 형태가 광범위하게 달라진다. 이는 key bridge 세 개의 장기 비용을 피하려고 훨씬 큰 source/binary compatibility surface를 흔드는 변경이므로 기각한다.

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
3. 시작 `writerIndex`와 `refCnt`를 읽고 `ProtoAny.pack(value)`를 정확히 한 번 수행한다. pack 자체가 실패하면 기존 `AbstractBinarySerializer.serialize`와 동일한
   `BinarySerializationException("Fail to serialize. graphType=...")` type/message/cause contract로 변환한다.
4. packed `Any` exact size를 계산하고 `ensureWritable(size)`로 capacity를 확보한다.
5. private bounded writer가 `[start, start + size)`에 absolute write하고 exact count를 확인한다.
6. 모든 write와 flush가 성공한 경우에만 `writerIndex(start + size)`를 한 번 호출한다.

heap, direct, sliced/wrapped, zero/one/multi-component composite target은 동일한 absolute writer 계약을 사용한다. NIO capability probe나 broad catch/fallback은 없다. strict non-Protobuf value는 superclass를 거쳐 기존 `ProtobufSerializer` 예외로 실패하고, trusted non-Protobuf value는 기존 fallback bytes를 유지한다. codec은 target을 교체하거나 retain/release하지 않는다.

## 5. API와 compatibility 계약

### 5.1 유지되는 public surface

다음 호출과 JVM descriptor는 바뀌지 않는다.

```kotlin
fun <V: Any> LettuceProtobufCodecs.protobuf(): LettuceBinaryCodec<V>
fun <V: Any> LettuceProtobufCodecs.trustedInternalProtobuf(): LettuceBinaryCodec<V>

LettuceBinaryCodec(serializer: BinarySerializer)
```

현재 `encodeValue(value, target: ByteBuf?)`의 nullable Kotlin signature와 null immediate no-op도 source/behavior compatibility surface다. subtype은 null 확인을 첫 연산으로 유지하며 Java reflection 호출에서도 같은 동작을 보장한다.

`gzipProtobuf`, `deflateProtobuf`, `lz4Protobuf`, `snappyProtobuf`, `zstdProtobuf`와 trusted compressed factory는 계속 일반 `LettuceBinaryCodec`을 만든다. factory 반환 객체의 concrete class name이나 `javaClass`는 기존 API 계약이 아니며 문서에서 의존하도록 안내하지 않는다. `serializer` property와 `toString()`의 의미는 유지한다.

### 5.2 `ByteBuffer` compatibility method

Lettuce `RedisCodec.encodeValue(value): ByteBuffer`는 기존
`ByteBuffer.wrap(serializer.serialize(value))` 동작을 유지한다. Lettuce 7.6 source가 이 method를 compatibility surface로 정의하고 direct `ByteBuf` 쓰기를
`ToByteBufEncoder`에 별도로 제공하므로, 이번 allocation 주장은 target을 받는 overload에만 적용한다.

### 5.3 wire 및 security

- optimized bytes는 기존 `packMessage(value)` 및 `ProtobufSerializer.serialize`
  결과와 byte-for-byte 동일해야 한다.
- strict factory는 non-Protobuf value를 계속 거부한다.
- trusted factory는 `ProtoMessage`만 optimized path로 보내고 다른 value는 기존 Kryo fallback serialization을 그대로 사용한다. 이 fallback은 trusted-input-only이며 untrusted Redis payload나 일반 application boundary에 대한 안전한 decode 경로로 문서화하지 않는다.
- `Any.typeUrl`, allowlist, class-loading, trusted decode 정책은 변경하지 않는다.
- zero-argument factory의 기본 decode allowlist는 계속 `io.bluetape4k.*`와
  `com.google.protobuf.*`다. 이 범위 밖 application message는 encode할 수 있어도 기본 factory로 decode할 수 없다.
- custom `allowedClassPrefixes`로 직접 만든 `ProtobufSerializer`는 일반
  `LettuceBinaryCodec` compatibility path를 사용하며 이번 optimized subtype 대상이 아니다. 새 allowlist factory overload는 #757 범위를 넓히므로 추가하지 않는다.
- encoded Redis value는 이전 codec과 양방향 호환되어야 한다.

### 5.4 JVM ABI 비용과 허용 경계

retained terminal의 ABI allowlist는 3.1의 exact raw transition으로 닫혀 있다. class의
`ACC_FINAL` 제거가 target source seam 하나와 raw `0x1041` bridge 세 개의 effective overrideability를 동시에 활성화한다. bridge는 source API가 아니지만 bytecode consumer 관점에서는 override 가능한 member가 되므로 장기 호환 비용에 포함한다.

| Risk                                   | 허용 범위                                               | Fail-closed proof                                                      | 장기 처리                                                    |
|----------------------------------------|---------------------------------------------------------|------------------------------------------------------------------------|--------------------------------------------------------------|
| source subclass surface 확대           | class `0x0031 -> 0x0021`; target raw `0x0001 -> 0x0001` | paired structural/verbose `javap`, Kotlin/Java fixture                 | target override contract를 public KDoc로 유지                |
| 다른 ordinary source method 보호       | 3.1의 일곱 method가 각각 `0x0001 -> 0x0011`             | exact key/flag transition 외 변화 거부                                 | target 외 source override를 final로 차단                     |
| compiler bridge overrideability 활성화 | 세 bridge raw `0x1041 -> 0x1041`; class finality만 변경 | bridge/synthetic/public flag 또는 extra change 거부                    | bytecode consumer override 가능성을 material ABI cost로 추적 |
| constructor/property/다른 method drift | 없음                                                    | exact constructor/target invariant와 전체 normalized member comparison | drift가 있으면 delivery 중단                                 |
| rejected terminal이 ABI 흔적을 남김    | 없음                                                    | raw member final flags를 포함한 exact normalized baseline equality     | subtype과 모든 `open` 변화를 제거                            |

일반 Kotlin/Java caller compile fixture는 source compatibility를 계속 증명하지만 bridge flags 자체의 증거를 대신하지 않는다. 반대로 `javap` validator는 source overload resolution을 대신하지 않으므로 두 검증은 모두 필요하다.

### 5.5 Immutable ABI manifest와 toolchain provenance

ABI validator는 임의의 `javap` text를 직접 받지 않고 validator-consumed immutable ABI payload manifest를 단일 입력 권한으로 사용한다. JMH run payload도 같은 source-identity 모델을 사용한다. payload manifest는 final delivery commit을 가리키지 않고 실제 class/JAR 생성과 JMH 측정을 수행한 immutable clean detached measurement-source commit/tree를 고정한다.

baseline과 candidate는 서로 다른 새 detached checkout과 canonical build root를 사용한다. baseline role은 commit `4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88`과 resolved tree
`086f83baa7eec0cd68e68fff132542ef6db0f200`에 exact-equal이어야 한다. candidate role은 측정 직전에 고정한 measurement-source commit/tree에 exact-equal이어야 한다. 각 checkout은 자체 `git rev-parse HEAD`와 `HEAD^{tree}`로 독립 검증한다. branch name, ancestor, caller가 전달한 SHA 또는 source-content 유사성은 authority가 아니다.

build 전에는 checkout의 tracked/non-ignored untracked 상태가 clean이고 build root가 새로 생성된 empty sibling인지 확인한다. pre-existing ignored build output, symlink escape, shared classes directory와 다른 checkout output reuse를 금지한다. build/측정 후에도 checkout의 tracked/non-ignored state가 clean이어야 하며, 모든 output은 sealed build/evidence root 안에만 있어야 한다. dirty tracked file, non-ignored untracked input, stale ignored class/JAR 또는 checkout 밖에서 주입된 build input을 발견하면 fail-closed다.

payload manifest는 schema version, validation mode, role, exact commit/tree, clean-state receipts, canonical checkout/build root, FQCN
`io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec`와 다음 provenance를 기록한다.

- standalone classfile path/SHA-256, containing JAR path/SHA-256와 exact JAR entry
  `io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.class`.
- structural/verbose raw output path와 SHA-256, generation 전후 exact classfile SHA-256.
- Kotlin compiler version과 compiler binary/distribution artifact SHA-256, Gradle wrapper version/distribution SHA-256, Java toolchain/JDK vendor·version·canonical `JAVA_HOME`, exact `JAVA_HOME/bin/java` 및 `JAVA_HOME/bin/javap` path와 binary SHA-256.
- 실행 command, UTC timestamp, OS/architecture, validator version/hash와 output archive relative path.

validator는 JAR에서 exact entry를 직접 추출해 SHA-256이 standalone classfile과 같은지 확인하고, `javap`가 그 동일 classfile 또는 검증된 JAR entry를 읽었음을 증명한다. missing, duplicate 또는 다른-path entry, JAR-entry/classfile hash mismatch, generation 사이 classfile 변경은 실패다.

두 build는 같은 Kotlin compiler, Gradle wrapper/toolchain, JDK와 정확히 같은
`$JAVA_HOME/bin/javap` binary를 사용해야 한다. version 문자열만 같고 binary hash가 다르거나 toolchain field가 하나라도 누락/불일치하면 fail-closed다. toolchain drift를 발견하면 새 compiler output을 기존 allowlist로 normalize하지 않고 spec과 plan을 다시 승인받은 뒤 baseline을 재설정한다.

structural normalization은 UTF-8/line ending/비의미적 whitespace와 member order만 canonicalize하고, FQCN, class kind, member kind/name/descriptor, constructor set, access/finality modifier를 보존한다. verbose normalization은 constant-pool index, code, line-number 같은 비-ABI body를 제외하고 class/member key, descriptor와 numeric access flag bitmask 전체를 보존한다. `ACC_PUBLIC`, `ACC_FINAL`, `ACC_BRIDGE`, `ACC_SYNTHETIC`을 삭제하거나 동치 처리하지 않는다. raw output은 normalize하기 전 byte stream 그대로 hash/archive한다.

retained mode는 위 baseline authority와 candidate measurement-source를 서로 다른 role로 고정하고 3.1의 class `0x0031 -> 0x0021`, ordinary source method 일곱 개
`0x0001 -> 0x0011`, unchanged target `0x0001 -> 0x0001`, unchanged bridge 세 개
`0x1041 -> 0x1041`만 허용한다. 나머지 member는 exact-equal이다. rejected mode는 class와 모든 member의 descriptor, access 및 raw numeric flag bitmask가 immutable authority baseline과 정확히 같아야 한다.

hash relation은 cycle 없는 DAG다. payload manifest는 classfile, JAR와 raw outputs만 hash한다. validator result는 payload-manifest SHA-256을 참조한다. tracked root index는 payload/result SHA-256과 archive generation을 참조하지만 자기 자신의 hash나 아직 없는 final commit/tree를 포함하지 않는다. Git tree가 tracked root를 bind한다. classfile, JAR, raw outputs, payload manifest, validator result와 root index는 immutable regular file로 보존하며 cleanup 가능한 backup으로 강등하지 않는다.

rejected terminal의 canonical approved rollback diff contract는 rollback 적용 전에 내용과 SHA-256이 승인된 immutable regular file이며 tracked root index가 그 SHA-256을 참조한다. contract 자신은 자기 hash를 담지 않는다. contract는 candidate measurement-source commit/tree와 governed implementation/test path의 exact pre/post blob/deletion set을 bind하지만 아직 없는 rollback-source commit/tree나 final head를 기록하지 않는다. contract artifact와 그 approval receipt의 docs/evidence path/hash는 root index가 별도로 exact 열거하며 production rollback allowlist로 확장되지 않는다. external verifier가 이미 존재하는 rollback-source commit/tree를 독립 resolve해 contract와 대조하고, 이후 final delivery root가 그 ancestor authority를 기록하므로 self-reference를 만들지 않는다.

final delivery head/tree는 commit 후 external exact-head verifier/CI가 terminal별로 독립 resolve한다. tracked payload/root file 안에는 self-referential final `HEAD`를 기록하지 않는다. retained terminal verifier는 final delivery head가 measurement-source의 descendant인지 확인하고 measurement-source부터 final head까지 production source, build script/catalog, test fixture, benchmark source/config/input이 바뀌지 않았음을 exact path와 blob SHA-256으로 증명한다. 허용 범위는 root index가 `docs/benchmarks/raw/issue-757/` 아래 file을 포함해 report, locale README/manual, `CHANGELOG.md`, 이 spec/plan 중 실제 변경된 docs/evidence file을 각각 exact path로 열거한 목록뿐이다. prefix나 wildcard는 allowlist가 아니며 production, build, test 또는 benchmark input을 통과시킬 수 없다.

`rejected-after-regression` terminal은 candidate 측정이 끝난 뒤 별도의 clean detached rollback-source commit/tree를 만들고 immutable authority로 기록한다. candidate measurement-source는 rollback-source의 ancestor여야 한다. rollback-source는 승인된 canonical approved rollback diff contract 하나와 exact 일치해야 한다. 이 contract는 version/ID와 허용된 governed implementation/test exact path set, 각 path의 pre/post blob SHA-256 또는 required deletion을 기록하고 다음을 모두 요구한다: optimized private subtype과 production dispatch 제거, `LettuceBinaryCodec`의 class-open 및 ordinary-method-final을 포함한 retained-only ABI delta와 raw flags를 immutable baseline과 exact-equal 상태로 복원, direct writer private seam과 구현 전용 테스트 제거. governed set 밖 production/build/test path 변화, blob mismatch, 누락된 deletion 또는 추가 변경은 rollback-source 승인을 거부한다. contract/approval artifact 같은 docs/evidence 변화는 root index의 별도 exact path/hash 목록으로만 허용한다. generic compatibility/security 테스트와 benchmark/evidence validator는 삭제 대상이 아니며 rollback-source에서 통과해야 한다. rollback-source에서 같은 toolchain으로 생성한 relevant ABI는 immutable baseline과 exact equal이어야 하고 optimized subtype은 class directory와 JAR exact-entry scan 및 reflection에 존재하지 않아야 한다.

rejected terminal의 external verifier는 final delivery head가 rollback-source의 descendant인지 확인하고 rollback-source부터 final head까지 production source, build script/catalog, test fixture, benchmark source/config/input에 drift가 없음을 exact path/blob으로 증명한다. 이 구간의 허용 범위도 root index가 열거한 exact docs/evidence file뿐이다. verifier가 final head를 같은 toolchain으로 다시 build해 relevant ABI의 exact baseline equality와 optimized subtype의 class/JAR/reflection absence도 재확인한다. retained 또는 rejected chain에서 ancestry 실패, authority rewrite, contract 불일치나 허용 범위 밖 변화가 발견되면 terminal validation을 중단한다. retained는 재측정 전까지, rejected는 새 승인된 rollback-source와 그 검증 전까지 delivery blocker다.

## 6. Buffer, failure, resource 계약

optimized encode는 시작 `writerIndex`를 저장하고 packed `Any`를 한 번 만든 뒤 exact size를 계산한다. `ensureWritable(size)`가 성공하면 private bounded output이 현재 writer range에 absolute write한다. 이 output은 자체 cursor와 upper bound를 가지며 target index를 바꾸지 않는다. 성공 시 `writerIndex`를 정확히 `size`만큼 한 번 전진시킨다. production writer는 private function dependency로 주입되어 테스트가 부분 write 후 실패를 결정적으로 만들 수 있으나 public ABI에는 노출되지 않는다. 테스트는 factory가 반환한 private subtype의 private constructor에 reflection으로 접근해 writer dependency를 교체하며, 테스트 편의를 위한 `internal`/public seam은 추가하지 않는다. private writer는 written count를 반환하며, 정상 반환하더라도 `written != size`이면 exact-count guard가 예외를 발생시키고 index commit을 금지한다.

| Condition                              | Result                                                               |
|----------------------------------------|----------------------------------------------------------------------|
| exact/충분한 writable capacity         | 기존 prefix 보존, exact byte count만큼 writer index 전진             |
| expandable capacity                    | target 자체가 확장된 뒤 동일 contract로 성공                         |
| max capacity 부족                      | 기존 Netty exception 전파, writer index 보존                         |
| read-only target                       | 기존 Netty `ReadOnlyBufferException` 전파, indices/marks/refCnt 보존 |
| `ProtoAny.pack` 실패                   | 기존 `BinarySerializationException` type/message/cause chain         |
| writer 실패                            | writer index 보존, 시도된 range bytes는 undefined                    |
| writer가 `size - 1` bytes 후 정상 반환 | exact-count failure, indices/marks/refCnt 보존                       |
| released target                        | 기존 Netty reference-count exception 전파                            |
| null target                            | value 검사/pack/serialize 없이 immediate no-op                       |
| heap/direct/composite/wrapped target   | 동일한 absolute-write contract                                       |
| strict non-Protobuf value              | 기존 serializer exception type/message/cause chain                   |
| trusted non-Protobuf value             | 기존 fallback bytes 및 decode compatibility                          |

codec은 input target의 `readerIndex`, marks, `refCnt`, ownership을 변경하지 않는다. 성공과 실패 모두 retain/release하지 않는다. `ensureWritable`로 증가한 capacity는 후속 writer 실패 시 축소하지 않으며, 실패한 attempted range의 contents도 rollback을 보장하지 않는다. caller는 실패 후 range를 clear/reinitialize하거나 buffer를 폐기해야 한다.

Lettuce가 생성한 temporary target의 최종 release는 Lettuce가 소유한다. codec이 해당 buffer 또는 bounded writer를 반환값, field, coroutine, 다른 thread로 escape시키지 않는다. 호출 동안 target은 thread-confined여야 한다.

## 7. Benchmark 및 evidence gate

기존 `benchmark/protobuf-codec-benchmark`에 existing project
`:bluetape4k-lettuce`를 benchmark-only dependency로 추가하고 Lettuce encode cells를 추가한다. production module dependency graph, 새 benchmark module, external library, profiler dependency는 만들지 않는다.

### 7.1 비교 matrix

동일한 canonical `BenchmarkMessage`, packed payload, target start/headroom을 사용해 다음 네 method를 추가한다.

| Target           | Baseline method                                    | Candidate method                                      | Claim eligibility |
|------------------|----------------------------------------------------|-------------------------------------------------------|-------------------|
| heap `ByteBuf`   | `ProtobufCodecBenchmark.lettuceEncodeHeapCopied`   | `ProtobufCodecBenchmark.lettuceEncodeHeapOptimized`   | Candidate only    |
| direct `ByteBuf` | `ProtobufCodecBenchmark.lettuceEncodeDirectCopied` | `ProtobufCodecBenchmark.lettuceEncodeDirectOptimized` | Candidate only    |

fully qualified prefix는
`io.bluetape4k.protobuf.benchmark.ProtobufCodecBenchmark`다. baseline은 generic
`LettuceBinaryCodec(ProtobufSerializer())`, candidate는 production
`LettuceProtobufCodecs.protobuf()`를 사용한다. 두 쌍은 동일 allocator, final capacity, max capacity, canonical prefix, initial reader/writer index, payload를 사용한다.

trial setup은 heap/direct buffer를 timed region 밖에서 동일 final capacity까지 미리 확장하고 canonical prefix와 initial indices를 설정한다. invocation setup은
`setIndex(0, prefixSize)`, prefix rewrite, `readerIndex(canonicalReaderIndex)` 순서로 복원한 뒤 prefix를 검증한다. timed method는 target allocation, capacity growth, duplicate, payload 생성 없이 codec을 호출하고, allocation 없는 방식으로 resulting writer index와 encoded range의 deterministic first/last-byte checksum을 `Blackhole`에 소비한다. checksum 관측은 index를 바꾸거나 decode하지 않는다. trial teardown에서 codec이 소유하지 않은 benchmark buffer를 정확히 한 번 release한다.

fixture validation은 모든 cell의 output length, prefix 보존, wire bytes, decoded message, writer-index delta, post-call `refCnt`를 검증한다. composite direct-write와 trusted non-Protobuf fallback은 unit/control test로 검증하되 positive allocation matrix에는 넣지 않는다.

### 7.2 fail-closed 규칙

기존 validator와 metadata matrix를 확장하여 다음을 non-zero failure로 처리한다.

- 새 Lettuce method 하나라도 missing, unexpected, duplicate 상태다.
- baseline/candidate pair 또는 heap/direct pair가 불완전하다.
- `secondaryMetrics["gc.alloc.rate.norm"]`, 그 metric의 `score`, `scoreError`, `B/op`
  unit 또는 run identity가 누락되거나 invalid다. `score`는 finite이고 `> 0`,
  `scoreError`는 finite이고 `>= 0`이어야 한다. JMH `primaryMetric` throughput 값은 allocation 판정식에 사용하지 않는다.
- 두 run의 method set, exact measurement-source commit/tree, built JAR SHA-256, JDK, JVM args, JMH mode, forks, warmup iterations/time, measurement iterations/time, threads, profiler configuration, allocator class, target capacities, benchmark parameters 또는 payload/config fingerprint가 다르다.
- raw run ID가 같거나 evidence를 덮어쓴다.

validator fixture test는 complete, missing, unexpected, wrong-unit, missing score/error, NaN/Infinity/negative score or error, identity mismatch, wrong method name, measurement configuration mismatch와 Lettuce pair mismatch를 포함한다.

### 7.3 측정 및 주장

- existing canonical JMH GC profile과 환경 manifest를 그대로 사용한다.
- immutable clean detached measurement-source checkout에서 다른 heavy work와 병렬 실행하지 않고 서로 다른 run ID로 두 번 순차 실행한다.
- `gc.alloc.rate.norm` B/op가 primary metric이며 throughput은 diagnostic이다.
- 아래 식의 `candidateScore`, `candidateError`, `baselineScore`, `baselineError`는 모두
  `secondaryMetrics["gc.alloc.rate.norm"]`의 `score`와 `scoreError`다.
- positive claim은 두 run 각각에서 `candidate <= baseline * 0.95`,
  `baseline - candidate >= 8 B/op`, 그리고
  `candidateScore + candidateError < baselineScore - baselineError`를 모두 만족할 때만 허용한다. validator metadata에 이 exact formula와 8 B/op floor를 기록한다.
- mixed direction, relative/absolute floor 미달 또는 uncertainty overlap은
  `inconclusive`로 기록하고 긍정 문구를 쓰지 않는다.
- optimized subtype 제거는 heap 또는 direct cell 중 하나라도 두 run 각각에서 위 공식을 대칭 적용한 `candidate >= baseline * 1.05`,
  `candidate - baseline >= 8 B/op`,
  `candidateScore - candidateError > baselineScore + baselineError`를 모두 만족할 때 실행한다. 그 외에는 cell별 positive claim을 판정하고 positive cell이 하나도 없으면
  `retained-inconclusive`로 처리한다. validator와 neutral evidence는 유지할 수 있다.
- delivery terminal precedence는 다음과 같다. eligible cell 하나라도 confirmed regression이면 `rejected-after-regression`, 그렇지 않고 eligible cell 하나라도 두 run 모두 acceptance formula를 만족하면 `retained-accepted`, 나머지는
  `retained-inconclusive`다. `retained-accepted`에서도 positive claim은 acceptance를 만족한 개별 cell에만 허용한다.
- 기능 및 compatibility 테스트가 통과해도 invalid/missing evidence로 성능 주장을 대신할 수 없다.

측정 결과의 delivery terminal은 다음 세 가지다.

| Terminal                    | Production dispatch                                                                                                                                    | Claim                                                   | Issue/PR disposition                                                                                                                                     |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `retained-accepted`         | 유지                                                                                                                                                   | 두 run 모두 acceptance formula를 만족한 cell만 positive | direct-write AC/DoD를 적용하고 merge-ready 진행                                                                                                          |
| `retained-inconclusive`     | 유지                                                                                                                                                   | neutral only                                            | direct-write 기능 AC/DoD는 적용하되 성능 개선 주장은 금지하고 inconclusive evidence를 기록                                                               |
| `rejected-after-regression` | canonical rollback contract로 subtype/dispatch/implementation-only tests를 제거하고 raw member final flags를 포함한 exact normalized baseline ABI 복원 | negative/neutral only                                   | immutable rollback-source 검증을 거친 non-closing benchmark/evidence-only PR로 merge-ready 진행하되 direct-write AC/DoD 완료나 #757 종료를 주장하지 않음 |

기존 promoted evidence는 덮어쓰지 않는다. archive-aware replacement command가 먼저 기존 promoted generation을
`docs/benchmarks/raw/issue-757/archive/<old-delivery-commit>/` 아래 regular files로 복사하고, 새 delivery manifest의 `superseded_evidence`에 generation, relative path, 각 file SHA-256과 archive file-set hash를 기록한다. 이 committed archive는 replacement backup cleanup 대상이 아니며 전용 validator를 통과하지 않으면 promotion을 거부한다. 기존 cleanup 가능 backup만 만드는 command는 이번 replacement에 사용할 수 없다. 새 raw JSON, derived CSV, environment metadata는 서로 다른 run ID, exact clean detached measurement-source commit/tree, built JAR SHA-256과 함께
`docs/benchmarks/raw/issue-757/<run-id>/` lifecycle을 따른다. verified delivery manifest만으로 report를 재생성하고 promoted tree validator가 immutable archive, raw, derived, metadata, report의 연결을 검증해야 한다. 결론은 기존 issue #757 allocation report에 Lettuce section으로 추가하고 README는 숫자를 복제하지 않고 report를 링크한다. 반복 replacement는 기존 `archive/` subtree를 다시 복사하지 않는다. 이전 manifest의
`superseded_evidence` entries는 동일 path/hash로 carry-forward하고, 바로 직전 active generation의 non-archive file set만 새 `<old-delivery-commit>` archive에 한 번 복사한다. 기존 rollback bundle이 참조하는 legacy archive는 해당 generation file set에 정확히 한 번 포함하며 nested archive duplication을 validator가 거부한다.

replacement는 destination 내부를 in-place 수정하지 않는다. input/file-set hash에서 결정한 고유 generation ID와 한 번도 존재하지 않은 immutable
`generations/<generation-id>` path를 예약하고, 같은 filesystem의 sibling에 owner/token을 포함한 고유 staging directory를 만든다. promoter는 reservation 전에 OS-level exclusive promotion lock을 획득하고 monotonically increasing fencing token을 받아 generation reservation부터 active pointer parent-directory fsync 완료까지 lock을 유지한다. 모든 staging receipt와 pointer candidate는 그 token을 포함하며 stale token은 거부한다.

staging에서 payload/root DAG, archive carry-forward, file-set hash, permissions와 모든 validator를 완성하고 각 staged file과 하위 directory를 fsync한 뒤에만 staging을 새 generation path로 publish한다. publish는 Linux의 `renameat2(RENAME_NOREPLACE)`, macOS의
`renamex_np(RENAME_EXCL)`처럼 atomic no-replace를 보장하는 platform primitive를 사용한다. 지원되는 no-replace primitive가 없으면 promotion을 fail-closed한다. rename 성공 뒤
`generations/` parent directory를 fsync해야 generation 생성이 durable하다. target path가 이미 존재하거나 non-empty이면 동일 내용처럼 보여도 replace/delete하지 않는다. 단, 이전 attempt가 generation rename과 parent fsync까지 완료했다는 owner/token/root-hash receipt가 exact 일치하는 idempotent resume는 그 immutable generation을 전부 재검증하고 generation rename 없이 pointer 단계만 재개한다. 기존 generation은 모두 유지한다.

새 generation이 완전히 존재한 뒤 작은 단일 `active-generation.json` pointer/index만 갱신한다. 새 pointer는 immutable generation ID와 expected root/file-set hash를 담되 자기 hash나 아직 없는 final commit/tree를 담지 않는다. 기존 pointer generation/hash를 compare-and-swap precondition으로 다시 확인하고, 같은 directory의 고유 temp file을 write·flush/fsync한 뒤 atomic rename으로 pointer 한 파일만 교체하고 pointer parent directory를 fsync한다. exclusive lock과 fencing token 아래에서 previous pointer를 다시 확인하므로 두 promoter의 read/check/rename race를 허용하지 않는다. concurrent pointer 변화나 stale fencing token은 promotion 실패이며 기존 pointer를 덮어쓰지 않는다. active pointer는 runtime selection일 뿐이고 tracked Git commit/tree가 generation, pointer와 evidence 전체의 최종 authority다.

validation 또는 generation rename 전에 process가 중단되면 old active pointer와 모든 generation은 그대로이고 owned staging만 incomplete 상태로 남는다. generation rename 뒤 pointer rename 전에 중단되면 새 generation은 검증된 unreferenced immutable generation으로 남고 old pointer가 계속 active다. rerun은 exact input/root hash가 같은 그 generation을 재검증한 뒤 pointer 단계만 재개할 수 있어야 하며 동일 input에서 같은 결과를 내는 idempotent operation이어야 한다. 시작 시 owner/age/manifest를 확인해 자신의 abandoned partial staging만 정리하고 active staging, immutable generation, old generation 또는 owner/hash가 다른 partial tree는 삭제하지 않는다.

## 8. 실패 모드와 처리

1. **잘못된 size
   estimate:** message 자체 크기 대신 packed `Any.serializedSize`를 사용한다. 실제 write count가 exact size와 다르면 writer index를 commit하지 않고 실패한다.
2. **분리된 NIO
   view:** NIO view를 사용하지 않는다. bounded writer는 target의 absolute `setByte/setBytes`만 사용하므로 heap/direct/composite 모두 원본 storage에 기록한다. detached `nioBuffer()`를 반환하는 hostile custom buffer도 NIO method가 호출되지 않아 stale bytes를 commit할 수 없다.
3. **max-capacity 또는 writer
   실패:** writer index와 `refCnt`를 보존한다. capacity expansion과 attempted bytes는 rollback하지 않으며 문서화된 Netty exception을 전파한다.
4. **pack failure compatibility:** `ProtoAny.pack` failure는 기존 serializer와 동일한
   `BinarySerializationException` type/message/cause로 변환하고 target state는 보존한다.
5. **trusted fallback
   우회:** only `ProtoMessage` type check가 direct dispatch를 활성화한다. non-Protobuf value는 기존 serializer가 처리하므로 fallback wire와 신뢰 경계를 바꾸지 않는다.
6. **strict profile 완화:** non-Protobuf value가 superclass path에서 성공하거나 다른 예외로 바뀌면 delivery blocker다.
7. **buffer lifetime 오류:** codec이 retain/release하거나 bounded writer를 escape해
   `refCnt`, lifetime, thread confinement가 달라지면 delivery blocker다.
8. **compressed codec의 우발적 변경:** compressed factory가 specialized subtype을 만들거나 diff/benchmark claim에 포함되면 범위 위반으로 되돌린다.
9. **allocation 개선
   미재현:** 두 fresh run이 relative 5%, absolute 8 B/op, non-overlapping uncertainty를 모두 증명하지 못하면 긍정 성능 문구를 제거한다. 첫 release/publish 전 두 run 모두 대칭 regression formula를 만족하면 optimized dispatch를 제거하고 class와 모든 member raw flags를 immutable authority baseline으로 복원한다. rejected validator가 raw member final flags를 포함한 exact normalized baseline equality를 증명한 뒤에만 `rejected-after-regression` terminal로 끝낸다. release 뒤 rollback에는 10.1의 published ABI 보존 규칙이 우선한다.
10. **ABI
    drift:** validator가 wrong class 또는 incomplete input을 받아 성공하거나, exact constructor/target invariant, 3.1의 class/일곱 ordinary method/target/세 bridge numeric transition 중 하나가 깨지거나 allowlist 밖 descriptor/access/flag가 바뀌면 delivery를 중단한다. Kotlin/Java caller compile 성공만으로 bytecode flag drift를 승인하지 않는다.
11. **ABI provenance 또는 toolchain
    drift:** baseline commit/tree가 immutable authority와 다르거나 candidate payload가 clean detached measurement-source commit/tree와 다르면 즉시 거부한다. distinct build root, clean-state receipt, class/JAR/raw-output hash 또는 same-classfile/JAR-entry binding 누락·불일치도 ABI evidence를 거부한다. Kotlin compiler, Gradle/toolchain, JDK 또는 exact `javap` binary가 다르면 기존 normalization을 재사용하지 않고 spec/plan reapproval로 돌아간다.
12. **source seam
    확대:** negative Kotlin/Java fixture 중 source key method나 erased/raw/generic bridge signature override 하나라도 compile되면 one-source-seam 전제가 깨진 것이다. synthetic bridge로 normalize-away하지 않고 spec을 reopen한다.
13. **decode security
    drift:** pre-existing/crafted out-of-prefix `typeUrl`, malformed payload, custom-prefix 또는 trusted fallback fixture의 exception/class-loading 경계가 바뀌면 delivery를 중단한다. unauthorized class loading 시도는 성공 여부와 관계없이 blocker다.
14. **post-release ABI
    제거:** release된 뒤 generic dispatch로 rollback하면서 class를 다시 final로 만들거나 3.1의 retained raw flag set을 바꾸면 binary compatibility 위반이다. 별도 compatibility/migration 승인 없이는 ABI 제거를 금지하고 dispatch만 되돌린다.
15. **retained measurement/delivery
    혼동:** tracked payload/root에 final `HEAD`를 기록하거나 retained final head가 measurement-source의 descendant가 아니거나 그 사이 exact docs/evidence allowlist 밖 production/build/test/benchmark path/blob이 달라지면 evidence를 무효화하고 재측정한다.
16. **rejected rollback-source
    불완전:** candidate measurement-source가 immutable clean rollback-source commit/tree의 ancestor가 아니거나 canonical rollback contract의 exact path set, pre/post blob hash, required deletion 중 하나라도 어긋나면 rejected terminal을 중단한다. optimized subtype/dispatch, retained-only ABI delta 또는 implementation-only test/seam이 남거나 relevant ABI가 baseline과 exact-equal하지 않거나 subtype-absence/functional-test proof가 없으면 새 승인된 rollback-source를 만들어 다시 검증한다.
17. **rejected final-head
    drift:** rejected final head가 rollback-source의 descendant가 아니거나 rollback-source부터 final head까지 exact docs/evidence allowlist 밖 production, build, test 또는 benchmark input이 바뀌거나 final relevant ABI/subtype-absence 재검증이 실패하면 delivery를 중단한다. candidate measurement-source부터 final head까지 production 무변경을 요구하지 않으며, 승인된 rollback contract가 정의한 candidate-to-rollback 변경만 별도 authority로 허용한다.
18. **cyclic 또는 불완전 hash
    graph:** payload가 validator/root/final commit을 hash하거나 root가 자기 자신을 hash하거나 validator result가 payload hash를 참조하지 않으면 promotion을 거부한다. rejected rollback contract가 아직 없는 rollback-source/final commit/tree 또는 자기 hash를 기록하는 경우도 거부한다. JAR exact entry와 standalone classfile hash mismatch도 blocker다.
19. **dirty/stale
    build:** detached checkout의 pre/post tracked/non-ignored state, empty build-root preflight 또는 stale ignored-output 거부 중 하나라도 빠지면 baseline과 candidate를 새 checkout에서 다시 build/measure한다.
20. **generation/pointer promotion
    위반:** staging을 fully validate하기 전에 generation으로 rename하거나 이미 존재하거나 non-empty인 generation target을 replace/delete하면 promotion을 거부한다. generation rename 전 중단은 owned staging만 남기고, generation rename 후 pointer 교체 전 중단은 old pointer와 unreferenced immutable generation을 보존한다. exclusive promotion lock/fencing token, platform atomic no-replace, staged file/directory와 generation parent fsync가 하나라도 없으면 publish를 거부한다. pointer는 lock 아래 previous generation/hash CAS, temp file fsync, atomic rename과 parent-directory fsync를 통과해야 하며 concurrent drift는 old pointer를 유지한 채 재검증한다. immutable old/new generation은 cleanup하지 않는다.
21. **recovery authority/checklist
    누락:** published GAV/JAR SHA/release commit·tree, planned recovery artifact/target/change request, baseline/observation window, threshold, metric query, supported release 또는 owner/escalation이 없으면 recovery dispatch를 중단한다. dispatch 뒤 actual digest/change ID/timestamps/deployment confirmation 또는
    `published-retained-vs-recovery` result가 없으면 observation 시작과 close를 중단한다.

## 9. 테스트 전략

### `LettuceBinaryCodec`와 factory compatibility

- 기존 generic codec의 key/value encode/decode, estimate, toString 회귀.
- Kotlin 및 Java에서 기존 한 인자 constructor와 factory 반환형 compile proof.
- immutable baseline과 candidate의 `./gradlew :bluetape4k-lettuce:compileKotlin` 산출물에 paired `javap -p -s`와 paired `javap -p -v`를 실행한다. structural output은 exact constructor/target invariants와 전체 member set을 비교하고, verbose output은 3.1의 class, ordinary source method 일곱 개, unchanged target, unchanged bridge 세 개와 나머지 exact-equal member의 numeric flags를 모두 증명한다.
- validator는 wrong-class, missing-baseline, missing-candidate, truncated structural/verbose input, wrong immutable baseline commit/tree, candidate measurement-source mismatch, dirty/stale checkout, constructor drift, target-method drift, extra changed method를 모두 non-zero로 거부한다.
- 구현 plan은 ordinary Kotlin/Java compile fixture와 별도로 bridge 하나의
  `ACC_PUBLIC`/`ACC_BRIDGE`/`ACC_SYNTHETIC` 제거, unexpected `ACC_FINAL` 또는 다른 extra flag 추가, allowlist 밖 method flag 변경, 네 번째 bridge/member 추가를 만든 bytecode-level mutation fixture를 포함한다. 모든 mutation은 validator의 non-zero failure를 증명해야 한다.
- authority/transition mutation matrix는 baseline commit, baseline tree, candidate measurement-source commit/tree를 각각 틀리게 만든 case와 class `0x0031 -> 0x0021`, ordinary method 일곱 개 `0x0001 -> 0x0011`, target `0x0001 -> 0x0001`, bridge 세 개
  `0x1041 -> 0x1041`의 각 baseline/candidate side를 하나씩 바꾼 case를 포함한다. 어느 required transition도 effective-final normalization으로 생략하지 않는다.
- detached checkout fixture는 shared root, dirty tracked file, non-ignored untracked input, pre-existing ignored class/JAR, post-build tracked drift와 symlink escape를 각각 거부한다.
- JAR fixture는 missing/duplicate/wrong-path entry, corrupted entry와 standalone classfile mismatch를 거부하고 exact entry와 standalone hash가 같은 positive case를 통과한다.
- hash-DAG fixture는 payload -> class/JAR/raw, result -> payload, root -> payload/result의 방향만 허용하고 payload의 result/root/final-head 참조와 root self-hash를 거부한다. rejected case는 root -> rollback contract, contract -> candidate measurement-source와 exact path/blob/deletion set만 허용하며 contract self-hash와 rollback-source/final-head 참조를 거부한다.
- retained external exact-head fixture는 measurement-source descendant와 exact docs/evidence path/blob allowlist만 통과시키고 non-descendant, rewritten measurement-source, production/build/test/benchmark blob drift, prefix 또는 wildcard allowlist를 거부한다.
- rejected verifier fixture는 candidate measurement-source -> immutable rollback-source ->
  final head ancestry를 통과시키고 wrong/missing/re-written rollback-source를 거부한다. canonical rollback contract의 exact path set과 pre/post blob SHA-256, required deletion을 하나씩 변형해 모두 거부되는지 검증하며 optimized subtype/dispatch, retained-only ABI delta, private writer seam 또는 implementation-only test가 남은 case도 거부한다. rollback-source의 relevant ABI exact baseline equality, class/JAR/reflection subtype absence와 generic compatibility/security functional-test receipt가 모두 필요하다. final verifier는 final relevant ABI exact baseline equality와 class/JAR/reflection subtype absence를 다시 증명하고, rollback-source부터 final head까지 exact docs/evidence allowlist만 허용하고 production, build, test, benchmark drift와 prefix/wildcard allowlist를 거부한다.
- generation promotion fixture는 validation failure, 한 번도 존재하지 않은 target으로의 platform atomic no-replace, unrelated existing/non-empty generation target 거부, old generation 보존을 검증한다. 두 simultaneous promoter에서 exclusive lock/fencing token이 한 promoter만 publish하게 하고 stale token이 pointer를 바꾸지 못하는지 검증한다. staged file/directory fsync 전후, generation rename 전후와 generation-parent fsync 전후, pointer temp fsync/rename/parent fsync 전후 crash를 각각 재현해 old pointer 또는 새 pointer가 온전하고 재시작 후 durable한지 확인한다. 동일 input generation의 idempotent resume, owned orphan staging cleanup, immutable generation 비삭제와 previous-pointer CAS/concurrent drift 거부도 검증한다.
- Kotlin 및 Java compile fixture와 reflection assertion을 targeted Gradle test로 실행함.
- compile-negative fixture는 Kotlin과 Java subclass가 source key methods (`encodeKey(String)`, `encodeKey(String, ByteBuf)`, `decodeKey(ByteBuffer)`)와 erased/raw/generic bridge signatures (`encodeKey(Object)`,
  `encodeKey(Object, ByteBuf)`, `decodeKey(ByteBuffer): Object`)를 각각 override하려는 source를 컴파일한다. 모든 fixture는 compile failure여야 한다. 하나라도 compile되면 one-source-seam premise가 틀린 것이므로 bridge를 synthetic noise로 숨기지 않고 spec을 reopen한다.
- Java reflection에서 private nested Protobuf codec의 public/protected constructor와 externally accessible codec class가 없음.
- strict/trusted, Protobuf/non-Protobuf 조합 모두 null target 호출 시 value 검사와 serialization 없는 no-op이며 Kotlin 및 reflective Java 호출 결과가 동일함.
- compressed factory가 계속 generic copied codec을 사용함.

ABI evidence command shape는 다음과 같다. `BASELINE_CLASSES`, `CANDIDATE_CLASSES`,
`ABI_EVIDENCE_DIR`는 plan에서 immutable baseline/candidate build와 tracked commit에 결속한다.

```bash
"$JAVA_HOME/bin/javap" -classpath "$BASELINE_CLASSES" -p -s \
  io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec \
  > "$ABI_EVIDENCE_DIR/baseline.struct.txt"
"$JAVA_HOME/bin/javap" -classpath "$CANDIDATE_CLASSES" -p -s \
  io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec \
  > "$ABI_EVIDENCE_DIR/candidate.struct.txt"
"$JAVA_HOME/bin/javap" -classpath "$BASELINE_CLASSES" -p -v \
  io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec \
  > "$ABI_EVIDENCE_DIR/baseline.verbose.txt"
"$JAVA_HOME/bin/javap" -classpath "$CANDIDATE_CLASSES" -p -v \
  io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec \
  > "$ABI_EVIDENCE_DIR/candidate.verbose.txt"
```

각 command 전후 exact classfile SHA-256을 다시 계산해 같은 role의 structural/verbose input binding을 manifest에 기록한다. validator command는 위 raw file path를 CLI에서 따로 받지 않고 5.5의 immutable manifest 하나만 필수 입력으로 받으며 manifest의 role, hash, toolchain, same-classfile binding을 먼저 검증한다. retained mode는 3.1의 exact allowlist만 허용한다. rejected mode는 raw member `final` flags를 normalize-away하지 않고 exact normalized baseline equality를 요구한다.

### Protobuf direct encode

이 절의 direct-writer 전용 테스트는 candidate measurement-source commit과 retained terminal의 final head에 적용한다. `rejected-after-regression` final head에서는 subtype, private seam, 그 구현에만 존재하는 테스트를 제거해 N/A로 처리하고, delivery manifest가 모든 기능 테스트를 통과한 immutable candidate measurement-source commit, tracked-tree hash, JAR hash와 negative JMH evidence를 가리키는지 검증한다.

- heap/direct `ByteBuf`의 exact-capacity 및 reusable oversized target.
- non-zero reader/writer index와 prefix 보존, exact writer-index advancement.
- `packMessage` 및 이전 generic codec과 wire-byte equality, 양방향 decode.
- initial capacity가 작지만 max capacity 안인 target 확장.
- max-capacity 부족과 private writer seam으로 partial absolute write 후 발생시킨 failure에서 reader/writer indices, observable marks, `refCnt` 보존. capacity와 attempted bytes는 변경될 수 있음.
- injected writer가 `size - 1` bytes만 쓰고 정상 반환하는 경우 exact-count guard가 실패시키며 reader/writer indices, observable marks, `refCnt`를 보존함.
- private writer seam은 reflection으로만 주입하고 public/internal JVM surface가 추가되지 않았음을 reflection/API diff로 함께 검증함.
- read-only target에서 `ReadOnlyBufferException`, reader/writer indices, observable marks,
  `refCnt` 보존.
- `ProtoAny.pack` failure에서 기존 `BinarySerializationException` type/message/cause와 target state 보존.
- released target의 기존 Netty failure.
- zero/one/multi-component composite, sliced/wrapped target에서 정확한 direct bytes 생성.
- `nioBufferCount() == 1`이면서 detached NIO copy를 반환하는 hostile custom target도 NIO API 호출 없이 정확한 bytes를 생성함.
- strict non-Protobuf rejection의 exception type/message/cause chain.
- trusted `ProtoMessage` direct path와 trusted-input-only non-Protobuf fallback fixture 호환.
- `encodeValue(value): ByteBuffer` 및 `decodeValue(ByteBuffer)` compatibility path 불변.

### Decode wire/security compatibility

- 이전 release가 만든 pre-existing strict/trusted wire fixture를 optimized codec이 기존과 같은 message 또는 exception으로 decode한다.
- allowlist 밖 application type을 가리키는 crafted `Any.typeUrl`은 기존 exception type/message/cause chain을 유지한다. counting/trap `ClassLoader`로 해당 class의 load/initialize 시도가 0회임을 검증한다.
- truncated field, invalid tag/length, corrupt embedded message를 포함한 malformed payload는 기존 exception contract를 유지하며 같은 trap에서 unauthorized class loading을 유발하지 않는다.
- custom-prefix `ProtobufSerializer` fixture는 optimized subtype으로 우회하지 않고 일반
  `LettuceBinaryCodec` copied path를 계속 사용한다.
- trusted non-Protobuf fallback fixture는 trusted-input-only로 표시하고 untrusted input acceptance 또는 class-loading safety proof로 재사용하지 않는다.

### Lettuce integration

- 기존 `LettuceProtobufCodecsTest` strict/trusted Redis round trip.
- 이전 codec으로 쓴 fixture를 새 codec이 읽고 새 codec bytes를 이전 codec이 읽음.
- Redis command encode 반복 호출에서 stale bytes, index drift, ownership 변화 없음.
- Testcontainers-backed Lettuce 검증은 다른 module/worktree의 container test와 병렬 실행하지 않는다.

### Benchmark gate

- fixture의 heap/direct copied/optimized semantic equality와 invocation reset.
- exact expected method set complete/missing/unexpected/duplicate/wrong-name.
- Lettuce baseline/candidate 및 heap/direct matrix pair validation.
- GC metric의 `score`/`scoreError`/unit/range, execution-parameter identity, relative/absolute/uncertainty formula mismatch failure.
- heap/direct mixed-cell accepted/inconclusive/regression 조합과 terminal precedence.
- compile/smoke 후 두 fresh canonical GC-profiler run.

## 10. 문서와 전달

- public KDoc는 optimized `ToByteBufEncoder` 범위, compatibility path, target ownership, failure 후 range 처리, zero-copy 비보장을 설명한다.
- `LettuceBinaryCodec` class KDoc는 유일하게 지원되는 source extension point, 나머지 source method의 final 경계, compiler bridge가 지원되는 source extension API가 아니라는 점을 설명하고 overload KDoc의 null/index/ownership/failure 계약으로 연결한다.
- `io/protobuf` README locale pair와 manual은 기본 allowlist에 이미 포함된 기존 factory 호출자만 migration 없이 direct path를 사용한다고 설명한다. 기본 allowlist, 일반 application package decode 제한, custom-prefix serializer는 generic codec path라는 사실과 compressed/decode/fallback 경로가 별도임을 명시한다. allowlist 밖의
  `MyMessage`가 곧바로 round-trip하는 예시는 쓰지 않는다.
- benchmark README locale pair와 manual은 새 method matrix, 실행/검증 command, B/op primary metric, two-run claim rule를 함께 갱신한다.
- allocation 수치의 단일 source of truth는 issue #757 benchmark report다.
- `CHANGELOG.md`는 measured direct-write 범위만 요약하고 throughput/zero-copy를 주장하지 않는다.
- merge 전 #757은 open 상태로 유지하고 implementation, tests, promoted-tree validation, exact-head evidence 및 PR을 연결한다.
- PR은 `bluetape4k/bluetape4k-projects`, base `develop`, head
  `feat/issue-757-lettuce-protobuf-buffer`로 만들며 body의 마지막 `##` heading은
  `## DoD Status`다.
- exact-head CI, automated review, applicable human review artifact와 unresolved thread 상태가 모두 통과하면 merge-ready로 보고하고 fresh merge approval을 기다린다.

### 10.1 Operational handoff와 rollback

issue assignee 또는 지정 maintainer가 운영 owner다. release 이후 wire mismatch, unexpected encode exception 증가, writer-index/ref-count violation, Redis round-trip regression 중 하나가 확인되면 첫 복구는 uncompressed strict/trusted factory를 generic
`LettuceBinaryCodec`으로 되돌리는 dispatch-only source revert다. 이미 publish된
`LettuceBinaryCodec` class와 target overload는 계속 `open`이고 세 compiler bridge도 retained raw `0x1041`을 유지한다. release 뒤 이 public ABI를 제거하는 rollback은 금지한다. ABI 제거가 필요하면 영향 consumer 조사, deprecation/migration path와 release line을 포함한 별도 compatibility/migration 승인을 먼저 받아야 한다.

rollback owner는 #757을 reopen하고 incident/release version, trigger, affected consumer, dispatch-only diff와 recovery owner를 기록한다. recovery authority는 실제 published artifact의 GAV, version, Maven-retrieved JAR SHA-256, release commit/tree와 repository/ref를 고정한다. local build나 branch head는 published artifact authority를 대신하지 못한다. recovery exact head는 별도 clean detached checkout에서 build하고 ABI validator의
`published-retained-vs-recovery` mode로 published JAR의 retained class/member raw flags와 recovery JAR가 exact-equal인지 검증한다.

recovery dispatch 전에 incident checklist가 다음을 모두 고정해야 한다.

- supported release/GAV와 deployment target, rollback owner, escalation owner/contact.
- incident 전 baseline window와 recovery observation window의 시작/종료/최소 duration.
- Redis command encode/SET failure, serialization/decode error, Netty reference-count, JVM allocation/GC와 round-trip 지표별 pass/fail threshold.
- 각 metric의 authoritative telemetry source, exact query/dashboard link, aggregation, sample/minimum traffic와 missing-data 처리.
- planned recovery artifact의 repository coordinate와 expected digest, deployment target, 실행 예정 command/change request ID, environment/region과 승인 receipt.

이 pre-dispatch authority의 필수 field, query, threshold, owner 또는 승인 receipt가 없으면 recovery dispatch를 fail-closed로 보류한다. dispatch 후 actual deployed digest, executed change ID, rollout 시작·완료 시각과 deployment confirmation evidence를 checklist에 추가한다. 이 post-dispatch evidence가 없으면 observation window 시작과 recovery close를 보류한다. 이어 targeted compatibility/module build, crafted/pre-existing wire security fixture, Redis round-trip, fresh allocation evidence와 promoted-tree validation을 다시 수행한다. release/tag/publish/재배포는 계속 별도 승인 경계다.

recovery close 조건은 checklist가 고정한 supported release에서 generic factory dispatch와 deployment digest가 확인되고, pinned observation window 동안 모든 metric이 pinned threshold를 만족하며, `published-retained-vs-recovery` ABI result와 refreshed wire/allocation evidence, consumer 영향 기록이 reopened #757에 연결되는 것이다. 이 조건을 모두 만족한 뒤에만 recovery issue를 다시 close할 수 있다. optimized dispatch 재도입 또는 published ABI 제거는 recovery close에 포함되지 않으며 각각 새 설계·compatibility 승인을 요구한다.

hot path에 새 telemetry를 넣지 않는다. 기존 authoritative telemetry가 checklist의 threshold/query를 판정할 수 없으면 recovery dispatch 전에 관측 gap을 명시하고 별도 운영 승인을 받는다.

`retained-accepted` 또는 `retained-inconclusive` terminal이 fresh 승인으로 merge된 뒤 같은 owner가 merge commit과 최종 evidence/report를 #757에 기록하고 issue를 close한다. 이어 #898의 #757 항목을 완료 처리하고 #756에는 Lettuce slice 완료와 compressed/custom-prefix/generic SPI 제외 범위를 링크한다. `rejected-after-regression`
terminal은 #757을 close하거나 #898을 완료 처리하지 않고 negative evidence와 남은 scope를 issue에 기록한다. evidence-only PR 자체는 동일한 exact-head 검증과 fresh merge approval을 거쳐 merge할 수 있지만 `Closes #757` metadata를 사용하지 않는다. 이 post-merge closure는 merge-ready 검증과 별도 단계이며 milestone, labels, assignee를 다시 확인한다.

## 11. Acceptance Criteria

### 11.1 Caller-visible acceptance

- uncompressed strict/trusted Protobuf factory의 public signature와 기존 호출 형태가 유지된다.
- `retained-accepted`/`retained-inconclusive`에서는 target을 받는 production Lettuce Protobuf encode success path에 최종 payload-sized `ByteArray` handoff가 없다.
- retained terminal의 의도된 source-level extension seam은
  `LettuceBinaryCodec.encodeValue(V, ByteBuf?)` 정확히 하나다. class
  `0x0031 -> 0x0021`, ordinary source method 일곱 개 `0x0001 -> 0x0011`, target
  `0x0001 -> 0x0001`, bridge 세 개 `0x1041 -> 0x1041` 외에는 descriptor/access/numeric flag drift가 없다.
- retained terminal의 세 bridge는 baseline/candidate 모두 exact method key와
  `0x1041 ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC`을 만족한다. class-final 제거가 effective bytecode overrideability를 활성화한다는 장기 ABI 비용이 KDoc/compatibility evidence에 명시된다.
- `rejected-after-regression`에서는 optimized subtype과 모든 `open` ABI 변화가 최종 diff에 없고 raw member final flags를 포함한 exact normalized baseline equality를 만족하며 direct-write 완료나 #757 종료를 주장하지 않는다.
- output은 기존 `Any` wire bytes와 byte-for-byte 호환된다.
- heap/direct/composite/wrapped target은 prefix, reader/writer indices, `refCnt`, ownership 계약을 지키고 read-only target은 기존 Netty 예외와 target state를 보존한다.
- null target은 serialization 없는 no-op다. compressed, single-argument `ByteBuffer`
  encode, decode, trusted non-Protobuf, custom-prefix serializer는 명시적 compatibility path다.
- Protobuf pack failure, strict rejection, trusted fallback의 exception/security/wire semantics가 유지된다.
- pre-existing/crafted out-of-prefix `typeUrl`과 malformed payload는 기존 exception contract를 유지하고 unauthorized class loading을 시도하지 않는다. custom-prefix는 generic path에 남고 trusted fallback은 trusted-input-only로 표시된다.
- max-capacity, writer failure, released buffer, repeated invocation 테스트가 통과한다.

### 11.2 Delivery/evidence gates

- retained terminal에서는 injected writer의 exception 및 short-success 양쪽에서 exact-count guard와 indices/observable marks/`refCnt` 보존이 검증된다. rejected terminal의 final head에서는 이 구현 전용 검사를 N/A로 처리하고 candidate measurement-source commit의 통과 기록과 manifest-bound hash를 검증한다.
- generic `LettuceBinaryCodec` 기존 behavior와 public descriptors가 유지된다. paired
  `javap -p -s` structural comparison, paired `javap -p -v` flags proof, exact constructor/target invariants가 함께 통과한다.
- ABI validator가 immutable payload manifest만 입력으로 받아 exact measurement-source commit/tree, distinct clean detached roots, FQCN/role, class/JAR/raw-output SHA-256와 same-classfile/JAR-entry binding을 검증한다. baseline role은 `4ee03eb2645e6715e5ec572ffdc10fd61c2a3e88`/
  `086f83baa7eec0cd68e68fff132542ef6db0f200`이고 candidate role은 immutable clean detached measurement-source commit/tree다. 두 role은 같은 Kotlin compiler, Gradle/toolchain, JDK와 exact
  `$JAVA_HOME/bin/javap` binary를 사용하며 version/path/hash가 manifest와 일치한다.
- validator가 wrong class/incomplete input, role/root/hash/toolchain mismatch,
  `ACC_PUBLIC`/`ACC_BRIDGE`/`ACC_SYNTHETIC` 제거, unexpected `ACC_FINAL`/extra flag, fourth bridge/member, wrong baseline authority 또는 required transition mutation을 fail-closed로 거부한다.
- ordinary Kotlin/Java compatibility fixture는 통과하고 source key method와 erased/raw/generic bridge override compile-negative fixture는 모두 실패한다. 하나라도 compile되면 spec이 reopen된다.
- benchmark validator가 Lettuce method/pair/metric/run drift를 fail-closed로 차단한다.
- 두 fresh measurement-source GC-profiler run과 raw/derived/environment evidence가 검증된다.
- positive allocation claim은 두 run 모두 relative 5%, absolute 8 B/op, non-overlapping uncertainty를 만족한 candidate에만 존재한다.
- heap 또는 direct cell 중 하나라도 두 run 모두 대칭 regression formula를 만족하면 immutable candidate measurement-source와 negative evidence를 먼저 확정한다. 그 descendant인 clean rollback-source는 승인된 canonical exact path/blob/deletion contract만 적용하고, optimized subtype/dispatch, retained-only ABI delta와 private writer seam 및 implementation-only test를 제거한다. rollback-source의 relevant ABI는 immutable baseline과 exact-equal이고 subtype은 class/JAR/reflection에서 absent이며 generic compatibility/security functional tests가 통과한다.
- KDoc, locale-paired docs, benchmark report, `CHANGELOG.md`, issue DoD가 source와 일치한다.
- 새 module, external/production dependency, generic serializer SPI, compressed optimization, decode optimization, release/tag/publish/settings 변경이 diff에 없다. existing `:bluetape4k-lettuce` benchmark-only project dependency만 허용한다.
- external exact-head verifier가 retained final delivery head/tree를 commit 후 resolve하고 measurement-source ancestry와 그 이후 exact docs/evidence allowlist 외 production/build/ test/benchmark input 무변경을 증명한다. rejected terminal은 candidate measurement-source ->
  canonical rollback-source ancestry/contract를 검증한 뒤 final head가 rollback-source를 descend하고 그 이후 같은 input drift가 없음을 증명한다. tracked payload/root는 어느 terminal에서도 self-referential final head를 저장하지 않는다.
- fully validated staging은 한 번도 존재하지 않은 immutable generation path로만 atomic rename되고 old generation은 유지된다. 작은 active-generation pointer는 previous-value CAS, temp+fsync+atomic rename으로만 교체된다. generation/pointer 각 interruption 지점, idempotent resume, owned staging cleanup과 concurrent drift 거부가 검증되며 Git commit/tree가 최종 authority다.
- final exact-head rereview가 P0=0, P1=0에 도달해야 merge-ready다. 최신 spec 수정은 caller, API/developer, stability, security, performance, operator 여섯 관점에서 각각 P0=0, P1=0, P2=0, P3=0으로 수렴했다.
- post-release rollback은 factory dispatch만 generic codec으로 되돌리고 3.1의 published retained raw flag set과 effective ABI를 유지한다. published GAV/version/JAR SHA/release commit/tree, `published-retained-vs-recovery` result와 incident checklist가 #757 reopen, dispatch, evidence refresh와 recovery close에 추적된다.

## 12. Definition Of Done

- 승인된 Lettuce narrow slice와 세 measurement terminal 중 하나가 spec, plan, tests, implementation/evidence, docs에 추적 가능하다.
- 기존 caller의 source/binary compatibility와 Protobuf wire/security, Netty ownership 계약이 유지되며, 3.1에 열거한 additive public ABI 비용만 명시적으로 수용된다.
- retained terminal은 source seam 1개, 이를 제외한 ordinary source method 일곱 개의 explicit-final 보호와 exact compiler bridge 3개를 구분한 paired structural/verbose
  `javap` proof, 3.1의 모든 exact numeric transition, fail-closed validator 및 bytecode mutation fixture를 통과한다. rejected terminal은 raw member final flags를 포함해 immutable authority baseline과 exact normalized equality를 통과한다.
- immutable ABI/JMH evidence set은 exact baseline/candidate measurement-source commit·tree, distinct clean detached roots, classfile/JAR exact entry, four raw `javap` outputs, toolchain/executable hashes, payload manifest와 validator result를 non-cyclic DAG로 hash-bind하고 promoted tree에 보존한다. root는 payload/result만 참조하며 final commit/tree를 저장하지 않는다. rejected root는 추가로 canonical rollback contract SHA-256을 참조하되 contract는 candidate authority와 exact path/blob/deletion set만 bind하고 아직 없는 rollback-source/final authority를 기록하지 않는다.
- ABI mutation fixture와 Kotlin/Java source/erased signature compile-negative fixture가 모두 기대한 failure를 보인다. wrong baseline authority와 class/ordinary method/target/bridge 각 transition mutation도 모두 거부되며, crafted/pre-existing wire fixture가 exception과 no-unauthorized-class-loading 경계를 증명한다.
- allocation claim 또는 neutral/negative verdict가 production factory와 generic copied control의 동일-payload 비교 및 두-run fail-closed evidence로 검증된다.
- targeted unit/integration/benchmark tests, compatibility check, Detekt/static checks,
  `git diff --check`, 관련 module build가 fresh evidence로 통과한다.
- promoted evidence archive, new run identities, verified delivery manifest와 regenerated report가 promoted-tree validator를 통과한다. fully validated staging은 고유한 non-existing immutable generation으로 atomic rename되고 기존 generation을 보존한다. active-generation pointer는 temp+fsync+atomic rename과 previous-value CAS로 교체되며 모든 interruption stage, idempotent resume와 owned-staging-only cleanup을 통과한다. tracked Git commit/tree가 최종 authority다.
- rejected terminal은 candidate measurement-source의 기능-test 통과 기록, tracked-tree/JAR hash와 negative evidence를 보존한다. 그 descendant인 immutable clean rollback-source는 canonical approved rollback contract의 exact path/pre-post blob/deletion set과 일치하고 optimized subtype/dispatch, retained-only ABI delta, private writer seam과 implementation-only test를 제거한다. rollback-source의 relevant ABI는 baseline과 exact-equal이고 subtype absence와 generic compatibility/security functional-test 통과를 증명한다.
- issue-linked PR의 repo/base/head, milestone, labels, assignee, final DoD heading, exact head가 live 상태와 일치한다. retained external verifier는 final head가 measurement-source를 descend하고 그 이후 exact docs/evidence allowlist 밖 input drift가 없음을 증명한다. rejected external verifier는 final head가 rollback-source를 descend하고 그 이후 같은 drift가 없음을 증명한다. candidate-to-rollback production 변경은 canonical rollback contract로만 승인된다. tracked payload/root는 final head/tree를 기록하지 않는다.
- merge, release, publish, tag, destructive cleanup은 각각 별도 승인 경계에 남는다.
- release 이후 rollback 검증은 generic dispatch만 복원하고 published retained raw flag set/effective ABI를 유지한다. published artifact authority와
  `published-retained-vs-recovery` mode, pinned baseline/observation windows, thresholds, metric queries, supported release, owner/escalation과 deployment evidence를 만족한다. public ABI 제거는 별도 compatibility/migration 승인 없이는 수행하지 않는다.

## 13. Spec review convergence

이전 round에서 caller, API/developer, stability, security, performance, operator 여섯 관점과 code/architecture adversarial 검토를 수행해 당시 findings를 반영했다. 이후 추가 Ops/developer findings로 terminal provenance와 promotion 계약을 다시 수정했다. 아래 표는 현재 설계에 적용한 disposition 이력이며 최신 수정 영향의 새 독립 rereview 완료를 뜻하지 않는다.

| Review concern                                     | Disposition                                                                                                                                                                                                                                                         |
|----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| nullable target compatibility                      | null-first no-op와 Kotlin/Java regression으로 반영                                                                                                                                                                                                                  |
| detached/custom NIO view와 composite 안정성        | NIO path를 기각하고 absolute `ByteBuf` writer로 교체                                                                                                                                                                                                                |
| subtype 및 writer seam 노출                        | private nested subtype/constructor/dependency와 source seam 1개로 제한                                                                                                                                                                                              |
| default allowlist와 custom-prefix caller           | zero-migration 문구를 제한하고 custom path를 명시                                                                                                                                                                                                                   |
| failure injection과 resource contract              | partial-write private seam, commit-after-success, marks/refCnt 검증 반영                                                                                                                                                                                            |
| JMH reset, observation, threshold, identity        | exact method/reset/Blackhole, 5%+8 B/op+uncertainty, full metadata 반영                                                                                                                                                                                             |
| evidence promotion과 운영 rollback                 | immutable archive/manifest/tree validation, owner/trigger/revert 절차 반영                                                                                                                                                                                          |
| issue lifecycle                                    | merge-ready와 post-merge #757/#898/#756 closure를 분리                                                                                                                                                                                                              |
| regression terminal과 unconditional DoD 충돌       | accepted/inconclusive/rejected terminal별 dispatch, claim, issue disposition을 분리                                                                                                                                                                                 |
| benchmark project dependency와 no-dependency 충돌  | existing Lettuce benchmark-only dependency만 허용하고 external/production dependency는 금지                                                                                                                                                                         |
| allocation uncertainty input                       | `gc.alloc.rate.norm.score/scoreError`와 finite/range fixture를 고정                                                                                                                                                                                                 |
| serialization failure compatibility                | pack failure wrapping과 raw Netty target failure 경계를 분리                                                                                                                                                                                                        |
| ABI/seam/read-only/archive 반복성                  | paired structural/verbose javap, exact 3 bridge flags, compile/reflection, private reflection seam, read-only case, non-recursive archive를 명시                                                                                                                    |
| open class의 compiler bridge overrideability       | bridge raw `0x1041 -> 0x1041`은 불변이고 class `0x0031 -> 0x0021`이 effective overrideability를 활성화한다고 정정                                                                                                                                                   |
| final class/Java conversion 대안                   | public cross-module seam/API break 및 Kotlin metadata/Companion/property/source drift가 더 커서 기각                                                                                                                                                                |
| ABI evidence provenance                            | baseline을 `4ee03eb...`/`086f83b...`, candidate를 clean detached measurement source로 고정하고 distinct roots, FQCN/role, class/JAR-entry/raw hash와 same-classfile binding을 payload로 검증                                                                        |
| compiler/toolchain drift                           | Kotlin/Gradle/JDK와 exact `JAVA_HOME/bin/javap` version/path/hash equality, drift 시 spec/plan reapproval로 고정                                                                                                                                                    |
| bridge mutation/source visibility                  | public/bridge/synthetic/final/extra-member mutation과 Kotlin/Java source·erased compile-negative fixture로 fail-closed                                                                                                                                              |
| crafted decode security                            | out-of-prefix typeUrl, malformed/pre-existing wire, exception parity와 no unauthorized class loading으로 고정                                                                                                                                                       |
| post-release rollback ABI 충돌                     | generic dispatch-only revert, published open ABI 보존, issue reopen/evidence refresh/recovery close 조건으로 분리                                                                                                                                                   |
| raw flag transition 오판                           | class, ordinary method 7개, unchanged target, unchanged bridge 3개와 all-other exact equality를 fresh same-toolchain `javap -v` 숫자로 열거                                                                                                                         |
| retained measurement source와 final head 순환 참조 | payload는 clean detached measurement-source만 bind하고 external verifier가 final ancestry와 measurement-source 이후 production/build/test/benchmark 무변경을 검증                                                                                                   |
| rejected rollback lineage와 final authority        | candidate measurement-source descendant인 immutable clean rollback-source를 canonical exact path/blob/deletion contract에 bind하고 baseline ABI equality, subtype absence, functional tests를 검증한 뒤 external verifier가 rollback-source 이후 final drift를 차단 |
| payload/root hash cycle                            | payload -> class/JAR/raw, result -> payload, root -> payload/result와 rejected rollback contract, Git tree -> root의 비순환 DAG로 고정하고 contract의 rollback/final self-reference를 금지                                                                          |
| standalone class와 JAR 불일치                      | exact JAR entry를 추출해 standalone class SHA와 같음을 요구하거나 validated JAR를 직접 javap                                                                                                                                                                        |
| dirty/stale build provenance                       | distinct detached checkout, pre/post clean receipt, empty build root와 stale/untracked input 거부로 고정                                                                                                                                                            |
| evidence replacement 중단                          | fully validated staging을 non-existing immutable generation으로만 rename하고 old generation을 유지하며 active pointer 하나만 temp+fsync+atomic rename/CAS로 교체; Git tree를 최종 authority로 고정                                                                  |
| recovery authority/관측 모호성                     | published GAV/JAR/release identity, published-retained-vs-recovery ABI mode와 metric/query/window/threshold/deployment checklist로 고정                                                                                                                             |

위 findings를 모두 반영한 최신 artifact를 caller, API/developer, stability, security, performance, operator 여섯 독립 관점에서 다시 검토했다. 각 관점의 최종 결과는 P0=0, P1=0, P2=0, P3=0이며 설계 convergence는 `APPROVED`다. 이 판정은 구현 검증이나 final exact-head merge-ready 검증을 대신하지 않는다.
