package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.logging.KLogging
import io.netty.buffer.Unpooled
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.redisson.client.handler.State
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

/**
 * [FastForyCodec]과 [ForyCodec] 간의 와이어 포맷 호환성 검증 테스트.
 *
 * ## 비대칭 호환성 규칙
 * - [FastForyCodec] encode → [FastForyCodec] decode: **성공** (roundtrip)
 * - [ForyCodec] encode → [FastForyCodec] decode: **성공** (FastFory 실패 → Fory fallback)
 * - [FastForyCodec] encode → [ForyCodec] decode: **실패** (Fory decode 실패 → Kryo5 fallback도 실패)
 */
class FastForyCompatibilityTest {

    companion object: KLogging()

    private val fastForyCodec = FastForyCodec()
    private val foryCodec = ForyCodec()

    data class SampleData(
        val id: Int,
        val name: String,
        val value: Double,
    ): java.io.Serializable

    private val testData = SampleData(id = 42, name = "test-data", value = 3.14)

    /**
     * Task 7 - 테스트 1: FastForyCodec roundtrip PASS 검증.
     * FastForyCodec으로 encode한 데이터를 FastForyCodec으로 decode하면 원본과 동일해야 합니다.
     */
    @Test
    fun `FastForyCodec roundtrip should succeed`() {
        val buf = fastForyCodec.valueEncoder.encode(testData)
        try {
            val decoded = fastForyCodec.valueDecoder.decode(buf, State())
            decoded shouldBeEqualTo testData
        } finally {
            buf.release()
        }
    }

    @Test
    fun `FastForyCodec uses direct NIO decode for a single buffer`() {
        val encoded = fastForyCodec.valueEncoder.encode(testData)
        val input = spyk(encoded)

        try {
            val readerIndex = input.readerIndex()
            val readableBytes = input.readableBytes()

            fastForyCodec.valueDecoder.decode(input, State()) shouldBeEqualTo testData

            verify(exactly = 1) { input.nioBufferCount() }
            verify(exactly = 1) { input.nioBuffer(readerIndex, readableBytes) }
        } finally {
            encoded.release()
        }
    }

