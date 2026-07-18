# Issue #758 Kafka ByteBuffer Codec Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 표준 Kafka `ByteArray` 경계를 유지하면서 `BinaryKafkaCodec`에 caller-owned `ByteBuffer` 입출력 계약, 동일한 poison-pill logging, 반복 가능한 allocation evidence를 추가한다.

**Architecture:** 새 `BufferAwareKafkaCodec<T>`는 기존 `KafkaCodec<T>` 구현체에 영향을 주지 않는 opt-in interface다. `BinaryKafkaCodec`만 `BinarySerializer.serializeTo`와 `deserializeFrom`으로 이를 구현하고, ByteArray와 ByteBuffer 역직렬화는 `AbstractKafkaCodec`의 inline failure boundary를 공유한다. 성능 증거는 기존 `serializer-benchmark` 모듈의 Kryo-backed codec-only 네 개 cell로 수집한다.

**Tech Stack:** Kotlin 2.3, Java 21, Apache Kafka 4.2, `ByteBuffer`, bluetape4k `BinarySerializer`, JUnit 5, bluetape assertions, Logback, kotlinx-benchmark/JMH 1.37, Gradle 9.6.

---

## 파일 구조

- `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt`: opt-in public contract와 공통 poison-pill failure boundary.
- `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt`: buffer-aware binary delegation과 header 동작.
- `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/BufferAwareKafkaCodecTest.kt`: interface default overload dispatch.
- `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecPoisonPillTest.kt`: logging 및 throwable identity 회귀.
- `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecBufferTest.kt`: header, buffer state, direct delegation, failure policy.
- `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodecTest.kt`: raw array identity 회귀.
- `benchmark/serializer-benchmark/build.gradle.kts`: Kafka benchmark dependency.
- `benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecBenchmarkSupport.kt`: fixture와 correctness validation.
- `benchmark/serializer-benchmark/src/test/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecBenchmarkSupportTest.kt`: pre-timed fixture 검증.
- `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecAllocationBenchmark.kt`: 네 allocation cell.
- `infra/kafka4/README.md`, `infra/kafka4/README.ko.md`: public API, ownership, logging, 한계.
- `benchmark/serializer-benchmark/README.md`, `benchmark/serializer-benchmark/README.ko.md`: 44-cell matrix와 실행법.
- `docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md`, `docs/benchmarks/raw/issue-758/`, `docs/benchmarks/README.md`: 반복 측정 수치와 index.

---

### Task 1: Public `BufferAwareKafkaCodec` Contract

**Files:**
- Create: `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/BufferAwareKafkaCodecTest.kt`
- Modify: `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt:10-52`

- [ ] **Step 1: Write the failing interface dispatch test**

```kotlin
package io.bluetape4k.kafka.codec

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.apache.kafka.common.header.Headers
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class BufferAwareKafkaCodecTest {
    private class RecordingCodec: BufferAwareKafkaCodec<String> {
        var serializeHeaders: Headers? = null
        var deserializeHeaders: Headers? = null

        override fun serialize(topic: String?, headers: Headers?, data: String?): ByteArray? =
            data?.encodeToByteArray()

        override fun deserialize(topic: String?, headers: Headers?, data: ByteArray?): String? =
            data?.decodeToString()

        override fun serializeTo(
            topic: String?,
            headers: Headers?,
            data: String,
            target: ByteBuffer,
        ): Int {
            serializeHeaders = headers
            val bytes = data.encodeToByteArray()
            target.put(bytes)
            return bytes.size
        }

        override fun deserializeFrom(topic: String?, headers: Headers?, source: ByteBuffer): String {
            deserializeHeaders = headers
            return source.duplicate().let { view ->
                ByteArray(view.remaining()).also(view::get).decodeToString()
            }
        }
    }

    @Test
    fun `headerless output delegates with null headers`() {
        val codec = RecordingCodec()
        val target = ByteBuffer.allocate(16)

        codec.serializeTo("events", "hello", target) shouldBeEqualTo 5

        codec.serializeHeaders.shouldBeNull()
        target.position() shouldBeEqualTo 5
    }

    @Test
    fun `headerless input delegates with null headers`() {
        val codec = RecordingCodec()
        val source = ByteBuffer.wrap("hello".encodeToByteArray())

        codec.deserializeFrom("events", source) shouldBeEqualTo "hello"

        codec.deserializeHeaders.shouldBeNull()
        source.position() shouldBeEqualTo 0
    }
}
```

- [ ] **Step 2: Run the test and prove the contract is absent**

```bash
repo-test-summary -- ./gradlew :bluetape4k-kafka4:test \
  --tests 'io.bluetape4k.kafka.codec.BufferAwareKafkaCodecTest' \
  --no-configuration-cache --no-build-cache
```

Expected: `compileTestKotlin` FAIL with unresolved `BufferAwareKafkaCodec`.

- [ ] **Step 3: Add the interface and English KDoc**

Import `java.nio.ByteBuffer` and insert after `KafkaCodec<T>`:

```kotlin
/**
 * Opt-in Kafka codec contract for caller-owned [ByteBuffer] input and output.
 *
 * Standard Kafka [Serializer] and [Deserializer] calls remain [ByteArray]-based. These methods avoid an additional
 * Kafka-layer array conversion when the backing codec already supports buffers; the concrete serializer still
 * determines whether its implementation is optimized or an allocating compatibility fallback.
 *
 * Output advances the target position by the returned byte count on success. Input reads the source's current
 * remaining range while preserving caller state. Buffers remain caller-owned and must not be mutated concurrently.
 */
interface BufferAwareKafkaCodec<T>: KafkaCodec<T> {
    fun serializeTo(topic: String?, data: T & Any, target: ByteBuffer): Int =
        serializeTo(topic, null, data, target)

    fun serializeTo(
        topic: String?,
        headers: Headers?,
        data: T & Any,
        target: ByteBuffer,
    ): Int

    fun deserializeFrom(topic: String?, source: ByteBuffer): T? =
        deserializeFrom(topic, null, source)

    fun deserializeFrom(topic: String?, headers: Headers?, source: ByteBuffer): T?
}
```

