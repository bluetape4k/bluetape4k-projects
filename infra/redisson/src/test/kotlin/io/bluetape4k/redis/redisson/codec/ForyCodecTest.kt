package io.bluetape4k.redis.redisson.codec

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("ForyCodec encode/decode & fallback")
class ForyCodecTest {

    companion object: KLogging()

    data class Sample(val id: Long, val name: String, val tags: List<String>): java.io.Serializable

    @Test
    fun `Fory roundtrip preserves the value`() {
        val codec = ForyCodec()
        val original = Sample(42L, "alice", listOf("a", "b", "c"))

        val buf = codec.valueEncoder.encode(original)
        try {
            codec.valueDecoder.decode(buf, null) shouldBeEqualTo original
        } finally {
            buf.release()
        }
    }

    @Test
    fun `single NIO buffer is decoded without a copied handoff`() {
        val codec = ForyCodec()
        val original = Sample(43L, "direct", listOf("nio"))
        val encoded = codec.valueEncoder.encode(original)
        val input = spyk(encoded)

        try {
            val readerIndex = input.readerIndex()
            val readableBytes = input.readableBytes()

            codec.valueDecoder.decode(input, null) shouldBeEqualTo original

            verify(exactly = 1) { input.nioBufferCount() }
            verify(exactly = 1) { input.nioBuffer(readerIndex, readableBytes) }
        } finally {
            encoded.release()
        }
    }

