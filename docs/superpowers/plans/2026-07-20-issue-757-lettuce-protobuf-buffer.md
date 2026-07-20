# Issue #757 Lettuce Protobuf Buffer Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** uncompressed strict/trusted Lettuce Protobuf factory가 caller-owned `ByteBuf`에 bounded absolute-index 방식으로 직접 기록하도록 하되, 기존 wire/security/failure/ABI 계약을 유지하고 두 번의 fresh JMH evidence로 유지 또는 제거 결정을 내린다.

**Architecture:** generic `LettuceBinaryCodec`에는 상속을 위한 최소 extension point만 열고, `LettuceProtobufCodecs` 내부 private subtype이 `ProtoAny.pack` 결과를 bounded `OutputStream` adapter와 `CodedOutputStream`으로 target에 기록한다. 성공 후에만 `writerIndex`를 한 번 commit하며 NIO view, retain/release, payload-sized final `ByteArray` handoff를 사용하지 않는다. benchmark와 evidence validator는 heap/direct copied/optimized matrix를 검증하고 `retained-accepted`, `retained-inconclusive`, `rejected-after-regression` 중 하나를 결정한다.

**Tech Stack:** Kotlin 2.3, Java 21, Gradle, Lettuce 7.6, Netty 4.2, Protobuf Java 4.35, JUnit 5, MockK, Testcontainers, kotlinx-benchmark/JMH, Python `unittest`.

---

## 0. 실행 계약

- **승인 기준 명세:** `docs/superpowers/specs/2026-07-20-issue-757-lettuce-protobuf-buffer-design.md`
- **작업 위치:** `.worktrees/feat-issue-757-lettuce-protobuf-buffer`
- **브랜치:** `feat/issue-757-lettuce-protobuf-buffer`
- **PR target:** `bluetape4k/bluetape4k-projects`, base `develop`, head `feat/issue-757-lettuce-protobuf-buffer`
- **범위:** `infra/lettuce`, `io/protobuf`, 기존 `benchmark/protobuf-codec-benchmark`, issue #757 evidence/docs만 변경한다.
- **금지:** 새 module, external/production dependency, generic serializer SPI, compressed/custom-prefix/decode optimization, settings/catalog/release/tag/publish 변경.
- **허용 dependency:** benchmark module의 existing project `:bluetape4k-lettuce`에 대한 `implementation` dependency만 추가한다.
- **테스트 순서:** unit/compile은 병렬화할 수 있지만 Redis Testcontainers와 canonical JMH는 다른 container/heavy work와 병렬 실행하지 않는다.
- **커밋:** 각 task는 작은 Lore commit으로 끝낸다. intent line은 why를 쓰고 `Constraint`, `Rejected`, `Confidence`, `Scope-risk`, `Directive`, `Tested`, `Not-tested` trailer를 실제 결과에 맞게 기록한다.
- **중단점:** Task 12에서 PR을 exact-head merge-ready로 보고한 뒤 fresh merge 승인을 기다린다. merge, release, publish, tag, branch/worktree 삭제는 이 계획의 자동 실행 범위가 아니다.

## 1. Task map

| Task | 복잡도 | 의존 | 독립 write scope | 핵심 검증 | terminal/rollback |
|---|---:|---|---|---|---|
| 1. ABI extension point | M | 없음 | `infra/lettuce` codec와 focused tests | Kotlin/Java compile, baseline/candidate `javap` | rejected면 원복 |
| 2. bounded writer | H | 1 | `LettuceProtobufCodecs.kt`, new unit test | heap/direct/composite/wrapped/failure/resource | rejected면 subtype/test 제거 |
| 3. factory/security/integration | H | 2 | existing Protobuf Lettuce tests | strict/trusted/fallback/wire/Redis | retained/rejected 공통 compatibility |
| 4. ABI/source compatibility proof | M | 2-3 | Java fixture, evidence commands | descriptor/reflection/source compile | 허용 diff 외 blocker |
| 5. Lettuce JMH matrix | H | 3 | benchmark Kotlin/Gradle/tests | four cells, semantic/reset/ownership | candidate measurement 준비 |
| 6. fail-closed verdict | H | 5 | validator와 Python tests | score/error/unit/matrix/terminal | deterministic precedence |
| 7. archive-aware evidence | H | 6 | runner와 Python tests | no-clobber, non-recursive archive, manifest | repeated replacement 안전성 |
| 8. candidate exact-head gate | M | 1-7 | 검증/commit only | tests, detekt, clean tree, JAR hash | immutable measurement commit |
| 9. two-run measurement | H | 8 | build evidence only | two sequential canonical runs | terminal 선택 |
| 10. terminal finalization | H | 9 | source/tests/rollback bundle | retained keep 또는 rejected remove | final source shape 결정 |
| 11. promotion/docs | H | 10 | evidence/report/docs/KDoc/changelog | promoted-tree/report/docs parity | non-closing rejected 지원 |
| 12. final review/PR | H | 11 | lesson/review/PR metadata | full build, six-lens review, exact head | merge approval에서 정지 |

## Task 1: 최소 ABI extension point를 TDD로 연다

**Files:**

- Modify: `infra/lettuce/src/main/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodec.kt`
- Create: `infra/lettuce/src/test/kotlin/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecTargetTest.kt`
- Create: `infra/lettuce/src/test/java/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecJavaCompatibilityFixture.java`
- Create: `infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py`
- Create: `infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py`

**Skills:** `test-driven-development`, `bluetape-kotlin-patterns`.

- [ ] **Step 1: baseline bytecode를 ignored evidence에 고정한다.**

```bash
mkdir -p .omx/evidence/issue-757-lettuce
./gradlew :bluetape4k-lettuce:clean :bluetape4k-lettuce:compileKotlin --no-build-cache
javap -classpath infra/lettuce/build/classes/kotlin/main -p -s \
  io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec \
  > .omx/evidence/issue-757-lettuce/lettuce-binary-codec-baseline.javap
```

Expected: public constructor descriptor `(Lio/bluetape4k/io/serializer/BinarySerializer;)V`, existing method descriptors, final class/target overload 상태가 기록된다.

- [ ] **Step 2: ABI validator fixture와 상속/기존 caller 호환 RED tests를 쓴다.**

normalized `javap` validator fixture tests는 임시 directory에 baseline/candidate text를 만들고
CLI exit code와 diagnostic을 함께 assertion한다. 최소 fixture matrix는 다음과 같다.

| mode | candidate 변화 | expected |
|---|---|---|
| `retained` | class `final` 제거 + target overload `final` 제거만 | 0 |
| `retained` | constructor/property/다른 method descriptor 또는 access 변화 | non-zero |
| `retained` | 허용한 두 `final` 중 하나만 제거하거나 다른 `final` 제거 | non-zero |
| `rejected` | baseline과 normalized ABI exact equality | 0 |
| `rejected` | class/target `final` 제거가 남거나 descriptor/access 변화 | non-zero |

fixture는 compiler noise와 declaration 순서 차이는 normalize하지만 class access, member name,
descriptor, access/final flag를 버리지 않는다. `retained`와 `rejected` 모두 positive/negative
case를 갖고 diagnostic에 mode와 첫 mismatch가 포함되는지 확인한다.

Kotlin test에서 한 인자 constructor, null target no-op, prefix/write behavior, estimate/toString을 고정하고 다음 test subclass를 추가한다.

```kotlin
private class RecordingCodec(
    serializer: BinarySerializer,
): LettuceBinaryCodec<Any>(serializer) {
    var targetCalls = 0

    override fun encodeValue(value: Any, target: ByteBuf?) {
        targetCalls++
        super.encodeValue(value, target)
    }
}
```

Java fixture는 기존 생성/호출 형태를 컴파일한다.

```java
static LettuceBinaryCodec<Object> newCodec() {
    return new LettuceBinaryCodec<>(new JdkBinarySerializer());
}

static void encode(LettuceBinaryCodec<Object> codec, Object value, ByteBuf target) {
    codec.encodeValue(value, target);
}
```

- [ ] **Step 3: Python validator와 Kotlin extension의 RED를 각각 확인한다.**

```bash
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
./gradlew :bluetape4k-lettuce:test \
  --tests "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecTargetTest" \
  --rerun-tasks --no-configuration-cache
```

Expected: Python은 validator script/module이 아직 없어 실패하고, Gradle은 class와 target
overload가 final이라 test subclass compile이 실패한다. 두 failure log를 각각 보존한다.

- [ ] **Step 4: normalized ABI validator를 구현하고 fixture GREEN을 만든다.**