- [ ] **Step 4: Re-run the focused test**

Run Step 2 again. Expected: PASS.

- [ ] **Step 5: Commit the contract slice**

```bash
git add infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt \
  infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/BufferAwareKafkaCodecTest.kt
git commit -m 'Expose an honest caller-owned buffer contract for Kafka codecs' \
  -m 'Constraint: Standard Kafka serialization remains ByteArray-based.
Rejected: Generic KafkaCodec buffer extensions | They would hide allocating fallbacks behind every codec.
Confidence: high
Scope-risk: narrow
Directive: Keep null tombstones on the standard Kafka API and buffer output non-null.
Tested: BufferAwareKafkaCodecTest
Not-tested: Binary codec integration and allocation evidence are covered by later tasks'
```

---

### Task 2: Shared Poison-Pill Logging Boundary

**Files:**
- Modify: `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecPoisonPillTest.kt:1-68`
- Modify: `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt:160-190`

- [ ] **Step 1: Add behavior-lock tests for bounded WARN context and identity**

Add Logback `Logger`, `ILoggingEvent`, `ListAppender`, `shouldBeEqualTo`, `shouldBeSameInstanceAs`, and `toUtf8Bytes` imports, then add/replace these tests:

```kotlin
@Test
fun `general Exception logs bounded context without payload or header values`() {
    val codec = ThrowingCodec(FakeException())
    val headers = RecordHeaders().add("trace-id", "secret-header".toUtf8Bytes())
    val logger = AbstractKafkaCodec.log as Logger
    val appender = ListAppender<ILoggingEvent>().apply { start() }
    logger.addAppender(appender)
    try {
        codec.deserialize("test-topic", headers, "secret-payload".toUtf8Bytes()).shouldBeNull()
        val message = appender.list.single().formattedMessage
        message.contains("topic=test-topic") shouldBeEqualTo true
        message.contains("trace-id") shouldBeEqualTo true
        message.contains("dataSize=14") shouldBeEqualTo true
        message.contains("secret-header") shouldBeEqualTo false
        message.contains("secret-payload") shouldBeEqualTo false
    } finally {
        logger.detachAppender(appender)
        appender.stop()
    }
}

@Test
fun `CancellationException is rethrown with identity preserved`() {
    val failure = CancellationException("coroutine cancelled")
    val thrown = assertFailsWith<CancellationException> {
        ThrowingCodec(failure).deserialize("test-topic", RecordHeaders(), byteArrayOf(1, 2, 3))
    }
    thrown shouldBeSameInstanceAs failure
}

@Test
fun `Error is propagated with identity preserved`() {
    val failure = OutOfMemoryError("simulated")
    val thrown = assertFailsWith<OutOfMemoryError> {
        ThrowingCodec(failure).deserialize("test-topic", RecordHeaders(), byteArrayOf(1, 2, 3))
    }
    thrown shouldBeSameInstanceAs failure
}
```

- [ ] **Step 2: Run the behavior lock before refactoring**

```bash
repo-test-summary -- ./gradlew :bluetape4k-kafka4:test \
  --tests 'io.bluetape4k.kafka.codec.AbstractKafkaCodecPoisonPillTest' \
  --no-configuration-cache --no-build-cache
```

Expected: PASS, proving current behavior before structural change.

- [ ] **Step 3: Extract the inline failure boundary**

Replace `deserialize(topic, headers, data)` and add:

```kotlin
override fun deserialize(topic: String?, headers: Headers?, data: ByteArray?): T? =
    data?.let { bytes ->
        deserializeSafely(topic, headers, bytes.size) {
            doDeserialize(topic, headers, bytes)
        }
    }

/**
 * Applies the poison-pill policy without allocating a capturing lambda on the deserialization hot path.
 * Ordinary exceptions become bounded WARN logs and `null`; cancellation and fatal JVM errors propagate.
 */
protected inline fun deserializeSafely(
    topic: String?,
    headers: Headers?,
    dataSize: Int,
    operation: () -> T?,
): T? =
    try {
        operation()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.warn(e) {
            "Fail to deserialize data. topic=$topic, headerKeys=${headers?.map { it.key() }}, " +
                "dataSize=$dataSize. Returning null (poison pill skipped)."
        }
        null
    }
```

- [ ] **Step 4: Run poison and codec regression tests**

```bash
repo-test-summary -- ./gradlew :bluetape4k-kafka4:test \
  --tests 'io.bluetape4k.kafka.codec.AbstractKafkaCodecPoisonPillTest' \
  --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' \
  --no-configuration-cache --no-build-cache
```

Expected: PASS with the same WARN fields and throwable identity.

- [ ] **Step 5: Commit the behavior-preserving boundary**

```bash
git add infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/KafkaCodec.kt \
  infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/AbstractKafkaCodecPoisonPillTest.kt
git commit -m 'Share Kafka poison handling without adding hot-path allocation' \
  -m 'Constraint: ByteArray and ByteBuffer deserialization must keep one exception and logging policy.
Confidence: high
Scope-risk: narrow
Directive: Log bounded metadata only and never swallow CancellationException or Error.
Tested: AbstractKafkaCodecPoisonPillTest and KafkaCodecTest
Not-tested: ByteBuffer dispatch is introduced in the next slice'
```

---

### Task 3: `BinaryKafkaCodec` Buffer Implementation

**Files:**
- Create: `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecBufferTest.kt`
- Modify: `infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt:1-30`

- [ ] **Step 1: Write failing delegation and contract tests**