    @Test
    fun `direct decode sees only the readable range and preserves caller state`() {
        val payload = byteArrayOf(1, 3, 5, 7)
        val serializer = RecordingForySerializer(
            directResult = { source ->
                source.isReadOnly.shouldBeTrue()
                "direct"
            },
        )
        val copyCalls = AtomicInteger()
        val fallback = RecordingFallbackCodec { error("Unexpected fallback") }
        val codec = ForyCodec.create(
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
            codec.valueDecoder.decode(input, null) shouldBeEqualTo "direct"

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
    fun `view failure uses copied primary exactly once without fallback`() {
        val payload = byteArrayOf(2, 4, 6, 8)
        val serializer = RecordingForySerializer(copiedResult = { "copied" })
        val copyCalls = AtomicInteger()
        val fallback = RecordingFallbackCodec { error("Unexpected fallback") }
        val codec = ForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                readableViewFactory = { throw AssertionError("view") },
                copiedBytesFactory = {
                    copyCalls.incrementAndGet()
                    copiedReadableBytes(it)
                },
            ),
        )
        val input = framedCodecInput(payload)
        val state = CodecInputState.capture(input)

        try {
            codec.valueDecoder.decode(input, null) shouldBeEqualTo "copied"

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
    fun `copied-primary setup failure propagates once without fallback logging`() {
        val copyFailure = IllegalStateException("copy")
        val copyCalls = AtomicInteger()
        val serializer = RecordingForySerializer(copiedResult = { error("Unexpected primary decode") })
        val fallback = RecordingFallbackCodec { error("Unexpected fallback") }
        val codec = ForyCodec.create(
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
        val input = framedCodecInput(byteArrayOf(2, 4))

        InMemoryLogbackAppender(ForyCodec::class).use { appender ->
            try {
                val failure = assertFailsWith<IllegalStateException> {
                    codec.valueDecoder.decode(input, null)
                }

                failure shouldBeSameInstanceAs copyFailure
                copyCalls.get() shouldBeEqualTo 1
                serializer.copiedCalls shouldBeEqualTo 0
                serializer.directCalls shouldBeEqualTo 0
                fallback.decodeCalls shouldBeEqualTo 0
                appender.messages
                    .filter { it.startsWith("Decoding: Value is not suitable for ForyCodec.") } shouldHaveSize 0
            } finally {
                input.release()
            }
        }
    }

    @Test
    fun `direct failure normalization preserves legacy message cause and logging`() {
        val semantic = IllegalArgumentException("semantic")
        val cancellation = CancellationException("cancel")
        val fatal = AssertionError("fatal")
        val nestedCause = IllegalStateException("nested-cause")
        val alreadyWrapped = BinarySerializationException("already-wrapped", nestedCause)
        val failures = listOf(semantic, cancellation, fatal, alreadyWrapped)

        InMemoryLogbackAppender(ForyCodecDecodeSupport.log.name).use { appender ->
            failures.forEach { directFailure ->
                val serializer = RecordingForySerializer(directResult = { throw directFailure })
                val failure = assertFailsWith<BinarySerializationException> {
                    serializer.deserializeDirectWithLegacyNormalization(ByteBuffer.wrap(byteArrayOf(1, 2)), 2)
                }

                failure.message shouldBeEqualTo "Fail to deserialize. bytesSize=2"
                failure.cause shouldBeSameInstanceAs
                    if (directFailure === alreadyWrapped) nestedCause else directFailure
            }

            appender.messages
                .filter { it.contains("Fail to deserialize. throw BinarySerializationException.") } shouldHaveSize 3
        }
    }

    @Test
    fun `composite input stays on the copied primary route`() {
        val serializer = RecordingForySerializer(copiedResult = { "composite" })
        val codec = ForyCodec.create(runtime = ForyCodecRuntime(serializerFactory = { serializer }))
        val input = Unpooled.compositeBuffer()
            .addComponents(
                true,
                Unpooled.wrappedBuffer(byteArrayOf(1, 2)),
                Unpooled.wrappedBuffer(byteArrayOf(3, 4)),
            )
        input.markReaderIndex()
        input.markWriterIndex()
        val state = CodecInputState.capture(input)

        try {
            input.nioBufferCount() shouldBeEqualTo 2
            codec.valueDecoder.decode(input, null) shouldBeEqualTo "composite"
            serializer.directCalls shouldBeEqualTo 0
            serializer.copiedCalls shouldBeEqualTo 1
            state.shouldRemainUnchanged(input)
        } finally {
            input.release()
        }
    }

    @Test
    fun `direct failures are normalized once before Fory fallback`() {
        val payload = byteArrayOf(9, 8, 7)
        val cancellation = CancellationException("cancel")
        val fatal = AssertionError("fatal")
        val failures = listOf(
            IllegalArgumentException("semantic"),
            cancellation,
            fatal,
            BinarySerializationException("nested", cancellation),
            BinarySerializationException("nested", fatal),
        )

        InMemoryLogbackAppender(ForyCodec::class).use { appender ->
            failures.forEachIndexed { index, directFailure ->
                val serializer = RecordingForySerializer(directResult = { throw directFailure })
                val copyCalls = AtomicInteger()
                val fallback = RecordingFallbackCodec { "fallback-$index" }
                val codec = ForyCodec.create(
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
                    codec.valueDecoder.decode(input, null) shouldBeEqualTo "fallback-$index"
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
                .filter { it.startsWith("Decoding: Value is not suitable for ForyCodec.") } shouldHaveSize failures.size
        }
    }

    @Test
    fun `Fory copied-primary Exception reaches fallback but Fast boundary is not implied`() {
        val primary = Exception("copied-primary")
        val serializer = RecordingForySerializer(copiedResult = { throw primary })
        val fallback = RecordingFallbackCodec { "fallback" }
        val codec = ForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                readableViewFactory = { null },
            ),
        )
        val input = framedCodecInput(byteArrayOf(5))

        try {
            codec.valueDecoder.decode(input, null) shouldBeEqualTo "fallback"
            serializer.copiedCalls shouldBeEqualTo 1
            fallback.decodeCalls shouldBeEqualTo 1
        } finally {
            input.release()
        }
    }

    @Test
    fun `fallback terminal failure keeps identity and suppresses cleanup failure`() {
        val payload = byteArrayOf(3, 2, 1)
        val primary = IllegalArgumentException("primary")
        val terminal = IllegalStateException("terminal")
        val cleanup = AssertionError("cleanup")
        val serializer = RecordingForySerializer(directResult = { throw primary })
        val fallback = RecordingFallbackCodec { throw terminal }
        val fallbackBuffer = spyk(Unpooled.wrappedBuffer(payload))
        every { fallbackBuffer.release() } throws cleanup
        val codec = ForyCodec.create(
            fallbackCodec = fallback,
            runtime = ForyCodecRuntime(
                serializerFactory = { serializer },
                fallbackBufferFactory = { fallbackBuffer },
            ),
        )
        val input = framedCodecInput(payload)

        try {
            val failure = assertFailsWith<IllegalStateException> {
                codec.valueDecoder.decode(input, null)
            }

            failure shouldBeSameInstanceAs terminal
            failure.cause shouldBeSameInstanceAs terminal.cause
            failure.suppressed.toList() shouldContainSame listOf(cleanup)
            serializer.directCalls shouldBeEqualTo 1
            serializer.copiedCalls shouldBeEqualTo 0
            fallback.decodeCalls shouldBeEqualTo 1
            verify(exactly = 1) { fallbackBuffer.release() }
        } finally {
            input.release()
        }
    }

    @Test
    fun `copy constructor preserves injected runtime configuration`() {
        val serializer = RecordingForySerializer(directResult = { "copied-config" })
        val codec = ForyCodec.create(runtime = ForyCodecRuntime(serializerFactory = { serializer }))
        val copied = ForyCodec(Thread.currentThread().contextClassLoader, codec)
        val input = framedCodecInput(byteArrayOf(1))

        try {
            copied.valueDecoder.decode(input, null) shouldBeEqualTo "copied-config"
            serializer.directCalls shouldBeEqualTo 1
        } finally {
            input.release()
        }
    }

    @Test
    fun `Kryo5 bytes are decoded through the configured fallback`() {
        val fallbackCodec = RedissonCodecs.Kryo5
        val codec = ForyCodec(fallbackCodec)
        val original = Sample(99L, "bob", listOf("x", "y"))
        val encoded = fallbackCodec.valueEncoder.encode(original)

        try {
            codec.valueDecoder.decode(encoded, null) shouldBeEqualTo original
        } finally {
            encoded.release()
        }
    }

    @Test
    fun `Encode path returns a decodable buffer`() {
        val codec = ForyCodec(RedissonCodecs.Kryo5)
        val buf = codec.valueEncoder.encode("simple-string")

        try {
            codec.valueDecoder.decode(buf, null) shouldBeEqualTo "simple-string"
        } finally {
            buf.release()
        }
    }

    @Test
    fun `ForyCodec exposes all encoder and decoder surfaces`() {
        val codec = ForyCodec()
        codec.valueEncoder.shouldNotBeNull()
        codec.valueDecoder.shouldNotBeNull()
        codec.mapKeyEncoder.shouldNotBeNull()
        codec.mapKeyDecoder.shouldNotBeNull()
        codec.mapValueEncoder.shouldNotBeNull()
        codec.mapValueDecoder.shouldNotBeNull()
    }

    @Test
    fun `ClassLoader constructor keeps the default Kryo5 fallback`() {
        val codec = ForyCodec(this::class.java.classLoader)
        val buf = codec.valueEncoder.encode("hello")

        try {
            codec.valueDecoder.decode(buf, null) shouldBeEqualTo "hello"
        } finally {
            buf.release()
        }
    }
}