validator CLI는 `--baseline`, `--candidate`, `--mode retained|rejected`를 필수로 받고
읽기/parse/mode 오류와 ABI mismatch를 모두 non-zero로 종료한다. `rejected`는 normalized
class/member ABI exact equality만 허용한다. `retained`는 baseline의 모든 declaration과
descriptor/access를 보존하면서 정확히 class와 `encodeValue(V, ByteBuf?)` target overload의
`final` flag 제거만 허용한다. name substring이나 raw line count로 판정하지 않는다.

```bash
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
```

Expected: retained/rejected positive와 negative fixture가 모두 통과한다.

- [ ] **Step 5: class와 target overload만 `open`으로 만든다.**

```kotlin
open class LettuceBinaryCodec<V: Any>(
    val serializer: BinarySerializer,
): RedisCodec<String, V>, ToByteBufEncoder<String, V> {
    // Existing methods remain unchanged.
    open override fun encodeValue(value: V, target: ByteBuf?) {
        target?.run { writeBytes(serializer.serialize(value)) }
    }
}
```

`serializer`, constructor, `encodeValue(value): ByteBuffer`, decode, estimate, key paths는 새로 `open` 처리하지 않는다.
target overload KDoc에 null no-op, caller ownership, success-only index commit, failure 후 attempted
range 처리, subclass compatibility 책임을 영어로 함께 고정한다.

- [ ] **Step 6: Kotlin/Java GREEN과 candidate bytecode의 retained gate를 확인한다.**

```bash
./gradlew :bluetape4k-lettuce:test \
  --tests "io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecTargetTest" \
  --rerun-tasks --no-configuration-cache
javap -classpath infra/lettuce/build/classes/kotlin/main -p -s \
  io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec \
  > .omx/evidence/issue-757-lettuce/lettuce-binary-codec-candidate.javap
diff -u .omx/evidence/issue-757-lettuce/lettuce-binary-codec-baseline.javap \
  .omx/evidence/issue-757-lettuce/lettuce-binary-codec-candidate.javap || true
python3 infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py \
  --baseline .omx/evidence/issue-757-lettuce/lettuce-binary-codec-baseline.javap \
  --candidate .omx/evidence/issue-757-lettuce/lettuce-binary-codec-candidate.javap \
  --mode retained
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
```

raw diff는 표시용이고 normalized validator가 최종 gate다. Expected: descriptor 제거/변경
없음. 허용 차이는 class와 target overload의 final 제거뿐이다.

- [ ] **Step 7: Lore commit.** Intent: `Allow only the Lettuce target encoder to specialize safely`.

## Task 2: private bounded absolute-index writer를 TDD로 구현한다

**Files:**

- Modify: `io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt`
- Create: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufByteBufCodecTest.kt`

**Skills:** `test-driven-development`, `bluetape-kotlin-patterns`.

- [ ] **Step 1: direct path RED matrix를 작성한다.**

다음 target을 parameterized fixture로 만든다.

```kotlin
enum class TargetKind { HEAP, DIRECT, COMPOSITE, WRAPPED, SLICED }

data class TargetFixture(
    val kind: TargetKind,
    val buffer: ByteBuf,
    val release: () -> Unit,
)
```

각 fixture에서 non-zero prefix, reader/writer mark, `refCnt`, exact writer delta, generic codec wire equality, 양방향 decode를 검증한다. direct path가 아직 없어 payload-sized copied path를 구별하는 reflection/private subtype assertion이 RED여야 한다.

fixture마다 allocation owner와 derived view의 release 책임을 따로 기록한다. codec call 뒤
`refCnt` 보존을 assertion하고, 이후 assertion이 실패해도 `finally`에서 owner를 정확히 한
번 release해 최종 `refCnt == 0`을 확인한다. slice/composite component를 owner와 중복
release하지 않는다.

- [ ] **Step 2: bounded adapter 실패 tests를 먼저 추가한다.**

테스트는 factory 반환 객체의 private nested class constructor와 private writer interface를
reflection으로 찾고 `Proxy`로 두 번째 parameter를 교체한다. visibility를
`internal`/public으로 넓히지 않는다. stored Kotlin function type은 primitive `Int`를
boxing하므로 사용하지 않는다.

```kotlin
val constructor = codec.javaClass.declaredConstructors.single { candidate ->
    !candidate.isSynthetic &&
        candidate.parameterCount == 2 &&
        candidate.parameterTypes[0] == ProtobufSerializer::class.java &&
        candidate.parameterTypes[1].declaredMethods.single().name == "write"
}.apply { isAccessible = true }
val writerType = constructor.parameterTypes[1]
val shortWriter = Proxy.newProxyInstance(
    writerType.classLoader,
    arrayOf(writerType),
) { proxy, method, arguments ->
    when (method.name) {
        "write" -> {
            requireNotNull(arguments)
            val target = arguments[1] as ByteBuf
            val start = arguments[2] as Int
            target.setByte(start, 1)
            1
        }
        "toString" -> "ShortPackedAnyWriter"
        "hashCode" -> System.identityHashCode(proxy)
        "equals" -> proxy === arguments?.singleOrNull()
        else -> error("Unexpected writer method: ${method.name}")
    }
}
val injected = constructor.newInstance(serializer, shortWriter) as LettuceBinaryCodec<Any>
```

포함할 failure cases:

- partial absolute write 후 exception
- `size - 1` short-success
- max capacity 부족
- read-only target의 `ReadOnlyBufferException`
- released target의 Netty reference-count exception
- null target은 mock `ProtoMessage`의 어떤 method도 호출하지 않는 no-op
- `ProtoAny.pack` 실패의 기존 `BinarySerializationException` message/cause
- `nioBufferCount()==1`이지만 `nioBuffer*` 호출 시 실패하는 spy target

각 실패에서 reader/writer indices, reset으로 확인 가능한 marks, `refCnt`를 검증하고 capacity/attempted bytes는 rollback assertion에서 제외한다.

- [ ] **Step 3: RED를 실행한다.**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest" \
  --rerun-tasks --no-configuration-cache
```

Expected: private optimized subtype/writer가 없어 실패한다.

- [ ] **Step 4: private subtype과 bounded writer를 최소 구현한다.**

세 declaration은 모두 `object LettuceProtobufCodecs` 안의 private nested declarations로
유지한다.

```kotlin
object LettuceProtobufCodecs {
    private fun interface PackedAnyWriter {
        fun write(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int
    }

    private object AbsolutePackedAnyWriter: PackedAnyWriter {
        override fun write(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int =
            writePackedAny(packed, target, start, end)
    }

    private class DirectProtobufLettuceCodec<V: Any> private constructor(
        serializer: ProtobufSerializer,
        private val writer: PackedAnyWriter = AbsolutePackedAnyWriter,
    ): LettuceBinaryCodec<V>(serializer) {
        companion object {
            fun <V: Any> create(serializer: ProtobufSerializer): LettuceBinaryCodec<V> =
                DirectProtobufLettuceCodec(serializer)
        }

        override fun encodeValue(value: V, target: ByteBuf?) {
            if (target == null) return
            if (value !is ProtoMessage) return super.encodeValue(value, target)

            val packed = try {
                ProtoAny.pack(value)
            } catch (failure: Throwable) {
                throw BinarySerializationException(
                    "Fail to serialize. graphType=${value.javaClass.name}",
                    failure,
                )
            }
            val size = packed.serializedSize
            val start = target.writerIndex()
            target.ensureWritable(size)
            val written = writer.write(packed, target, start, start + size)
            check(written == size) { "Packed Any writer wrote $written bytes, expected $size" }
            target.writerIndex(start + size)
        }
    }
}
```

`writePackedAny`는 NIO API를 호출하지 않고 bounded `OutputStream`을 사용한다.

```kotlin
private fun writePackedAny(packed: ProtoAny, target: ByteBuf, start: Int, end: Int): Int {
    val output = BoundedByteBufOutputStream(target, start, end)
    val coded = CodedOutputStream.newInstance(output, 0)
    packed.writeTo(coded)
    coded.flush()
    return output.written
}
```

adapter의 `write(Int)`와 `write(ByteArray, offset, length)`는 Java array bounds와 `[start,end)`를 먼저 검사한 뒤 `setByte`/`setBytes`만 호출한다. target index, mark, ref-count를 직접 변경하거나 target/writer를 field 밖으로 escape시키지 않는다.

- [ ] **Step 5: factory dispatch를 uncompressed strict/trusted에만 연결한다.**