```kotlin
package io.bluetape4k.kafka.codec

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.CancellationException
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException

class BinaryKafkaCodecBufferTest {
    private class RecordingSerializer: BinarySerializer {
        var serializeTarget: ByteBuffer? = null
        var deserializeSource: ByteBuffer? = null

        override fun serialize(graph: Any?): ByteArray = "encoded".encodeToByteArray()

        override fun serializeTo(graph: Any?, target: ByteBuffer): Int {
            serializeTarget = target
            val bytes = "encoded".encodeToByteArray()
            target.put(bytes)
            return bytes.size
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any> deserialize(bytes: ByteArray?): T? = "decoded" as T

        @Suppress("UNCHECKED_CAST")
        override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
            deserializeSource = source
            return "decoded" as T
        }
    }

    private class ThrowingSerializer(private val failure: Throwable): BinarySerializer {
        override fun serialize(graph: Any?): ByteArray = throw failure
        override fun serializeTo(graph: Any?, target: ByteBuffer): Int = throw failure
        override fun <T: Any> deserialize(bytes: ByteArray?): T? = throw failure
        override fun <T: Any> deserializeFrom(source: ByteBuffer): T? = throw failure
    }

    private class TestBinaryCodec(
        serializer: BinarySerializer,
        override val writeValueTypeHeader: Boolean = true,
    ): BinaryKafkaCodec(serializer)

    @Test
    fun `buffer methods delegate exact caller buffers`() {
        val serializer = RecordingSerializer()
        val codec: BufferAwareKafkaCodec<Any?> = TestBinaryCodec(serializer)
        val target = ByteBuffer.allocate(32).apply { position(3) }
        val source = ByteBuffer.wrap("encoded".encodeToByteArray()).asReadOnlyBuffer()

        codec.serializeTo("events", "value", target) shouldBeEqualTo 7
        codec.deserializeFrom("events", source) shouldBeEqualTo "decoded"

        serializer.serializeTarget shouldBeSameInstanceAs target
        serializer.deserializeSource shouldBeSameInstanceAs source
    }

    @Test
    fun `buffer serialization preserves headers and header opt out`() {
        val headers = RecordHeaders().add("trace-id", "trace-value".encodeToByteArray())
        TestBinaryCodec(RecordingSerializer()).serializeTo(
            "events",
            headers,
            "value",
            ByteBuffer.allocate(32),
        )

        headers.lastHeader("trace-id").value().toUtf8String() shouldBeEqualTo "trace-value"
        headers.lastHeader(AbstractKafkaCodec.VALUE_TYPE_KEY).value().toUtf8String() shouldBeEqualTo
            String::class.java.name

        val noHeader = RecordHeaders()
        TestBinaryCodec(RecordingSerializer(), writeValueTypeHeader = false).serializeTo(
            "events",
            noHeader,
            "value",
            ByteBuffer.allocate(32),
        )
        noHeader.lastHeader(AbstractKafkaCodec.VALUE_TYPE_KEY).shouldBeNull()
    }

    @Test
    fun `serialization failure keeps header and propagates identity`() {
        val failure = IllegalStateException("encode failed")
        val headers = RecordHeaders()

        assertFailsWith<IllegalStateException> {
            TestBinaryCodec(ThrowingSerializer(failure)).serializeTo(
                "events",
                headers,
                "value",
                ByteBuffer.allocate(32),
            )
        } shouldBeSameInstanceAs failure
        headers.lastHeader(AbstractKafkaCodec.VALUE_TYPE_KEY).value().toUtf8String() shouldBeEqualTo
            String::class.java.name
    }

    @Test
    fun `Kryo round trip preserves bounded caller state`() {
        val codec: BufferAwareKafkaCodec<Any?> = KryoKafkaCodec()
        val target = ByteBuffer.allocateDirect(4096).order(ByteOrder.LITTLE_ENDIAN).apply {
            position(5)
            limit(capacity() - 7)
        }
        val start = target.position()
        val targetLimit = target.limit()
        val written = codec.serializeTo("events", listOf("a", "b", "c"), target)
        val source = target.duplicate().apply {
            position(start)
            limit(start + written)
        }.slice().asReadOnlyBuffer().apply { mark() }
        val sourcePosition = source.position()
        val sourceLimit = source.limit()

        codec.deserializeFrom("events", source) shouldBeEqualTo listOf("a", "b", "c")

        target.position() shouldBeEqualTo start + written
        target.limit() shouldBeEqualTo targetLimit
        target.order() shouldBeEqualTo ByteOrder.LITTLE_ENDIAN
        source.position() shouldBeEqualTo sourcePosition
        source.limit() shouldBeEqualTo sourceLimit
        source.reset().position() shouldBeEqualTo sourcePosition
    }

    @Test
    fun `Kryo input supports heap direct sliced and read only buffers`() {
        val codec: BufferAwareKafkaCodec<Any?> = KryoKafkaCodec()
        val payload = listOf("heap", "direct", "slice", "read-only")
        val wire = requireNotNull(codec.serialize("events", null, payload))
        val heap = ByteBuffer.wrap(wire)
        val direct = ByteBuffer.allocateDirect(wire.size).apply { put(wire).flip() }
        val sliced = ByteBuffer.allocate(wire.size + 4).apply {
            position(2)
            put(wire)
            flip()
            position(2)
            limit(2 + wire.size)
        }.slice()
        val readOnly = ByteBuffer.wrap(wire).asReadOnlyBuffer()

        listOf(heap, direct, sliced, readOnly).forEach { source ->
            val position = source.position()
            val limit = source.limit()
            source.mark()

            codec.deserializeFrom("events", source) shouldBeEqualTo payload

            source.position() shouldBeEqualTo position
            source.limit() shouldBeEqualTo limit
            source.reset().position() shouldBeEqualTo position
        }
        codec.deserializeFrom("events", ByteBuffer.allocate(0)).shouldBeNull()
    }

    @Test
    fun `too small and read only targets preserve position`() {
        val codec: BufferAwareKafkaCodec<Any?> = KryoKafkaCodec()
        val tooSmall = ByteBuffer.allocate(1).apply { position(1) }
        val readOnly = ByteBuffer.allocate(32).asReadOnlyBuffer().apply { position(4) }

        assertFailsWith<BufferOverflowException> { codec.serializeTo("events", "value", tooSmall) }
        assertFailsWith<ReadOnlyBufferException> { codec.serializeTo("events", "value", readOnly) }

        tooSmall.position() shouldBeEqualTo 1
        readOnly.position() shouldBeEqualTo 4
    }

    @Test
    fun `ordinary buffer failure logs bounded context and returns null`() {
        val codec = TestBinaryCodec(ThrowingSerializer(IllegalArgumentException("secret-payload")))
        val logger = AbstractKafkaCodec.log as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            codec.deserializeFrom(
                "events",
                RecordHeaders().add("trace-id", byteArrayOf(1)),
                ByteBuffer.allocate(17),
            ).shouldBeNull()
            val message = appender.list.single().formattedMessage
            message.contains("topic=events") shouldBeEqualTo true
            message.contains("trace-id") shouldBeEqualTo true
            message.contains("dataSize=17") shouldBeEqualTo true
            message.contains("secret-payload") shouldBeEqualTo false
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @Test
    fun `buffer cancellation and Error preserve identity`() {
        val cancellation = CancellationException("cancelled")
        val fatal = OutOfMemoryError("fatal")
        val source = ByteBuffer.allocate(1)

        assertFailsWith<CancellationException> {
            TestBinaryCodec(ThrowingSerializer(cancellation)).deserializeFrom("events", source)
        } shouldBeSameInstanceAs cancellation
        assertFailsWith<OutOfMemoryError> {
            TestBinaryCodec(ThrowingSerializer(fatal)).deserializeFrom("events", source)
        } shouldBeSameInstanceAs fatal
    }
}
```