    @Test
    fun `FastFory direct decode preserves the bounded readable range and caller state`() {
        val payload = byteArrayOf(4, 5, 6)
        val serializer = RecordingForySerializer(
            directResult = { source ->
                source.isReadOnly.shouldBeTrue()
                "direct-fast"
            },
        )
        val copyCalls = AtomicInteger()
        val fallback = RecordingFallbackCodec { error("Unexpected fallback") }
        val codec = FastForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                copiedBytesFactory = {
                    copyCalls.incrementAndGet()
                    copiedReadableBytes(it)
                },
            ),
        )
        val input = framedCodecInput(payload, direct = true)
        val state = CodecInputState.capture(input)

        try {
            codec.valueDecoder.decode(input, State()) shouldBeEqualTo "direct-fast"
            serializer.directCalls shouldBeEqualTo 1
            serializer.copiedCalls shouldBeEqualTo 0
            serializer.directBytes shouldContainSame payload
            copyCalls.get() shouldBeEqualTo 0
            fallback.decodeCalls shouldBeEqualTo 0
            state.shouldRemainUnchanged(input)
        } finally {
            input.release()
        }
    }

    @Test
    fun `FastFory view failure invokes copied primary once`() {
        val serializer = RecordingForySerializer(copiedResult = { "copied-fast" })
        val copyCalls = AtomicInteger()
        val fallback = RecordingFallbackCodec { error("Unexpected fallback") }
        val codec = FastForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                readableViewFactory = { throw CancellationException("view") },
                copiedBytesFactory = {
                    copyCalls.incrementAndGet()
                    copiedReadableBytes(it)
                },
            ),
        )
        val input = framedCodecInput(byteArrayOf(7, 8))
        val state = CodecInputState.capture(input)

        try {
            codec.valueDecoder.decode(input, State()) shouldBeEqualTo "copied-fast"
            serializer.directCalls shouldBeEqualTo 0
            serializer.copiedCalls shouldBeEqualTo 1
            copyCalls.get() shouldBeEqualTo 1
            fallback.decodeCalls shouldBeEqualTo 0
            state.shouldRemainUnchanged(input)
        } finally {
            input.release()
        }
    }

    @Test
    fun `FastFory copied-primary setup failure propagates once without fallback logging`() {
        val copyFailure = IllegalStateException("copy")
        val copyCalls = AtomicInteger()
        val serializer = RecordingForySerializer(copiedResult = { error("Unexpected primary decode") })
        val fallback = RecordingFallbackCodec { error("Unexpected fallback") }
        val codec = FastForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                readableViewFactory = { null },
                copiedBytesFactory = {
                    copyCalls.incrementAndGet()
                    throw copyFailure
                },
            ),
        )
        val input = framedCodecInput(byteArrayOf(7, 8))

        InMemoryLogbackAppender(FastForyCodec::class).use { appender ->
            try {
                val failure = assertFailsWith<IllegalStateException> {
                    codec.valueDecoder.decode(input, State())
                }

                failure shouldBeSameInstanceAs copyFailure
                copyCalls.get() shouldBeEqualTo 1
                serializer.copiedCalls shouldBeEqualTo 0
                serializer.directCalls shouldBeEqualTo 0
                fallback.decodeCalls shouldBeEqualTo 0
                appender.messages.filter { it.startsWith("FastFory decode 실패") } shouldHaveSize 0
            } finally {
                input.release()
            }
        }
    }

    @Test
    fun `FastFory composite input stays on the copied primary route`() {
        val serializer = RecordingForySerializer(copiedResult = { "composite-fast" })
        val codec = FastForyCodec.create(runtime = ForyCodecRuntime(serializerFactory = { serializer }))
        val input = Unpooled.compositeBuffer()
            .addComponents(
                true,
                Unpooled.wrappedBuffer(byteArrayOf(5)),
                Unpooled.wrappedBuffer(byteArrayOf(6)),
            )
        input.markReaderIndex()
        input.markWriterIndex()
        val state = CodecInputState.capture(input)

        try {
            input.nioBufferCount() shouldBeEqualTo 2
            codec.valueDecoder.decode(input, State()) shouldBeEqualTo "composite-fast"
            serializer.directCalls shouldBeEqualTo 0
            serializer.copiedCalls shouldBeEqualTo 1
            state.shouldRemainUnchanged(input)
        } finally {
            input.release()
        }
    }

    @Test
    fun `FastFory direct failures are normalized into runtime fallback once`() {
        val payload = byteArrayOf(1, 4, 9)
        val cancellation = CancellationException("cancel")
        val fatal = AssertionError("fatal")
        val failures = listOf(
            IllegalArgumentException("semantic"),
            cancellation,
            fatal,
            BinarySerializationException("nested", cancellation),
            BinarySerializationException("nested", fatal),
        )

        InMemoryLogbackAppender(FastForyCodec::class).use { appender ->
            failures.forEachIndexed { index, directFailure ->
                val serializer = RecordingForySerializer(directResult = { throw directFailure })
                val copyCalls = AtomicInteger()
                val fallback = RecordingFallbackCodec { "fast-fallback-$index" }
                val codec = FastForyCodec.create(
                    fallbackCodec = fallback,
                    runtime = ForyCodecRuntime(
                        serializerFactory = { serializer },
                        copiedBytesFactory = {
                            copyCalls.incrementAndGet()
                            copiedReadableBytes(it)
                        },
                    ),
                )
                val input = framedCodecInput(payload)
                val state = CodecInputState.capture(input)

                try {
                    codec.valueDecoder.decode(input, State()) shouldBeEqualTo "fast-fallback-$index"
                    serializer.directCalls shouldBeEqualTo 1
                    serializer.copiedCalls shouldBeEqualTo 0
                    copyCalls.get() shouldBeEqualTo 1
                    fallback.decodeCalls shouldBeEqualTo 1
                    fallback.decodedBytes shouldContainSame payload
                    state.shouldRemainUnchanged(input)
                } finally {
                    input.release()
                }
            }

            appender.messages
                .filter { it.startsWith("FastFory decode 실패") } shouldHaveSize failures.size
        }
    }

    @Test
    fun `FastFory copied-primary checked Exception does not cross the runtime catch boundary`() {
        val primary = Exception("checked")
        val serializer = RecordingForySerializer(copiedResult = { throw primary })
        val fallback = RecordingFallbackCodec { error("Unexpected fallback") }
        val codec = FastForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                readableViewFactory = { null },
            ),
        )
        val input = framedCodecInput(byteArrayOf(2))

        try {
            val failure = assertFailsWith<Exception> {
                codec.valueDecoder.decode(input, State())
            }
            failure shouldBeSameInstanceAs primary
            serializer.copiedCalls shouldBeEqualTo 1
            fallback.decodeCalls shouldBeEqualTo 0
        } finally {
            input.release()
        }
    }

    @Test
    fun `FastFory fallback terminal failure preserves its identity`() {
        val primary = IllegalArgumentException("primary")
        val terminal = IllegalStateException("terminal")
        val serializer = RecordingForySerializer(directResult = { throw primary })
        val fallback = RecordingFallbackCodec { throw terminal }
        val codec = FastForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(serializerFactory = { serializer }),
        )
        val input = framedCodecInput(byteArrayOf(3))

        try {
            val failure = assertFailsWith<IllegalStateException> {
                codec.valueDecoder.decode(input, State())
            }
            failure shouldBeSameInstanceAs terminal
            serializer.directCalls shouldBeEqualTo 1
            serializer.copiedCalls shouldBeEqualTo 0
            fallback.decodeCalls shouldBeEqualTo 1
        } finally {
            input.release()
        }
    }

    @Test
    fun `FastFory fallback terminal failure suppresses cleanup failure`() {
        val payload = byteArrayOf(3, 1, 4)
        val primary = IllegalArgumentException("primary")
        val terminal = IllegalStateException("terminal")
        val cleanup = AssertionError("cleanup")
        val serializer = RecordingForySerializer(directResult = { throw primary })
        val fallback = RecordingFallbackCodec { throw terminal }
        val fallbackBuffer = spyk(Unpooled.wrappedBuffer(payload))
        every { fallbackBuffer.release() } throws cleanup
        val codec = FastForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                fallbackBufferFactory = { fallbackBuffer },
            ),
        )
        val input = framedCodecInput(payload)

        try {
            val failure = assertFailsWith<IllegalStateException> {
                codec.valueDecoder.decode(input, State())
            }

            failure shouldBeSameInstanceAs terminal
            failure.suppressed.toList() shouldContainSame listOf(cleanup)
            serializer.directCalls shouldBeEqualTo 1
            serializer.copiedCalls shouldBeEqualTo 0
            fallback.decodeCalls shouldBeEqualTo 1
            verify(exactly = 1) { fallbackBuffer.release() }
        } finally {
            input.release()
        }
    }

    /**
     * Task 7 - 테스트 2 (방향 A): ForyCodec encode → FastForyCodec decode 성공 검증.
     * ForyCodec(COMPATIBLE 모드)으로 encode한 데이터를 FastForyCodec으로 decode할 때
     * FastFory 직접 decode가 실패하더라도 Fory fallback을 통해 성공해야 합니다.
     */
    @Test
    fun `ForyCodec encoded data should be decodable by FastForyCodec via fallback`() {
        val buf = foryCodec.valueEncoder.encode(testData)
        val bytes = ByteArray(buf.readableBytes())
        buf.getBytes(buf.readerIndex(), bytes)
        buf.release()

        // FastForyCodec은 Fory fallback을 통해 ForyCodec 인코딩 데이터를 읽을 수 있습니다
        val decodeBuf = Unpooled.wrappedBuffer(bytes)
        try {
            val decoded = fastForyCodec.valueDecoder.decode(decodeBuf, State())
            decoded shouldBeEqualTo testData
        } finally {
            decodeBuf.release()
        }
    }

    /**
     * M3: copy-constructor 경로 검증 — Redisson 동적 인스턴스화 시 사용되는 경로.
     */
    @Test
    fun `copy-constructor(classLoader, codec) should produce functional codec`() {
        val classLoader = Thread.currentThread().contextClassLoader
        val copied = FastForyCodec(classLoader, fastForyCodec)

        val buf = copied.valueEncoder.encode(testData)
        try {
            val decoded = copied.valueDecoder.decode(buf, State())
            decoded shouldBeEqualTo testData
        } finally {
            buf.release()
        }
    }

    @Test
    fun `copy constructor preserves injected FastFory runtime configuration`() {
        val serializer = RecordingForySerializer(directResult = { "copied-runtime" })
        val codec = FastForyCodec.create(runtime = ForyCodecRuntime(serializerFactory = { serializer }))
        val copied = FastForyCodec(Thread.currentThread().contextClassLoader, codec)
        val input = framedCodecInput(byteArrayOf(6))

        try {
            copied.valueDecoder.decode(input, State()) shouldBeEqualTo "copied-runtime"
            serializer.directCalls shouldBeEqualTo 1
        } finally {
            input.release()
        }
    }

    /**
     * Task 7 - 테스트 3 (방향 B 고정): FastForyCodec encode → ForyCodec decode 비호환 검증.
     *
     * FastForyCodec(SCHEMA_CONSISTENT)으로 encode한 데이터는 ForyCodec(COMPATIBLE)으로 올바르게
     * 복원되지 않습니다. ForyCodec 내부에서 예외를 잡고 fallback(Kryo5)도 실패하며 null을 반환합니다.
     * 즉, 원본 객체와 동일한 값을 얻을 수 없습니다.
     *
     * ⚠️ 비대칭 호환성: ForyCodec이 FastFory 데이터를 decode할 때 예외 없이 null을 반환합니다.
     */
    @Test
    fun `FastForyCodec encoded data cannot be correctly decoded by ForyCodec`() {
        val buf = fastForyCodec.valueEncoder.encode(testData)
        val bytes = ByteArray(buf.readableBytes())
        buf.getBytes(buf.readerIndex(), bytes)
        buf.release()

        // ForyCodec은 FastFory 포맷을 COMPATIBLE decode 실패 → Kryo5 fallback도 실패 → null 반환
        val decodeBuf = Unpooled.wrappedBuffer(bytes)
        try {
            val decoded = foryCodec.valueDecoder.decode(decodeBuf, State())
            decoded shouldNotBeEqualTo testData
        } finally {
            decodeBuf.release()
        }
    }
}