```kotlin
private val strictSerializer: ProtobufSerializer by lazy { ProtobufSerializer() }
private val trustedInternalSerializer: ProtobufSerializer by lazy {
    ProtobufSerializer.trustedInternalProtobuf()
}

fun <V: Any> protobuf(): LettuceBinaryCodec<V> =
    DirectProtobufLettuceCodec.create(strictSerializer)

fun <V: Any> trustedInternalProtobuf(): LettuceBinaryCodec<V> =
    DirectProtobufLettuceCodec.create(trustedInternalSerializer)
```

compressed factories는 기존 generic `LettuceBinaryCodec(CompressableBinarySerializer(...))` 그대로 둔다.
같은 task에서 factory KDoc에 uncompressed optimized target 범위, payload-sized handoff 제거,
zero-copy 비보장, compressed/custom-prefix compatibility path를 영어로 기록한다.

- [ ] **Step 6: GREEN, static check, no-NIO scan.**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest" \
  --rerun-tasks --no-configuration-cache
rg -n "nioBuffer|nioBuffers|internal class Direct|public class Direct" \
  io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecs.kt
git diff --check
```

Expected: tests PASS; production direct writer에는 NIO call과 새 public/internal seam이 없다.
`javap -c -p`로 writer invocation descriptor가 `(ProtoAny, ByteBuf, int, int)int`이며 hot call
주변에 `Integer.valueOf`/`intValue` boxing이 없음을 확인한다.

- [ ] **Step 7: Lore commit.** Intent: `Write packed Protobuf bytes without a detached buffer view`.

## Task 3: factory, security, wire, Redis compatibility를 고정한다

**Files:**

- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecsTest.kt`
- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufByteBufCodecTest.kt`

**Skills:** `test-driven-development`, `bluetape-kotlin-patterns`. Testcontainers는 sequential.

- [ ] **Step 1: strict/trusted/compressed factory type assertions를 추가한다.**

reflection으로 uncompressed strict/trusted 두 factory만 같은 private optimized subtype인지
확인한다. Gzip, Deflate, LZ4, Snappy, Zstd의 five compressed pairs/ten factories는 모두
generic codec인지 검증한다. private subtype에 public/protected constructor가 없고
externally accessible nested class가 없음을 확인한다.

- [ ] **Step 2: security/wire control tests를 추가한다.**

```kotlin
val oldCodec = LettuceBinaryCodec<Any>(ProtobufSerializer())
val newCodec = LettuceProtobufCodecs.protobuf<Any>()

// old -> new and new -> old wire compatibility
newCodec.decodeValue(oldCodec.encodeValue(message)) shouldBeEqualTo message
oldCodec.decodeValue(newCodec.encodeValue(message)) shouldBeEqualTo message
```

함께 검증할 항목:

- strict non-Protobuf exception type/message/cause
- trusted `ProtoMessage` direct path와 trusted non-Protobuf Kryo fallback bytes
- default allowlist 밖 decode는 계속 차단
- default strict와 trusted factory가 같은 well-formed outside-prefix `Any`를 거부하고,
  trusted profile도 이 `SecurityException`을 Kryo fallback으로 보내지 않음
- caller-created `ProtobufSerializer(allowedClassPrefixes=...)`를 감싼 generic
  `LettuceBinaryCodec`은 private optimized subtype이 아니며 명시적으로 허용한 custom
  prefix만 decode함
- `encodeValue(value): ByteBuffer`, decode, key encode, estimate 불변
- repeated target invocation에서 prefix/stale byte/index/ref-count drift 없음

- [ ] **Step 3: focused unit tests를 실행한다.**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest" \
  --rerun-tasks --no-configuration-cache
```

- [ ] **Step 4: Redis integration을 단독 실행한다.**

```bash
./gradlew :bluetape4k-protobuf:test \
  --tests "io.bluetape4k.protobuf.serializers.redis.LettuceProtobufCodecsTest" \
  --rerun-tasks --no-configuration-cache
```

Expected: strict/trusted Redis SET/GET/HSET round trip PASS. 동시에 다른 Testcontainers suite를 실행하지 않는다.

- [ ] **Step 5: Lore commit.** Intent: `Preserve Lettuce Protobuf trust and wire contracts`.

## Task 4: source/binary compatibility proof를 완성한다

**Files:**

- Modify: `infra/lettuce/src/test/java/io/bluetape4k/redis/lettuce/codec/LettuceBinaryCodecJavaCompatibilityFixture.java`
- Create: `io/protobuf/src/test/java/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufCodecJavaCompatibilityFixture.java`
- Modify: `io/protobuf/src/test/kotlin/io/bluetape4k/protobuf/serializers/redis/LettuceProtobufByteBufCodecTest.kt`

- [ ] **Step 1: Java/Kotlin caller fixture를 compile/runtime test로 고정한다.**

Java fixture는 `LettuceProtobufCodecs.INSTANCE.protobuf()`와 `trustedInternalProtobuf()`를 기존 반환형으로 받으며 target overload를 호출한다. Kotlin test는 Java fixture를 실행해 null target no-op과 wire bytes를 확인한다.

- [ ] **Step 2: reflection ABI assertions를 추가한다.**

```kotlin
val clazz = LettuceBinaryCodec::class.java
Modifier.isPublic(clazz.modifiers) shouldBeEqualTo true
Modifier.isFinal(clazz.modifiers) shouldBeEqualTo false
clazz.getDeclaredConstructor(BinarySerializer::class.java)
clazz.getMethod("encodeValue", Any::class.java, ByteBuf::class.java)
```

`serializer` getter와 기존 public method descriptors가 남아 있는지 확인한다.

- [ ] **Step 3: compile/test/javap diff를 실행한다.**

```bash
./gradlew :bluetape4k-lettuce:test :bluetape4k-protobuf:compileTestJava \
  :bluetape4k-protobuf:test --no-configuration-cache
javap -classpath infra/lettuce/build/classes/kotlin/main -p -s \
  io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec \
  > .omx/evidence/issue-757-lettuce/lettuce-binary-codec-final-candidate.javap
diff -u .omx/evidence/issue-757-lettuce/lettuce-binary-codec-baseline.javap \
  .omx/evidence/issue-757-lettuce/lettuce-binary-codec-final-candidate.javap || true
python3 infra/lettuce/scripts/validate-lettuce-binary-codec-abi.py \
  --baseline .omx/evidence/issue-757-lettuce/lettuce-binary-codec-baseline.javap \
  --candidate .omx/evidence/issue-757-lettuce/lettuce-binary-codec-final-candidate.javap \
  --mode retained
```

raw diff는 표시용이며 normalized validator의 non-zero가 blocker다. 허용 차이보다 넓은 ABI
변경은 구현을 진행하지 말고 수정한다.

- [ ] **Step 4: Lore commit.** Intent: `Prove existing Lettuce codec callers remain compatible`.

## Task 5: Lettuce heap/direct JMH matrix를 TDD로 추가한다

**Files:**

- Modify: `benchmark/protobuf-codec-benchmark/build.gradle.kts`
- Modify: `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupport.kt`
- Modify: `benchmark/protobuf-codec-benchmark/src/main/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkMetadata.kt`
- Modify: `benchmark/protobuf-codec-benchmark/src/benchmark/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmark.kt`
- Modify: `benchmark/protobuf-codec-benchmark/src/test/kotlin/io/bluetape4k/protobuf/benchmark/ProtobufCodecBenchmarkSupportTest.kt`

**Skills:** `test-driven-development`; benchmark hazard gate 적용.

- [ ] **Step 1: benchmark task 이름을 현재 Gradle에서 확인한다.**

```bash
./gradlew :protobuf-codec-benchmark:tasks --all --no-configuration-cache | \
  rg "benchmarkBenchmark(Compile|Jar|Run|$)"
```

- [ ] **Step 2: exact four-cell matrix RED test를 쓴다.**

```kotlin
val lettuceMethods = setOf(
    "lettuceEncodeHeapCopied",
    "lettuceEncodeHeapOptimized",
    "lettuceEncodeDirectCopied",
    "lettuceEncodeDirectOptimized",
)
```

fixture test는 동일 payload/prefix/start/final capacity/max capacity, output length, wire bytes, decoded message, writer delta, `refCnt`, repeated reset을 검증한다. copied는 `LettuceBinaryCodec(ProtobufSerializer())`, optimized는 production factory를 사용한다.

- [ ] **Step 3: benchmark-only project dependency를 추가한다.**

```kotlin
dependencies {
    implementation(project(":bluetape4k-protobuf"))
    implementation(project(":bluetape4k-redisson"))
    implementation(project(":bluetape4k-lettuce"))
}
```

- [ ] **Step 4: fixture와 benchmark methods를 구현한다.**