- [ ] **Step 2: Run the focused test and prove binary dispatch is absent**

```bash
repo-test-summary -- ./gradlew :bluetape4k-kafka4:test \
  --tests 'io.bluetape4k.kafka.codec.BinaryKafkaCodecBufferTest' \
  --no-configuration-cache --no-build-cache
```

Expected: FAIL because `BinaryKafkaCodec` is not a `BufferAwareKafkaCodec`.

- [ ] **Step 3: Implement direct delegation, header semantics, KDoc, and rationale comment**

Import `java.nio.ByteBuffer` and replace the opening `BinaryKafkaCodec` declaration/body with:

```kotlin
/**
 * Kafka codec backed by a [BinarySerializer].
 *
 * Standard Kafka calls retain their required [ByteArray] boundary. [BufferAwareKafkaCodec] methods delegate to the
 * serializer's caller-owned buffer API without a Kafka-layer array conversion. Allocation behavior still depends on
 * the concrete serializer because interface-default buffer methods may be allocating compatibility fallbacks.
 *
 * Buffer deserialization uses the same WARN-and-null poison-pill policy as standard Kafka deserialization while
 * preserving coroutine cancellation and fatal JVM errors.
 */
abstract class BinaryKafkaCodec(
    private val serializer: BinarySerializer,
): AbstractKafkaCodec<Any?>(), BufferAwareKafkaCodec<Any?> {

    override fun doSerialize(topic: String?, headers: Headers?, graph: Any?): ByteArray =
        serializer.serialize(graph)

    override fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): Any? =
        serializer.deserialize(bytes)

    override fun serializeTo(
        topic: String?,
        headers: Headers?,
        data: Any,
        target: ByteBuffer,
    ): Int {
        // Match the standard path: a committed header is not rolled back when serializer work fails.
        if (writeValueTypeHeader) setValueType(headers, data.javaClass)
        return serializer.serializeTo(data, target)
    }

    override fun deserializeFrom(topic: String?, headers: Headers?, source: ByteBuffer): Any? =
        deserializeSafely(topic, headers, source.remaining()) {
            serializer.deserializeFrom<Any>(source)
        }
}
```

- [ ] **Step 4: Run focused and codec regression tests**

```bash
repo-test-summary -- ./gradlew :bluetape4k-kafka4:test \
  --tests 'io.bluetape4k.kafka.codec.BufferAwareKafkaCodecTest' \
  --tests 'io.bluetape4k.kafka.codec.BinaryKafkaCodecBufferTest' \
  --tests 'io.bluetape4k.kafka.codec.AbstractKafkaCodecPoisonPillTest' \
  --tests 'io.bluetape4k.kafka.codec.KafkaCodecTest' \
  --no-configuration-cache --no-build-cache
```

Expected: PASS without broker/Testcontainers execution.

- [ ] **Step 5: Commit the binary implementation**

```bash
git add infra/kafka4/src/main/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecs.kt \
  infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/BinaryKafkaCodecBufferTest.kt
git commit -m 'Route Kafka binary codecs through caller-owned buffers' \
  -m 'Constraint: Header and poison-pill behavior must match the existing ByteArray path.
Rejected: Kafka Bytes overloads | They add no measured lower-allocation boundary beyond the existing array API.
Confidence: high
Scope-risk: moderate
Directive: Delegate directly to BinarySerializer and keep success paths free of logging.
Tested: BufferAwareKafkaCodecTest, BinaryKafkaCodecBufferTest, AbstractKafkaCodecPoisonPillTest, KafkaCodecTest
Not-tested: Allocation evidence is collected after benchmark wiring'
```

---

### Task 4: Raw `ByteArray` Passthrough Identity

**Files:**
- Modify: `infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodecTest.kt:1-103`

- [ ] **Step 1: Tighten existing assertions from equality to identity plus equality**

Import `shouldBeSameInstanceAs`. Add these assertions to the existing serialize, deserialize, and binary round-trip tests:

```kotlin
bytes shouldBeSameInstanceAs original
deserialized shouldBeSameInstanceAs original
bytes shouldBeSameInstanceAs binaryData
deserialized shouldBeSameInstanceAs binaryData
```

- [ ] **Step 2: Run the passthrough regression**

```bash
repo-test-summary -- ./gradlew :bluetape4k-kafka4:test \
  --tests 'io.bluetape4k.kafka.codec.ByteArrayKafkaCodecTest' \
  --no-configuration-cache --no-build-cache
```

Expected: PASS without production changes.

- [ ] **Step 3: Commit the identity proof**

```bash
git add infra/kafka4/src/test/kotlin/io/bluetape4k/kafka/codec/ByteArrayKafkaCodecTest.kt
git commit -m 'Lock Kafka raw payload passthrough to the original array' \
  -m 'Constraint: Buffer support must not turn ByteArrayKafkaCodec into a copying adapter.
Confidence: high
Scope-risk: narrow
Directive: Preserve array identity on both standard codec directions.
Tested: ByteArrayKafkaCodecTest
Not-tested: No broker integration is required for this codec-only contract'
```

---

### Task 5: Codec-Only Benchmark Fixture And Cells

**Files:**
- Modify: `benchmark/serializer-benchmark/build.gradle.kts:50-66`
- Create: `benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecBenchmarkSupport.kt`
- Create: `benchmark/serializer-benchmark/src/test/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecBenchmarkSupportTest.kt`
- Create: `benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecAllocationBenchmark.kt`

- [ ] **Step 1: Add a failing fixture test**

```kotlin
package io.bluetape4k.benchmark.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class KafkaCodecBenchmarkSupportTest {
    @Test
    fun `fixture validates equivalent Kafka codec paths`() {
        val fixture = KafkaCodecBenchmarkFixture()

        fixture.validate()

        fixture.wireSize > 0 shouldBeEqualTo true
    }
}
```

- [ ] **Step 2: Add the Kafka dependency and prove the fixture is absent**

Add after the Avro project dependency:

```kotlin
implementation(project(":bluetape4k-kafka4"))
```

Run:

```bash
repo-test-summary -- ./gradlew :serializer-benchmark:test \
  --tests 'io.bluetape4k.benchmark.serializer.KafkaCodecBenchmarkSupportTest' \
  --no-configuration-cache --no-build-cache
```

Expected: `compileTestKotlin` FAIL with unresolved `KafkaCodecBenchmarkFixture`.

- [ ] **Step 3: Implement the fixture and pre-timed validation**

```kotlin
package io.bluetape4k.benchmark.serializer

import io.bluetape4k.kafka.codec.BufferAwareKafkaCodec
import io.bluetape4k.kafka.codec.KryoKafkaCodec
import java.nio.ByteBuffer

class KafkaCodecBenchmarkFixture(
    private val codec: BufferAwareKafkaCodec<Any?> = KryoKafkaCodec(),
    val payload: SerializerBenchmarkPayload = SerializerBenchmarkPayload.sample(),
) {
    companion object {
        const val TOPIC = "allocation-benchmark"
    }

    private val wire = requireNotNull(codec.serialize(TOPIC, null, payload))
    private val source = ByteBuffer.allocateDirect(wire.size).apply {
        put(wire)
        flip()
    }.asReadOnlyBuffer()

    val wireSize: Int = wire.size

    fun newTarget(): ByteBuffer = ByteBuffer.allocate(maxOf(wireSize * 2, 4096))

    fun serializeByteArray(): ByteArray = requireNotNull(codec.serialize(TOPIC, null, payload))

    fun serializeOptimized(target: ByteBuffer): Int = codec.serializeTo(TOPIC, payload, target)

    fun deserializeByteArray(): SerializerBenchmarkPayload? =
        codec.deserialize(TOPIC, null, wire) as? SerializerBenchmarkPayload

    fun deserializeOptimized(): SerializerBenchmarkPayload? =
        codec.deserializeFrom(TOPIC, source) as? SerializerBenchmarkPayload

    fun validate() {
        check(wire.isNotEmpty()) { "Kafka Kryo codec produced an empty payload." }
        check(payload.semanticallyEquals(deserializeByteArray())) { "Kafka ByteArray path changed the payload." }

        val sourcePosition = source.position()
        val sourceLimit = source.limit()
        check(payload.semanticallyEquals(deserializeOptimized())) { "Kafka ByteBuffer path changed the payload." }
        check(source.position() == sourcePosition) { "Kafka ByteBuffer input changed source position." }
        check(source.limit() == sourceLimit) { "Kafka ByteBuffer input changed source limit." }

        val target = newTarget().apply { position(3) }
        val start = target.position()
        val written = serializeOptimized(target)
        check(written > 0) { "Kafka ByteBuffer output wrote no bytes." }
        check(target.position() == start + written) { "Kafka ByteBuffer output reported an inconsistent count." }
        val bytes = target.duplicate().apply {
            position(start)
            limit(start + written)
        }.let { view -> ByteArray(view.remaining()).also(view::get) }
        check(payload.semanticallyEquals(codec.deserialize(TOPIC, null, bytes) as? SerializerBenchmarkPayload)) {
            "Kafka ByteBuffer output changed the payload."
        }
    }
}
```

- [ ] **Step 4: Run the fixture test**

Run Step 2 again. Expected: PASS.

- [ ] **Step 5: Add the four allocation cells**

```kotlin
package io.bluetape4k.benchmark.serializer

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.Level
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import org.openjdk.jmh.infra.Blackhole
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
class KafkaCodecAllocationBenchmark {
    private lateinit var fixture: KafkaCodecBenchmarkFixture
    private lateinit var target: ByteBuffer

    @Setup(Level.Trial)
    fun setup() {
        fixture = KafkaCodecBenchmarkFixture().also { it.validate() }
        target = fixture.newTarget()
    }

    @Setup(Level.Invocation)
    fun resetTarget() {
        target.clear()
    }

    @Benchmark
    fun kafkaSerializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fixture.serializeByteArray())
    }

    @Benchmark
    fun kafkaSerializeOptimized(blackhole: Blackhole) {
        blackhole.consume(fixture.serializeOptimized(target))
    }

    @Benchmark
    fun kafkaDeserializeByteArray(blackhole: Blackhole) {
        blackhole.consume(fixture.deserializeByteArray())
    }

    @Benchmark
    fun kafkaDeserializeOptimized(blackhole: Blackhole) {
        blackhole.consume(fixture.deserializeOptimized())
    }
}
```

- [ ] **Step 6: Compile generated JMH sources and list exactly four methods**

```bash
./gradlew :serializer-benchmark:benchmarkBenchmarkCompile --no-configuration-cache --no-build-cache
./gradlew :serializer-benchmark:benchmarkBenchmarkJar --no-configuration-cache --no-build-cache
java -jar benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/*-JMH.jar \
  -l '.*KafkaCodecAllocationBenchmark.*'
```