trial setup에서 heap/direct `ByteBuf`를 final capacity까지 미리 확장하고 prefix를 쓴다. invocation setup은 allocation 없이 index와 prefix를 복원한다. timed method는 codec call 후 writer index와 encoded range의 first/last-byte checksum만 `Blackhole`에 전달한다.

```kotlin
@Benchmark
fun lettuceEncodeHeapCopied(blackhole: Blackhole) =
    fixture.lettuceEncodeHeapCopied(blackhole)

@Benchmark
fun lettuceEncodeHeapOptimized(blackhole: Blackhole) =
    fixture.lettuceEncodeHeapOptimized(blackhole)
```

direct pair도 동일하게 추가하고 trial teardown에서 fixture-owned buffers를 정확히 한 번 release한다.

- [ ] **Step 5: metadata identity를 갱신한다.**

matrix version을 `issue-757-lettuce-v2`로 올리고 allocator class, heap/direct capacity/maxCapacity, canonical prefix/reader/writer index, expanded method set을 `config_json`/SHA에 포함한다.

- [ ] **Step 6: tests/compile/JAR smoke.**

```bash
./gradlew :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkCompile \
  :protobuf-codec-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache
```

- [ ] **Step 7: Lore commit.** Intent: `Compare Lettuce copied and direct writes under one payload contract`.

## Task 6: allocation uncertainty와 terminal precedence를 fail-closed로 만든다

**Files:**

- Modify: `benchmark/protobuf-codec-benchmark/scripts/validate-jmh.py`
- Modify: `benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py`

**Skills:** `test-driven-development`.

- [ ] **Step 1: malformed metric RED fixtures를 추가한다.**

complete fixture에 `secondaryMetrics["gc.alloc.rate.norm"].scoreError`를 추가하고 missing score/error, wrong unit, zero/negative score, negative error, `NaN`, `Infinity`, duplicate/missing/unexpected Lettuce method를 각각 non-zero failure로 고정한다.

- [ ] **Step 2: mixed-cell verdict RED fixtures를 추가한다.**

다음을 모두 test한다.

- one accepted + one inconclusive → `retained-accepted`, accepted cell만 positive
- both inconclusive → `retained-inconclusive`
- one regression + one accepted/inconclusive → `rejected-after-regression`
- relative 5%는 통과하지만 absolute 8 B/op 미달 → inconclusive
- uncertainty interval overlap → inconclusive
- error field가 바뀐 두 run identity mismatch → failure가 아니라 metric-specific comparison input; execution identity는 동일해야 함
- raw JMH `scoreError`가 per-run summary CSV와 comparison CSV를 거쳐 verdict formula까지
  byte-for-byte 전달됨
- allocator class, heap/direct capacity·maxCapacity, canonical reader/writer index 중 하나가
  다르면 config/two-run identity failure

- [ ] **Step 3: RED를 실행한다.**

```bash
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
```

- [ ] **Step 4: metric model과 formula를 구현한다.**

```python
rows[method] = {
    "throughput": validate_positive_finite(primary["score"], ...),
    "allocation": validate_positive_finite(allocation["score"], ...),
    "allocation_error": validate_non_negative_finite(allocation["scoreError"], ...),
    ...,
}
```

`SUMMARY_FIELDS`에 `allocation_error_bytes_per_operation`을 추가하고 summary reader가 이를
finite/non-negative float로 복원한다. `COMPARISON_FIELDS`에는 run A/B candidate와 baseline
각각의 allocation error를 추가한다. compare formula는 raw JMH를 다시 읽지 않고 이
state-bound summary fields만 사용하며, end-to-end fixture가 raw JSON → summary CSV →
comparison CSV → validation verdict를 검증한다.

`CONFIG_KEYS`와 metadata type validation에는 allocator class, heap/direct capacity와
maxCapacity, canonical reader/writer index를 추가한다. copied/optimized pair가 같은 target
fixture identity를 공유하는지 Kotlin fixture와 Python config validator 양쪽에서 확인한다.

positive formula:

```python
candidate <= baseline * 0.95
baseline - candidate >= 8.0
candidate + candidate_error < baseline - baseline_error
```

regression formula는 대칭이다. `ROLLBACK_DISPATCH_CELLS`에 `lettuce_encode`와 heap/direct optimized cells를 추가하고 delivery terminal은 regression 우선, 다음 accepted, 나머지 inconclusive로 집계한다. compare validation과 runner state에 canonical `delivery_terminal` field를 기록한다.

- [ ] **Step 5: Python tests와 Gradle fixture를 GREEN으로 만든다.**

```bash
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
./gradlew :protobuf-codec-benchmark:test --no-configuration-cache
```

- [ ] **Step 6: Lore commit.** Intent: `Reject ambiguous Lettuce allocation evidence`.

## Task 7: promoted evidence replacement를 immutable/non-recursive로 만든다

**Files:**

- Modify: `benchmark/protobuf-codec-benchmark/scripts/run-evidence.py`
- Modify: `benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py`

**Skills:** `test-driven-development`. 이 task는 파일 lifecycle 테스트만 수행하고 live promoted tree를 아직 바꾸지 않는다.

- [ ] **Step 1: repeated replacement RED fixtures를 추가한다.**

temporary repo fixture에서 다음을 검증한다.

- 기존 active generation regular files가 `archive/<old-delivery-commit>/`에 한 번 복사됨
- 기존 `archive/` subtree를 새 archive 아래 재귀 복사하지 않음
- 이전 manifest의 `superseded_evidence` path/hash가 carry-forward됨
- new archive entry는 relative path, file hashes, file-set hash를 가짐
- symlink, missing/extra file, nested `archive/**/archive`, hash drift, destination collision은 실패
- legacy rollback archive는 해당 generation에 한 번만 포함됨
- replacement 실패 시 기존 destination 복원, cleanup 가능한 unique backup만 별도 유지
- successful replacement manifest의 unique `replacement_id`로 exact backup path를 다시
  계산하며, unrelated/missing/colliding backup이나 expected-head mismatch에서는 cleanup 실패
- rebase recovery는 manifest의 delivery tree hash와 같은 commit을 current `HEAD` ancestry에서
  정확히 하나만 선택하며 zero/multiple match, non-ancestor, manifest/selection hash drift를 거부
- `lettuce_encode`가 parser/decision ordering/cell mapping/source predicate에 없으면 실패
- candidate commit에서 `lettuce_encode` rollback을 준비하고 subtype과 `open` ABI를 제거한
  committed head에서만 finalize가 성공함
- candidate verification command 하나라도 실패하면 receipt가 생성되지 않음
- receipt의 commit/tree/JAR/log hash drift 또는 rollback bundle 누락을 거부함
- receipt/output/log root의 symlink, traversal, absolute/duplicate/extra file, unauthorized root,
  output collision을 거부함
- unrelated committed manifest로 다른 evidence destination replacement를 승인할 수 없음
- raw argument 또는 ancestor component가 symlink인 manifest/destination/backup root를
  `resolve()` 전에 거부함
- carried path/file key의 absolute path, `..`, duplicate, non-canonical separator,
  `archive/<validated-commit>/` 밖 경로를 거부함
- swap 후 semantic/manifest verification 실패 시 destination과 state를 이전 generation으로
  transactional restore하고 downstream promotion을 중단함

- [ ] **Step 2: archive model helper를 구현한다.**

```python
def active_generation_files(root):
    return sorted(
        path for path in root.rglob("*")
        if path.is_file() and "archive" not in path.relative_to(root).parts
    )
```

실제 구현은 symlink-free regular file 검증을 재사용하고, manifest에 다음 shape를 기록한다.

```json
{
  "superseded_evidence": [
    {
      "delivery_commit": "0123456789abcdef0123456789abcdef01234567",
      "path": "archive/0123456789abcdef0123456789abcdef01234567",
      "files": {"relative/file": "sha256"},
      "file_set_sha256": "sha256"
    }
  ]
}
```

- [ ] **Step 3: `replace-promoted` lifecycle을 갱신한다.**

기존 committed manifest를 먼저 validate하고, staging에 새 state evidence를 복사한 뒤 이전 active file set과 carried entries를 추가한다. staging semantic/archive validation이 모두 통과한 뒤에만 atomic replacement한다. `verify-promoted`, `validate-committed`, report hash input도 `superseded_evidence`를 포함한다.

pre-mutation authorization guard는 raw path를 resolve하기 전에 모든 ancestor의 symlink를
거부하고 다음 invariant를 요구한다.

```python
expected_manifest == destination / "delivery-manifest.json"
destination.is_relative_to(repo_root / "docs/benchmarks/raw")
backup_root.is_relative_to(repo_root / "benchmark/protobuf-codec-benchmark/build")
```