Expected: PASS and four Kafka benchmark method names.

- [ ] **Step 7: Commit benchmark wiring**

```bash
git add benchmark/serializer-benchmark/build.gradle.kts \
  benchmark/serializer-benchmark/src/main/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecBenchmarkSupport.kt \
  benchmark/serializer-benchmark/src/test/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecBenchmarkSupportTest.kt \
  benchmark/serializer-benchmark/src/benchmark/kotlin/io/bluetape4k/benchmark/serializer/KafkaCodecAllocationBenchmark.kt
git commit -m 'Measure Kafka codec allocation without broker noise' \
  -m 'Constraint: Timed cells exclude headers, brokers, networking, and buffer construction.
Rejected: End-to-end Kafka throughput benchmark | It cannot attribute allocation to codec dispatch.
Confidence: high
Scope-risk: moderate
Directive: Keep method names compatible with summarize-jmh.py baseline pairing.
Tested: KafkaCodecBenchmarkSupportTest, benchmarkBenchmarkCompile, benchmarkBenchmarkJar, JMH method listing
Not-tested: Two fresh GC-profiler evidence runs are the next task'
```

---

### Task 6: English/Korean Public Documentation

**Files:**
- Modify: `infra/kafka4/README.md:128-170`
- Modify: `infra/kafka4/README.ko.md:128-170`
- Modify: `benchmark/serializer-benchmark/README.md:1-35`
- Modify: `benchmark/serializer-benchmark/README.ko.md:1-35`

- [ ] **Step 1: Add the English API section before the Fory security section**

````markdown
### Caller-owned ByteBuffer API

Kafka's standard `Serializer` and `Deserializer` interfaces remain `ByteArray`-based. Binary codecs additionally
implement `BufferAwareKafkaCodec`, an opt-in API for callers that already own reusable buffers. This removes an
extra Kafka-layer array conversion but is not a zero-copy Kafka boundary; the backing `BinarySerializer` may still
use an allocating compatibility fallback.

```kotlin
val codec: BufferAwareKafkaCodec<Any?> = KafkaCodecs.Kryo
val target = ByteBuffer.allocate(4096)
val written = codec.serializeTo("events", event, target)
target.flip()
val decoded = codec.deserializeFrom("events", target.asReadOnlyBuffer())
```

```java
BufferAwareKafkaCodec<Object> codec = KafkaCodecs.INSTANCE.getKryo();
ByteBuffer target = ByteBuffer.allocate(4096);
int written = codec.serializeTo("events", event, target);
target.flip();
Object decoded = codec.deserializeFrom("events", target.asReadOnlyBuffer());
```

Successful output advances `position` by `written` without widening `limit`. Input reads only the initial remaining
range and preserves source state. Ordinary decode exceptions produce the existing bounded WARN log and return
`null`; cancellation and fatal errors propagate. Keep buffers caller-owned and thread-confined during a call.

Allocation claims are limited to measured Kryo codec directions in the [issue #758 report](../../docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md). Throughput and broker costs are not measured.
````

- [ ] **Step 2: Add the meaning-equivalent Korean API section**

````markdown
### 호출자 소유 ByteBuffer API

Kafka 표준 `Serializer`와 `Deserializer` 인터페이스는 계속 `ByteArray` 기반입니다. 바이너리 codec은
재사용 버퍼를 이미 소유한 호출자를 위한 opt-in `BufferAwareKafkaCodec`도 구현합니다. 이 API는 Kafka
layer의 추가 배열 변환을 제거하지만 zero-copy Kafka 경계를 뜻하지 않으며, 하위 `BinarySerializer`가
할당이 있는 compatibility fallback을 사용할 수도 있습니다.

```kotlin
val codec: BufferAwareKafkaCodec<Any?> = KafkaCodecs.Kryo
val target = ByteBuffer.allocate(4096)
val written = codec.serializeTo("events", event, target)
target.flip()
val decoded = codec.deserializeFrom("events", target.asReadOnlyBuffer())
```

```java
BufferAwareKafkaCodec<Object> codec = KafkaCodecs.INSTANCE.getKryo();
ByteBuffer target = ByteBuffer.allocate(4096);
int written = codec.serializeTo("events", event, target);
target.flip();
Object decoded = codec.deserializeFrom("events", target.asReadOnlyBuffer());
```

출력 성공 시 `limit`을 넓히지 않고 `written`만큼 `position`을 전진시킵니다. 입력은 최초 remaining
범위만 읽고 source 상태를 보존합니다. 일반 decode 예외는 제한된 metadata만 WARN으로 기록하고
`null`을 반환하며 cancellation과 fatal error는 전파합니다. 호출 중 버퍼는 호출자가 소유하고 한
thread에서만 사용해야 합니다.

allocation 주장은 [issue #758 보고서](../../docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md)에서 측정한 Kryo codec 방향으로 제한합니다. throughput과 broker 비용은 측정하지 않습니다.
````

- [ ] **Step 3: Update both benchmark READMEs**

Use this English matrix statement and row:

```markdown
The 44 cells cover JDK (6), Kryo (6), Fory (4), Jackson 2 (6), Jackson 3 (6), Fastjson2 (6), Avro reflect (6), and the Kryo-backed Kafka codec (4), with serialization and deserialization measured separately.

| Kafka Kryo codec | standard ByteArray vs caller-owned optimized target | standard ByteArray vs caller-owned optimized source |
```

Use the meaning-equivalent Korean text:

```markdown
44개 셀은 JDK(6), Kryo(6), Fory(4), Jackson 2(6), Jackson 3(6), Fastjson2(6), Avro reflect(6), Kryo 기반 Kafka codec(4)을 포함하며 직렬화와 역직렬화를 분리합니다.

| Kafka Kryo codec | 표준 ByteArray 대 호출자 소유 optimized target | 표준 ByteArray 대 호출자 소유 optimized source |
```

Add this command to both command sections:

```bash
java -jar build/benchmarks/benchmark/jars/*-JMH.jar '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json -rff kafka-codec-jmh.json
```

Replace the sentence excluding #758 with the #758 report link; keep #755-#757 out of scope.

- [ ] **Step 4: Verify locale parity and stale-scope removal**

```bash
rg -n 'BufferAwareKafkaCodec|ByteArray|zero-copy|WARN|CancellationException|issue #758|KafkaCodecAllocationBenchmark|44' \
  infra/kafka4/README.md infra/kafka4/README.ko.md \
  benchmark/serializer-benchmark/README.md benchmark/serializer-benchmark/README.ko.md
git diff --check
```

Expected: concepts exist in both locales and no stale “#758 out of scope” remains.

- [ ] **Step 5: Commit documentation**

```bash
git add infra/kafka4/README.md infra/kafka4/README.ko.md \
  benchmark/serializer-benchmark/README.md benchmark/serializer-benchmark/README.ko.md
git commit -m 'Document the real Kafka buffer boundary in both locales' \
  -m 'Constraint: Public docs must separate caller-owned buffers from the unavoidable Kafka ByteArray boundary.
Confidence: high
Scope-risk: narrow
Directive: Keep English and Korean API, logging, and benchmark claims meaning-equivalent.
Tested: Locale concept scan, stale-scope scan, git diff --check
Not-tested: Numeric allocation claims await the committed evidence runs'
```

---

### Task 7: Two Fresh Allocation Runs And Report

**Files:**
- Create: `docs/benchmarks/raw/issue-758/run-*/environment.txt`
- Create: `docs/benchmarks/raw/issue-758/run-*/jmh.json`
- Create: `docs/benchmarks/raw/issue-758/run-*/summary.csv`
- Create: `docs/benchmarks/raw/issue-758/comparison.csv`
- Create: `docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md`
- Modify: `docs/benchmarks/README.md:7-15`

- [ ] **Step 1: Reconfirm generated tasks instead of guessing**

```bash
./gradlew :serializer-benchmark:tasks --all --no-configuration-cache | \
  rg 'benchmarkBenchmark(Compile|Jar)|compileBenchmarkKotlin'
```

Expected: `benchmarkBenchmarkCompile`, `benchmarkBenchmarkJar`, and `compileBenchmarkKotlin` are listed.

- [ ] **Step 2: Build the JMH jar and run a one-iteration smoke**

```bash
./gradlew :serializer-benchmark:clean :serializer-benchmark:benchmarkBenchmarkJar \
  --no-configuration-cache --no-build-cache
java -jar benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/*-JMH.jar \
  '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 1 -wi 1 -i 1 -w 1s -r 1s -prof gc
```

Expected: four methods complete without setup, classpath, overflow, or profiler errors.

- [ ] **Step 3: Run two sequential GC-profiler evidence runs and compare them**

```bash
evidence_root='docs/benchmarks/raw/issue-758'
first_run_id="run-$(date -u +%Y%m%dT%H%M%SZ)"
first_run_dir="$evidence_root/$first_run_id"
mkdir -p "$first_run_dir"
{
  git rev-parse HEAD
  java -version 2>&1
  sw_vers
  sysctl -n machdep.cpu.brand_string
  sysctl -n hw.memsize
} > "$first_run_dir/environment.txt"
java -jar benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/*-JMH.jar \
  '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json \
  -rff "$first_run_dir/jmh.json"
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input "$first_run_dir/jmh.json" --output "$first_run_dir/summary.csv"

second_run_id="run-$(date -u +%Y%m%dT%H%M%SZ)"
second_run_dir="$evidence_root/$second_run_id"
mkdir -p "$second_run_dir"
{
  git rev-parse HEAD
  java -version 2>&1
  sw_vers
  sysctl -n machdep.cpu.brand_string
  sysctl -n hw.memsize
} > "$second_run_dir/environment.txt"
java -jar benchmark/serializer-benchmark/build/benchmarks/benchmark/jars/*-JMH.jar \
  '.*KafkaCodecAllocationBenchmark.*' \
  -t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json \
  -rff "$second_run_dir/jmh.json"
python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py run \
  --input "$second_run_dir/jmh.json" --output "$second_run_dir/summary.csv"

python3 benchmark/serializer-benchmark/scripts/summarize-jmh.py compare \
  --run "$first_run_dir/summary.csv" \
  --run "$second_run_dir/summary.csv" \
  --output "$evidence_root/comparison.csv"
```

Expected: each JSON contains four cells. `comparison.csv` contains only `kafkaSerializeOptimized` and `kafkaDeserializeOptimized`, paired with their ByteArray baselines.

- [ ] **Step 4: Write the report from exact CSV values**

Create `docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md` with these complete sections:

```markdown
# Kafka ByteBuffer Codec Allocation Benchmark - 2026-07-19

## Scope

Issue #758 compares standard Kafka ByteArray codec calls with opt-in caller-owned ByteBuffer calls for the same Kryo-backed BinaryKafkaCodec payload. Broker, network, batching, compression, and header creation costs are excluded.

## Commands

Record the literal Gradle and JMH commands executed in Steps 1-3.

## Run Conditions

Record commit, both run IDs, JDK, OS, CPU, memory, JMH 1.37, one thread, two forks, three warmups, five measurements, one-second windows, and the GC profiler.

## Raw Artifacts

Link both environment files, JMH JSON files, summary CSV files, and the comparison CSV. Record charts as not produced because tables and raw JSON are authoritative.

## Allocation Results

Give serialize and deserialize rows with each run's ByteArray baseline B/op, optimized ByteBuffer B/op, delta, and comparison verdict.

## Diagnostic Throughput

State that throughput scores remain in raw JSON and summaries but do not establish the allocation decision.

## Interpretation

Describe allocation reduction only for a direction whose two fresh runs are each at least five percent lower. Otherwise record the result as inconclusive.

## Limitations

Limit the result to the committed payload, Kryo configuration, caller-owned heap output, bounded direct input, codec-only loop, and measured environment. Do not claim zero-copy Kafka or broker throughput.
```

Use the exact run IDs and numeric values from generated artifacts; do not round a value differently from `comparison.csv` when deciding the verdict.

- [ ] **Step 5: Add the report index row**

```markdown
| [Kafka ByteBuffer Codec Allocation Benchmark](./2026-07-19-kafka-bytebuffer-codec-allocation.md) | `:bluetape4k-kafka4`, issue #758 | [`raw/issue-758/`](./raw/issue-758/) | Not produced |
```

- [ ] **Step 6: Validate raw evidence and wording**

```bash
python3 benchmark/serializer-benchmark/scripts/test_summarize_jmh.py
for result in docs/benchmarks/raw/issue-758/run-*/jmh.json; do
  python3 -m json.tool "$result" >/dev/null
done
rg -n 'gc.alloc.rate.norm|B/op|inconclusive|accepted|throughput|zero-copy|broker' \
  docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md
git diff --check
```

Expected: script tests pass, both JSON files parse, and report claims match comparison verdicts exactly.

- [ ] **Step 7: Commit measured evidence**

```bash
git add docs/benchmarks/README.md \
  docs/benchmarks/2026-07-19-kafka-bytebuffer-codec-allocation.md \
  docs/benchmarks/raw/issue-758
git commit -m 'Ground Kafka buffer claims in repeated allocation evidence' \
  -m 'Constraint: Allocation is primary and both fresh runs must clear the same five-percent threshold.
Confidence: high
Scope-risk: narrow
Directive: Do not generalize codec-only results to broker throughput or zero-copy Kafka.
Tested: Two sequential JMH GC-profiler runs, JSON parsing, summarize-jmh tests, comparison review
Not-tested: Broker, network, compressed codecs, and other payloads are outside this evidence'
```

---

### Task 8: Full Verification, Review, Push, And PR

**Files:**
- Review: every path in `git diff --name-only origin/develop...HEAD`
- Create ignored temporary file: `.omx/issue-758-pr-body.md`

- [ ] **Step 1: Run the complete focused verification sequence**

```bash
repo-test-summary -- ./gradlew \
  :bluetape4k-kafka4:test \
  :serializer-benchmark:test \
  :serializer-benchmark:benchmarkBenchmarkCompile \
  --no-configuration-cache --no-build-cache --rerun-tasks
./gradlew detekt --no-configuration-cache --no-build-cache
python3 benchmark/serializer-benchmark/scripts/test_summarize_jmh.py
git diff origin/develop...HEAD --check
```

Expected: all tests, benchmark compilation, root detekt, Python tests, and whitespace checks pass. Do not invoke nonexistent `:bluetape4k-kafka4:detekt` or module-local API tasks.

- [ ] **Step 2: Perform a fresh material 7-Tier review**

Review the complete branch diff for:

```text
1. Contract correctness: null, position, limit, mark, byte order, identity
2. Error semantics: Exception, CancellationException, Error, buffer failures
3. Logging/security: bounded WARN metadata, no payload/header values, unchanged allowlist
4. Compatibility: standard Kafka ByteArray calls and downstream subclasses
5. Performance: direct BinarySerializer dispatch, no success logging or capturing allocation
6. Tests/evidence: RED proof, raw JSON, two-run threshold, no broker conflation
7. Documentation: English KDoc, rationale comments, EN/KO semantic parity
```

Expected: P0/P1 findings are zero. Fix findings and rerun the smallest affected proof plus Step 1.

- [ ] **Step 3: Verify branch state and push exact head**

```bash
repo-status
git log --oneline origin/develop..HEAD
git push -u origin perf/issue-758-kafka-bytebuffer-codecs
local_head=$(git rev-parse HEAD)
remote_head=$(git rev-parse origin/perf/issue-758-kafka-bytebuffer-codecs)
test "$local_head" = "$remote_head"
```

Expected: clean worktree and matching local/remote SHA.

- [ ] **Step 4: Create the English pull request**

Create `.omx/issue-758-pr-body.md` with:

```markdown
## Summary

- add an opt-in `BufferAwareKafkaCodec` contract without changing Kafka's standard `ByteArray` boundary
- route binary codecs through existing `BinarySerializer` buffer APIs while preserving headers and poison-pill logging
- add codec-only allocation evidence, public KDoc, and equivalent English/Korean documentation

## Validation

- `:bluetape4k-kafka4:test`
- `:serializer-benchmark:test`
- `:serializer-benchmark:benchmarkBenchmarkCompile`
- root `detekt`
- two sequential JMH GC-profiler runs with committed raw JSON and comparison CSV

## Performance Boundary

The report uses `gc.alloc.rate.norm` as the primary metric. It does not claim a zero-copy Kafka boundary, broker throughput improvement, or results beyond the measured Kryo codec cells.

Closes #758
```

Run:

```bash
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head perf/issue-758-kafka-bytebuffer-codecs \
  --title 'Add allocation-aware ByteBuffer APIs to Kafka binary codecs' \
  --body-file .omx/issue-758-pr-body.md
```

Expected: one open PR with base `develop` and the approved feature head.

- [ ] **Step 5: Verify exact-head CI and stop at the merge gate**

```bash
pr_number=$(gh pr view --repo bluetape4k/bluetape4k-projects --json number --jq .number)
gh pr checks "$pr_number" --repo bluetape4k/bluetape4k-projects --watch
local_head=$(git rev-parse HEAD)
remote_head=$(git rev-parse origin/perf/issue-758-kafka-bytebuffer-codecs)
pr_head=$(gh pr view "$pr_number" --repo bluetape4k/bluetape4k-projects --json headRefOid --jq .headRefOid)
test "$local_head" = "$remote_head"
test "$local_head" = "$pr_head"
gh pr view "$pr_number" --repo bluetape4k/bluetape4k-projects \
  --json url,state,mergeStateStatus,reviewDecision,statusCheckRollup,headRefOid
```

Expected: CI/checks and actionable review threads are green and all three heads match. Do not enable auto-merge or merge. Report the exact PR/head and request fresh merge approval; use rebase merge only after approval if the final history remains suitable.