각 `superseded_evidence.path`는 `archive/<40-or-64-hex-validated-commit>` canonical relative
directory이고, file key도 그 subtree 아래의 unique canonical relative regular file이어야
한다. source, staging, destination, backup의 resolved path가 승인 root 아래에 남는지 복사와
rename 직전에 다시 확인한다.

같은 runner에서 rejected terminal이 실행 가능하도록 다음 mapping과 removal proof도 추가한다.

```python
DISPATCH_ORDER = (
    "serializer_encode",
    "serializer_decode",
    "redisson_contiguous",
    "lettuce_encode",
)
DISPATCH_CELLS["lettuce_encode"] = (
    "lettuceEncodeHeapOptimized",
    "lettuceEncodeDirectOptimized",
)
DISPATCH_SOURCE_PATHS["lettuce_encode"] = (
    "io/protobuf/src/main/kotlin/io/bluetape4k/protobuf/serializers/redis/"
    "LettuceProtobufCodecs.kt"
)
```

`verify_dispatch_removal`의 Lettuce case는 final committed head에서 private optimized subtype과
writer가 없고 두 uncompressed factory가 generic codec으로 복원됐으며
`LettuceBinaryCodec` class/target overload가 다시 final인지 source와 candidate-baseline
`javap` proof와 같은 validator의 `--mode rejected`로 확인한다. `retained` mode는 class와
target overload의 final 제거만 허용하고 `rejected` mode는 baseline과 exact ABI equality를
요구한다. predicate는 단순 substring 하나가 아니라 필요한 복원 조건을
모두 검사하고, archived candidate commit/tree/JAR identity는 rollback bundle에 유지한다.

candidate 기능 검증을 manifest-bound evidence로 만들기 위해 runner에
`verify-candidate --evidence-root PATH --selection-state FILE` command를
추가한다. 이 command는 clean
committed HEAD에서 Task 8의 focused tests, Lettuce full build, Protobuf full build,
benchmark/Python/static checks를 순차 실행하고 command argv, exit code, bounded log
file/hash, commit, tree, benchmark JAR SHA-256, authorized baseline `javap` hash와 retained
ABI validator result를 canonical receipt에 기록한다. 하나라도
실패하면 receipt를 emit하지 않는다. `resolve-jar --verification-receipt FILE`은 receipt
identity가 현재 clean HEAD/tree/JAR와 일치할 때만 state에 path/hash를 bind한다. promotion은
receipt와 logs를 regular files로 복사하고 delivery manifest에 path/hash를 기록한다.
rollback preparation/bundle은 candidate receipt 전체를 immutable archive에 포함하며 final
rejected manifest가 그 archive path/hash를 재검증한다.

verification output은 repo의 exact authorized root `.omx/evidence/issue-757-lettuce/` 바로
아래 unique no-clobber directory만 허용한다. raw output-root, receipt, log path와 모든
ancestor를 `resolve()` 전에 symlink 검사한다. receipt의 log paths는 receipt directory
기준 canonical relative path이며 absolute path, `..`, separator ambiguity, duplicate,
unexpected/extra file을 거부한다. `resolve-jar`/`validate-verification`은 receipt directory에서
exact expected command/log file set을 재구성해 regular-file type, successful exit code, hash,
commit/tree/JAR identity와 authorized-root containment를 모두 확인한다. promotion과 rollback은
이 validated file set만 fixed collision-free relative destination으로 복사한다.

runner가 `candidate-{commit}-{run-id}` 또는 `final-rejected-{commit}-{run-id}` unique direct
child를 생성하고 성공 후에만 selection state를 새 receipt path/hash/attempt ID로 atomic
교체한다. 실패한 attempt directory/log는 보존하되 selection state는 바꾸지 않는다. retry
fixture는 첫 attempt를 삭제하지 않고 두 번째 attempt와 rollback root가 충돌 없이 성공함을
검증한다. `record-rollback`은 selected attempt ID로 만든 unique rollback root를 받고 exact
preparation path를 runner state에 기록하며, `finalize-rollback`도 exact bundle path를 state에
기록한다. downstream은 broad `find`를 사용하지 않고 이 state handles만 사용한다.

`verify-final --terminal rejected --evidence-root PATH --selection-state FILE`은 제거 후 clean committed
HEAD에서 direct-writer 전용 tests를 N/A로 제외하고 generic Lettuce/Protobuf full builds,
benchmark/Python/static checks, rejected-mode ABI validator, `verify_dispatch_removal`을 순차 실행해 별도 final receipt를
만든다. post-removal `resolve-jar`는 이 final receipt를 current identity로 bind하고 finalized
rollback bundle을 통해 candidate receipt와 negative evidence를 함께 authenticate한다.

`replace-promoted`는 swap 뒤 `verify_promoted`까지 한 transaction에서 실행한다. post-swap
verification 또는 state write가 실패하면 raw pre-state snapshot과 state-recorded backup을
사용해 이전 destination/state를 복원하고 non-zero로 종료한다. 별도 unrestricted restore
command는 만들지 않는다.

successful manifest에는 delivery commit과 그 exact tree hash, random no-clobber
`replacement_id`, previous active generation file-set hash를 기록한다.
`cleanup-replacement-backup --manifest FILE --expected-head REV
--backup-root PATH`는 committed manifest의 delivery ancestry, `replacement_id`, previous hash와
`backup-root/{replacement_id}` exact symlink-free directory를 검증한 뒤 그 한 directory만
삭제한다. disposable runner state, directory scan, glob, latest-file selection은 사용하지
않으며 mismatch에서는 아무것도 삭제하지 않는다.

tree-equivalent rebase recovery를 위해
`resolve-rebased-delivery --manifest FILE --head REV --selection-state FILE`을 추가한다. 이
command는 manifest의 `delivery.tree_hash`와 같은 tree를 가진 commit을 `REV` ancestry에서
찾아 exactly one match와 ancestor 관계를 검증하고, manifest hash/head/selected commit/tree를
canonical no-clobber selection state에 기록한다. `rebind-rebased-delivery`는 raw `HEAD` 대신
이 selection state를 입력받아 identity를 다시 검증한 뒤 manifest delivery commit만 바꾼다.

- [ ] **Step 4: tests를 실행한다.**

```bash
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
git diff --check
```

- [ ] **Step 5: Lore commit.** Intent: `Keep repeated benchmark evidence replacement auditable`.

## Task 8: candidate measurement exact head를 검증·커밋한다

**Files:** candidate source/test/benchmark files only; promoted docs/evidence는 아직 변경하지 않는다.

- [ ] **Step 1: candidate source를 먼저 Lore commit한다.**

```bash
git diff --check
git status --short
```

remaining source changes를 commit한다. Intent:
`Make the Lettuce allocation candidate reproducible`. 이후 모든 verification은 이 clean exact
head에 귀속한다.

- [ ] **Step 2: candidate verification receipt command를 실행한다.**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-candidate \
  --evidence-root .omx/evidence/issue-757-lettuce \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json \
  --abi-baseline \
  .omx/evidence/issue-757-lettuce/lettuce-binary-codec-baseline.javap
```

runner는 다른 container/heavy work가 없음을 확인하고 아래 argv를 정확히 순차 실행한다.
container-capable Gradle invocation은 모두 `--no-parallel`이다.

```text
./gradlew :bluetape4k-lettuce:test --tests io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecTargetTest --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:test --tests io.bluetape4k.protobuf.serializers.redis.LettuceProtobufByteBufCodecTest --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:test --tests io.bluetape4k.protobuf.serializers.redis.LettuceProtobufCodecsTest --rerun-tasks --no-parallel --no-configuration-cache
./gradlew :bluetape4k-lettuce:clean :bluetape4k-lettuce:build --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:clean :bluetape4k-protobuf:build --no-parallel --no-configuration-cache
./gradlew :protobuf-codec-benchmark:clean :protobuf-codec-benchmark:test :protobuf-codec-benchmark:benchmarkBenchmarkCompile :protobuf-codec-benchmark:benchmarkBenchmarkJar --no-configuration-cache
python3 -m unittest benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
python3 -m unittest infra/lettuce/scripts/test_validate_lettuce_binary_codec_abi.py
./gradlew :bluetape4k-lettuce:detekt :bluetape4k-protobuf:detekt --no-configuration-cache
```

- [ ] **Step 3: receipt와 clean exact head를 검증한다.**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-verification \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json
git status --short
find benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  -maxdepth 1 -type f -name '*.jar' -print
```

Expected: receipt의 commit/tree/JAR/log hashes가 현재 clean HEAD와 일치한다. 이 receipt는
rejected terminal에서도 immutable negative evidence identity다.

## Task 9: canonical JMH를 두 번 순차 실행하고 terminal을 선택한다

**Files:** `benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/**` ignored runtime evidence.

- [ ] **Step 1: 환경 gate.** 다른 heavy work, Gradle daemon build, container suite가 없는지 확인하고 `--concurrent-heavy-work absent`를 사용할 수 있을 때만 진행한다.

- [ ] **Step 2: pinned JAR state를 만든다.**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --jar-dir benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --verification-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json
```

- [ ] **Step 3: canonical profile을 서로 다른 run ID로 두 번 순차 실행한다.**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence
```

- [ ] **Step 4: compare/validation을 실행한다.**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py compare \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --output benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/comparison.csv \
  --validation benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/validation.json
```

- [ ] **Step 5: terminal을 기계적으로 선택한다.**

1. Lettuce eligible cell 하나라도 두 run 모두 symmetric regression → `rejected-after-regression`.
2. 그 외 Lettuce cell 하나라도 두 run 모두 acceptance → `retained-accepted`; positive text는 해당 cell만.
3. 그 외 → `retained-inconclusive`; positive text 금지.

runner state와 comparison validation의 `delivery_terminal`이 같은지 확인한다. committed
source of truth는 후속 manifest/report이며 수동 terminal 파일을 만들지 않는다.

## Task 10: 선택된 terminal로 final source를 만든다

### Branch A — `retained-accepted` / `retained-inconclusive`

- [ ] Task 2-4 production subtype, `open` extension point, direct-writer tests를 유지한다.
- [ ] Task 8 candidate receipt를 `validate-verification`으로 다시 확인한다. production 또는
  benchmark input이 바뀌었다면 기존 receipt를 재사용하지 않고 source를 commit한 뒤 새
  receipt와 Task 9 two-run lifecycle을 처음부터 수행한다.
- [ ] accepted는 accepted cell만 positive, inconclusive는 neutral-only metadata를 사용한다.

### Branch B — `rejected-after-regression`

- [ ] **Step B1: candidate evidence를 immutable rollback preparation으로 기록한다.**

```bash
ISSUE757_ATTEMPT=$(python3 -c \
  'import json; print(json.load(open("benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-candidate-verification.json"))["attempt_id"])')
ISSUE757_ROLLBACK_ROOT=".omx/evidence/issue-757-lettuce/rollback-$ISSUE757_ATTEMPT"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py record-rollback \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json \
  --dispatch lettuce_encode \
  --archive-root "$ISSUE757_ROLLBACK_ROOT"
```

- [ ] **Step B2: optimized subtype, private writer, direct-writer-only tests, class/overload `open`을 제거한다.** target/factory KDoc도 generic copied behavior로 복원한다. generic factory/codec behavior tests, benchmark/validator/archive code, candidate negative evidence identity는 유지한다.

- [ ] **Step B3: Lore commit.** Intent: `Remove the Lettuce dispatch after confirmed allocation regression`. `Rejected` trailer에 measured direct dispatch와 공식/두 run을 기록한다.

- [ ] **Step B4: rollback bundle을 finalize하고 final source를 검증한다.**

```bash
ISSUE757_PREPARATION=$(python3 -c \
  'import json; print(json.load(open("benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json"))["rollback_preparation_path"])')
test -n "$ISSUE757_PREPARATION"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py finalize-rollback \
  --preparation "$ISSUE757_PREPARATION"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py verify-final \
  --terminal rejected \
  --evidence-root .omx/evidence/issue-757-lettuce \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-final-verification.json \
  --abi-baseline \
  .omx/evidence/issue-757-lettuce/lettuce-binary-codec-baseline.javap
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-verification \
  --selection-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-final-verification.json
```

- [ ] **Step B5: finalized bundle로 별도 post-removal state를 만들고 canonical run 두 번을 다시 수집한다.**

```bash
ISSUE757_BUNDLE=$(python3 -c \
  'import json; print(json.load(open("benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json"))["rollback_bundle_path"])')
test -n "$ISSUE757_BUNDLE"
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-jar \
  --jar-dir benchmark/protobuf-codec-benchmark/build/benchmarks/benchmark/jars \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --rollback-bundle "$ISSUE757_BUNDLE" \
  --verification-state \
  benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-final-verification.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py run \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --profile canonical --concurrent-heavy-work absent \
  --output-root benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py compare \
  --state benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json \
  --output benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/comparison.csv \
  --validation benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/validation.json
```

final comparison에서 removed Lettuce cells는 ineligible이고 candidate archive가 authenticated
chain에 남아야 한다. delivery terminal은 post-removal comparison으로 다시 선택하지 않고
authenticated rollback bundle의 `rejected-after-regression` 결정을 유지한다.

### 공통 finalization

- [ ] `git status --short`가 clean인지 확인한다. retained는 Task 8 candidate commit이 final
  source head이고, rejected는 B3 removal commit과 final receipt가 final source head다. evidence
  promotion 전에 추가 source change가 있으면 해당 terminal의 verification/two-run lifecycle을
  새 exact head에서 다시 수행한다.

## Task 11: evidence를 archive-aware promote하고 문서를 동기화한다

**Files:**

- Modify: `docs/benchmarks/raw/issue-757/**`
- Modify: `docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md`
- Modify: `docs/benchmarks/README.md`
- Modify: `benchmark/protobuf-codec-benchmark/README.md`
- Modify: `benchmark/protobuf-codec-benchmark/README.ko.md`
- Modify: `docs/manual/en/modules/protobuf-codec-benchmark.md`
- Modify: `docs/manual/ko/modules/protobuf-codec-benchmark.md`
- Modify: `io/protobuf/README.md`
- Modify: `io/protobuf/README.ko.md`
- Modify: `docs/manual/en/modules/bluetape4k-protobuf.md`
- Modify: `docs/manual/ko/modules/bluetape4k-protobuf.md`
- Modify: `docs/security/serialization-trust-profiles.md`
- Modify: `CHANGELOG.md`

**Skills:** `bluetape-writer` for paired docs. Public KDoc/README English, locale counterpart Korean.

- [ ] **Step 1: existing committed manifest를 expected input으로 archive-aware replacement한다.**

terminal에 맞는 verified state를 먼저 고정한다.

```bash
ISSUE757_TERMINAL=$(python3 -c \
  'import json; print(json.load(open("benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json"))["delivery_terminal"])')
case "$ISSUE757_TERMINAL" in
  retained-accepted|retained-inconclusive)
    ISSUE757_STATE=benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-evidence/jar.json
    ;;
  rejected-after-regression)
    ISSUE757_STATE=benchmark/protobuf-codec-benchmark/build/issue-757-lettuce-post-removal/jar.json
    ;;
  *) exit 1 ;;
esac
test -f "$ISSUE757_STATE"
```

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py replace-promoted \
  --state "$ISSUE757_STATE" \
  --expected-manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --destination docs/benchmarks/raw/issue-757 \
  --backup-root benchmark/protobuf-codec-benchmark/build/replacement-backups
```

`replace-promoted` success 자체가 post-swap `verify_promoted`와 manifest/state write까지
transactional하게 통과했다는 증거다. non-zero면 이전 destination/state가 복원됐는지
`validate-committed`로 확인하고 Task 11 downstream을 중단한다. Expected: 이전 active
generation만 `archive/<old-delivery-commit>/`에 한 번 보존되고 기존 archive는 재귀
복제되지 않는다.

- [ ] **Step 2: manifest에서 report를 재생성·검증한다.**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py render-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --output docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
```

- [ ] **Step 3: terminal별 문구를 적용한다.**

- retained-accepted: accepted cell의 B/op만 manifest-derived positive claim.
- retained-inconclusive: direct behavior는 설명하되 allocation improvement 문구 없음.
- rejected: candidate regression과 dispatch removal을 neutral/negative로 기록; direct-write 완료, `Closes #757`, #898 완료 문구 금지.

README는 숫자를 복제하지 않고 report를 링크한다. Task 1-2에서 측정 전에 고정한 KDoc와
일치하도록 custom-prefix/compressed/decode/fallback compatibility path를 설명하되 production
Kotlin source는 이 task에서 수정하지 않는다.

English/Korean README와 manual pair에 동일한 실행 의미의 예제를 넣는다.

- default allowlist 안 message의 기존 zero-argument Kotlin factory 사용
- trusted-internal opt-in warning과 shared/untrusted boundary에서 사용 금지
- custom prefix `ProtobufSerializer(allowedClassPrefixes=...)`를 generic
  `LettuceBinaryCodec`으로 감싸는 caller-owned compatibility path
- unchanged compressed factory 사용
- Java의 `LettuceProtobufCodecs.INSTANCE.protobuf()` 호출
- target-taking Lettuce `ByteBuf` direct path와 unchanged single-argument `ByteBuffer`
  encode/decode path 구분
- retained terminal은 caller migration 없음, rejected terminal은 generic copied behavior 유지

outside-default-prefix `MyMessage`가 zero-argument factory로 곧바로 round-trip하는 예시는
금지한다.

- [ ] **Step 4: locale/doc parity와 placeholders를 검사한다.**

```bash
rg -n 'TO''DO|T''BD|FIX''ME|PLACE''HOLDER' \
  io/protobuf benchmark/protobuf-codec-benchmark docs/manual docs/benchmarks CHANGELOG.md
./gradlew exportManualModuleInventory --no-configuration-cache
ruby scripts/manual/validate_manuals.rb
ruby scripts/manual/sync_navigation_metadata.rb --check
ruby scripts/manual/export_manifest.rb --check
python3 - <<'PY'
from pathlib import Path

pairs = [
    (
        Path("io/protobuf/README.md"),
        Path("io/protobuf/README.ko.md"),
        ("LettuceProtobufCodecs", "trustedInternalProtobuf", "allowedClassPrefixes",
         "LettuceBinaryCodec", "ByteBuf", "ByteBuffer", "gzipProtobuf", "INSTANCE"),
        True,
    ),
    (
        Path("docs/manual/en/modules/bluetape4k-protobuf.md"),
        Path("docs/manual/ko/modules/bluetape4k-protobuf.md"),
        ("LettuceProtobufCodecs", "trustedInternalProtobuf", "allowedClassPrefixes",
         "LettuceBinaryCodec", "ByteBuf", "ByteBuffer", "gzipProtobuf", "INSTANCE"),
        False,
    ),
    (
        Path("benchmark/protobuf-codec-benchmark/README.md"),
        Path("benchmark/protobuf-codec-benchmark/README.ko.md"),
        ("lettuceEncodeHeapCopied", "lettuceEncodeHeapOptimized",
         "lettuceEncodeDirectCopied", "lettuceEncodeDirectOptimized",
         "gc.alloc.rate.norm", "scoreError", "retained-inconclusive"),
        True,
    ),
    (
        Path("docs/manual/en/modules/protobuf-codec-benchmark.md"),
        Path("docs/manual/ko/modules/protobuf-codec-benchmark.md"),
        ("lettuceEncodeHeapCopied", "lettuceEncodeHeapOptimized",
         "lettuceEncodeDirectCopied", "lettuceEncodeDirectOptimized",
         "gc.alloc.rate.norm", "scoreError", "retained-inconclusive"),
        False,
    ),
]
for english, korean, required, reciprocal_readme in pairs:
    left, right = english.read_text(), korean.read_text()
    assert left.count("```") == right.count("```")
    assert left.count("\n## ") == right.count("\n## ")
    assert left.count("\n### ") == right.count("\n### ")
    assert all(token in left and token in right for token in required)
    if reciprocal_readme:
        assert "](./README.ko.md)" in left
        assert "](./README.md)" in right
PY
git diff --check
```

- [ ] **Step 5: docs/evidence Lore commit.** Intent: `Bind the Lettuce delivery claim to reproducible evidence`.

- [ ] **Step 6: committed manifest를 durable terminal authority로 검증한 뒤 cleanup 가능한 replacement backup만 삭제한다.**

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json
ISSUE757_TERMINAL=$(python3 -c \
  'import json; print(json.load(open("docs/benchmarks/raw/issue-757/delivery-manifest.json"))["delivery_terminal"])')
case "$ISSUE757_TERMINAL" in
  retained-accepted|retained-inconclusive|rejected-after-regression) ;;
  *) exit 1 ;;
esac
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py cleanup-replacement-backup \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --expected-head HEAD \
  --backup-root benchmark/protobuf-codec-benchmark/build/replacement-backups
```

`cleanup-replacement-backup`은 committed manifest와 expected head에서 필요한 source state를
역으로 검증하며 disposable `build/**/jar.json`에 의존하지 않는다. committed `archive/`는
cleanup 대상이 아니다. Task 12의 PR title, issue comment, post-merge disposition도 이 validated
manifest의 `delivery_terminal`만 사용한다.

## Task 12: final verification, review, lesson, push, PR

**Files:**

- Create: `docs/lessons/2026-07-20-issue-757-lettuce-protobuf-buffer.md`
- Create: `docs/review/issue-757-lettuce-protobuf-buffer-review.md`
- Create ignored handoff: `.omx/issue-757-pr-body.md`
- Create ignored handoff: `.omx/issue-757-progress.md`
- GitHub: issue #757 progress update and PR body in English.

**Skills:** `requesting-code-review`, `verification-before-completion`, `bluetape-writer`; PR review는 six independent lanes + main integration.

- [ ] **Step 1: final exact-head local verification.**

```bash
./gradlew :bluetape4k-lettuce:clean :bluetape4k-lettuce:build \
  --no-parallel --no-configuration-cache
./gradlew :bluetape4k-protobuf:clean :bluetape4k-protobuf:build \
  --no-parallel --no-configuration-cache
./gradlew :protobuf-codec-benchmark:clean :protobuf-codec-benchmark:test \
  :protobuf-codec-benchmark:benchmarkBenchmarkJar --no-configuration-cache
python3 -m unittest \
  benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py \
  benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py
./gradlew :bluetape4k-lettuce:detekt :bluetape4k-protobuf:detekt --no-configuration-cache
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
git diff --check
git status --short
```

각 container-capable invocation 전에 다른 container/heavy work가 없음을 확인한다. evidence
delivery commit의 정상 descendant로 docs/review/lesson commit만 추가된 경우에는 strict
`validate-committed` ancestry 검증만 수행한다. `rebind-rebased-delivery`는 기존 delivery
commit 자체가 tree-equivalent rebase되어 SHA만 바뀐 경우에만 사용한다. production 또는
benchmark input tree가 바뀐 경우에만 fresh build/two-run/promotion lifecycle을 다시
수행한다. hash를 직접 수정하지 않는다.

- [ ] **Step 2: six-lens plan-to-code review를 실행한다.** performance, stability, security, operator, developer/API, caller lanes를 독립 실행하고 main session에서 중복을 통합한다. P0/P1은 수정 후 affected lane을 재검토하고, P2/P3은 수정 또는 rationale/follow-up을 남긴다. 최종 조건은 P0=0, P1=0이다.

review edit가 생기면 push 전에 다음 routing table을 적용하고 affected lane을 다시
검토한다.

| Tracked change after review | Required route |
|---|---|
| only `docs/review/**` and `docs/lessons/**` | commit, `validate-committed` ancestry와 report validation 재실행 |
| delivery commit 자체의 tree-equivalent rebase | 아래 transactional rebind/report/commit command 뒤 final exact-head 및 affected review gate 재진입 |
| production, tests, runner, validator, Gradle/build metadata, benchmark, KDoc, evidence/report/README/manual/CHANGELOG를 포함한 그 밖의 모든 변경 | commit한 뒤 retained는 Task 8-9-11, rejected는 B4-B5-Task 11의 terminal-specific verification/two-run/promotion lifecycle을 새 exact head에서 반복 |

어느 route든 변경 후 affected review lane과 final exact-head gate를 다시 통과하지 않으면
push하지 않는다.

tree-equivalent rebase route의 complete command는 다음과 같다.

```bash
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py resolve-rebased-delivery \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --head HEAD \
  --selection-state .omx/evidence/issue-757-lettuce/rebased-delivery-selection.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py rebind-rebased-delivery \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --selection-state .omx/evidence/issue-757-lettuce/rebased-delivery-selection.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py render-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --output docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
git diff --check
git diff --exit-code -- \
  ':!docs/benchmarks/raw/issue-757/delivery-manifest.json' \
  ':!docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md'
python3 - <<'PY'
import subprocess

expected = {
    " M docs/benchmarks/raw/issue-757/delivery-manifest.json",
    " M docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md",
}
observed = set(filter(None, subprocess.check_output(
    ["git", "status", "--porcelain=v1"], text=True,
).splitlines()))
assert observed == expected, (observed, expected)
PY
git add docs/benchmarks/raw/issue-757/delivery-manifest.json \
  docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
git commit \
  -m "Keep issue 757 evidence valid after a tree-equivalent rebase" \
  -m $'Constraint: Rebind only when the delivery tree is byte-equivalent\nRejected: Edit provenance hashes directly | It bypasses semantic validation\nConfidence: high\nScope-risk: narrow\nDirective: Re-enter exact-head verification and affected review before push\nTested: Working manifest semantics, deterministic report, and diff checks\nNot-tested: Remote CI remains pending'
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-committed \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json
python3 benchmark/protobuf-codec-benchmark/scripts/run-evidence.py validate-report \
  --manifest docs/benchmarks/raw/issue-757/delivery-manifest.json \
  --input docs/benchmarks/2026-07-18-protobuf-buffer-allocation.md
```

`render-report`는 `require_git_commit=False` semantic manifest validation을 먼저 수행하므로
rebind 이후의 working manifest가 invalid하면 report를 쓰지 않는다. commit 후에는 Task 12
Step 1 exact-head gate와 operator/reviewer affected lane을 다시 실행하고, 모두 통과하기 전에는
push하지 않는다.

- [ ] **Step 3: Korean review/lesson과 English GitHub handoff artifacts를 작성한다.** review에는 spec/plan mapping, terminal, ABI/wire/security/resource proof, commands/results, remaining risk를 기록한다. lesson에는 NIO view를 기각한 이유, absolute writer invariant, measurement/final-head split, evidence replacement 교훈을 기록한다.

validated committed manifest에서 terminal, delivery commit, tree/JAR/evidence identity를 읽어
`.omx/issue-757-pr-body.md`와 `.omx/issue-757-progress.md`를 영어로 작성한다. PR body는
`Summary`, `Terminal`, `Compatibility`, `Verification`, `Evidence`, `Excluded Scope`,
`Issue Link`, 마지막 heading `DoD Status`를 갖고 `Closes #757`을 포함하지 않는다. progress
comment는 같은 terminal/identity와 PR URL을 기록하며 retained/rejected post-merge disposition을
명시한다. 두 artifact의 terminal이 manifest와 다른 경우 GitHub mutation을 중단한다.

- [ ] **Step 4: final Lore commit 후 clean/exact head를 확인한다.** Intent: `Make issue 757 review and recovery evidence durable`.

```bash
git status --short
git rev-parse HEAD
git log -1 --format=fuller
```

- [ ] **Step 5: branch를 push하고 exact remote head를 확인한다.**

```bash
git push -u origin feat/issue-757-lettuce-protobuf-buffer
git rev-parse HEAD
git ls-remote --heads origin feat/issue-757-lettuce-protobuf-buffer
```

- [ ] **Step 6: terminal-specific English PR을 생성한다.** repo/base/head를 정확히 지정하고
body의 마지막 `##` heading을 `## DoD Status`로 둔다. accepted만 measured positive wording을
허용하고 inconclusive는 neutral wording을 사용한다. 모든 terminal은 post-merge evidence
기록보다 먼저 issue를 auto-close하지 않도록 `Closes #757`을 쓰지 않는다. rejected는
non-closing evidence-only PR임을 명시한다.

```bash
ISSUE757_TERMINAL=$(python3 -c \
  'import json; print(json.load(open("docs/benchmarks/raw/issue-757/delivery-manifest.json"))["delivery_terminal"])')
case "$ISSUE757_TERMINAL" in
  retained-accepted)
    ISSUE757_PR_TITLE="Optimize Lettuce Protobuf ByteBuf encoding"
    ;;
  retained-inconclusive)
    ISSUE757_PR_TITLE="Add Lettuce Protobuf direct ByteBuf encoding evidence"
    ;;
  rejected-after-regression)
    ISSUE757_PR_TITLE="Record Lettuce Protobuf ByteBuf allocation regression"
    ;;
  *) exit 1 ;;
esac
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head feat/issue-757-lettuce-protobuf-buffer \
  --title "$ISSUE757_PR_TITLE" \
  --body-file .omx/issue-757-pr-body.md
```

- [ ] **Step 7: English #757 progress comment를 게시한다.** validated committed manifest의
`delivery_terminal`을 그대로 사용하며 comment에는 terminal, PR URL,
exact head, compatibility/test/evidence result, compressed/custom-prefix/decode exclusions를
기록한다. retained는 approved merge/post-merge closure 전까지 issue가 open임을, rejected는
issue와 #898 item을 open으로 유지함을 명시한다.

```bash
gh issue comment 757 \
  --repo bluetape4k/bluetape4k-projects \
  --body-file .omx/issue-757-progress.md
```

- [ ] **Step 8: issue/PR live metadata와 exact-head CI/review를 확인한다.** milestone `1.12.0`, labels, assignee, unresolved threads, automated/human review artifact, local/remote/PR head equality를 확인한다.

- [ ] **Step 9: merge-ready 보고 후 정지한다.** merge는 fresh explicit user approval을 받은 뒤 별도 단계로 수행한다. post-merge issue #757/#898/#756 처리는 terminal별 명세를 따르고, release/tag/publish는 별도 승인 없이는 실행하지 않는다.

### Post-merge owner checklist (fresh merge approval 이후에만 실행)

- retained terminal: merged commit과 merged manifest/report를 다시 검증하고 #757에 PR/merge
  commit/final evidence를 영어로 기록한 뒤 issue를 close한다. #898의 #757 item을 완료하고
  #756에 Lettuce slice 완료와 compressed/custom-prefix/generic SPI 제외 범위를 링크한다.
- rejected terminal: #757과 #898 item을 open으로 유지하고 negative evidence, removed dispatch,
  남은 scope를 영어로 기록한다. #756에는 미완료 Lettuce slice와 제외 범위를 링크한다.
- 두 terminal 모두 milestone, labels, assignee를 live metadata에서 다시 확인한다.

## 2. 최종 spec-to-task traceability

| Spec/DoD | Plan task |
|---|---|
| minimal open ABI와 기존 descriptors | 1, 4, 10, 12 |
| bounded absolute writer/no NIO | 2, 3 |
| null/read-only/max/released/partial/short-success/resource | 2 |
| strict/trusted/fallback/allowlist/compressed compatibility | 2, 3, 11 |
| old/new wire와 Redis round trip | 3 |
| heap/direct copied/optimized benchmark | 5 |
| score/error/5%+8 B/op/uncertainty/terminal precedence | 6, 9 |
| immutable candidate evidence와 rejected removal | 8, 9, 10 |
| archive-aware repeated replacement | 7, 11 |
| promoted manifest/report/docs/locale/KDoc/changelog | 11 |
| module/static/full verification | 8, 12 |
| six-lens review, exact-head PR, merge gate | 12 |

## 3. 재실행 및 복구 규칙

- test failure는 `systematic-debugging`으로 root cause를 고정한 뒤 가장 작은 affected command부터 다시 실행한다.
- JMH run이 invalid/missing/identity mismatch면 해당 run ID를 재사용하거나 덮어쓰지 않고 새 run ID로 다시 두 run을 수집한다.
- candidate confirmed regression은 성능 문구만 제거하는 것으로 끝내지 않고 `lettuce_encode` rollback lifecycle과 subtype/open ABI 제거를 수행한다.
- evidence replacement 실패 시 unique backup을 보존하고 기존 promoted destination을 복원한다. committed archive나 raw evidence를 수동 삭제하지 않는다.
- evidence manifest/hash는 직접 수정하지 않는다. runner/validator/rebind의 검증된 command로만 생성한다.
- Testcontainers/JMH는 timeout 또는 flaky retry가 성공해도 원인을 review artifact에 남긴다.
- scope 밖 문제는 현재 구현에 섞지 않고 follow-up issue 후보로 기록한다.

## 4. 구현 계획 독립 검토 수렴

2026-07-20에 performance, stability, security, operator, developer/API, caller 여섯 관점을
독립 실행하고 지적을 계획에 통합한 뒤 affected lane을 재검토했다.

| 관점 | 최종 P0 | 최종 P1 | 최종 P2 | 최종 P3 | 판정 |
|---|---:|---:|---:|---:|---|
| Performance | 0 | 0 | 0 | 0 | PASS |
| Stability | 0 | 0 | 0 | 0 | PASS |
| Security | 0 | 0 | 0 | 0 | PASS |
| Operator | 0 | 0 | 0 | 0 | PASS |
| Developer/API | 0 | 0 | 0 | 0 | PASS |
| Caller | 0 | 0 | 0 | 0 | PASS |

통합한 핵심 보강은 allocation `scoreError` 보존과 terminal precedence, sequential heavy-work
gate, authorized receipt/rollback roots, private-only writer seam과 retained/rejected ABI validator
TDD, 네 개 locale 문서 pair의 executable parity, committed manifest terminal authority,
tree-equivalent rebase의 exact ancestor selection과 transactional recovery다. 최종 수렴 조건은
모든 관점 P0=P1=P2=P3=0이며 현재 이를 만족한다.
