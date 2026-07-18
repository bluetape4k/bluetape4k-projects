package io.bluetape4k.kafka.codec

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.annotations.BluetapeDelicateApi
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.io.serializer.AbstractBinarySerializer
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.CompressableBinarySerializer
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.CancellationException
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ReadOnlyBufferException
import java.util.stream.Stream

class BinaryKafkaCodecBufferTest {
    companion object {
        @JvmStatic
        @OptIn(BluetapeDelicateApi::class)
        fun compressedCodecs(): Stream<Arguments> = Stream.of(
            Arguments.of("LZ4 Kryo", LZ4KryoKafkaCodec()),
            Arguments.of("LZ4 Fory", LZ4ForyKafkaCodec()),
            Arguments.of("Snappy Kryo", SnappyKryoKafkaCodec()),
            Arguments.of("Snappy Fory", SnappyForyKafkaCodec()),
            Arguments.of("Zstd Kryo", ZstdKryoKafkaCodec()),
            Arguments.of("Zstd Fory", ZstdForyKafkaCodec()),
        )

        @JvmStatic
        fun nestedControlFailures(): Stream<Arguments> = Stream.of(
            Arguments.of("cancellation", CancellationException("cancelled")),
            Arguments.of("error", AssertionError("fatal")),
        )
    }

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

    private class WrappingArraySerializer(private val failure: Throwable): AbstractBinarySerializer() {
        override fun doSerialize(graph: Any): ByteArray = throw failure
        override fun <T: Any> doDeserialize(bytes: ByteArray): T? = throw failure
    }

    private class TestBinaryCodec(
        serializer: BinarySerializer,
        override val writeValueTypeHeader: Boolean = true,
    ): BinaryKafkaCodec(serializer)

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedControlFailures")
    fun `compressed Kafka buffer output restores nested control failure identity`(
        @Suppress("UNUSED_PARAMETER") name: String,
        failure: Throwable,
    ) {
        val codec = TestBinaryCodec(
            CompressableBinarySerializer(WrappingArraySerializer(failure), Compressors.LZ4),
        )

        val actual = assertFailsWith<Throwable> {
            codec.serializeTo("events", "payload", ByteBuffer.allocate(1024))
        }

        actual shouldBeSameInstanceAs failure
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nestedControlFailures")
    fun `compressed Kafka buffer input restores nested control failure identity`(
        @Suppress("UNUSED_PARAMETER") name: String,
        failure: Throwable,
    ) {
        val codec = TestBinaryCodec(
            CompressableBinarySerializer(WrappingArraySerializer(failure), Compressors.LZ4),
        )
        val wire = Compressors.LZ4.compress(byteArrayOf(1))

        val actual = assertFailsWith<Throwable> {
            codec.deserializeFrom("events", ByteBuffer.wrap(wire).asReadOnlyBuffer())
        }

        actual shouldBeSameInstanceAs failure
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("compressedCodecs")
    fun `compressed codecs preserve standard and buffer wire compatibility`(
        @Suppress("UNUSED_PARAMETER") name: String,
        codec: BufferAwareKafkaCodec<Any?>,
    ) {
        val payload = "trusted-compressible-kafka-payload-".repeat(256)
        val standardWire = requireNotNull(codec.serialize("events", payload))
        val source = ByteBuffer.allocateDirect(standardWire.size + 8).apply {
            position(3)
            put(standardWire)
            flip()
            position(3)
            limit(3 + standardWire.size)
        }.slice().asReadOnlyBuffer().apply { mark() }
        val sourcePosition = source.position()
        val sourceLimit = source.limit()

        codec.deserializeFrom("events", source) shouldBeEqualTo payload

        source.position() shouldBeEqualTo sourcePosition
        source.limit() shouldBeEqualTo sourceLimit
        source.reset().position() shouldBeEqualTo sourcePosition

        val target = ByteBuffer.allocateDirect(64 * 1024).apply {
            position(7)
            limit(capacity() - 11)
        }
        val targetStart = target.position()
        val targetLimit = target.limit()
        val written = codec.serializeTo("events", payload, target)
        val bufferWire = target.duplicate().apply {
            position(targetStart)
            limit(targetStart + written)
        }.let { view -> ByteArray(view.remaining()).also(view::get) }

        written shouldBeGreaterThan 0
        target.position() shouldBeEqualTo targetStart + written
        target.limit() shouldBeEqualTo targetLimit
        codec.deserialize("events", bufferWire) shouldBeEqualTo payload
    }

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
            "events", headers, "value", ByteBuffer.allocate(32),
        )

        headers.lastHeader("trace-id").value().toUtf8String() shouldBeEqualTo "trace-value"
        headers.lastHeader(AbstractKafkaCodec.VALUE_TYPE_KEY).value().toUtf8String() shouldBeEqualTo
            String::class.java.name

        val noHeader = RecordHeaders()
        TestBinaryCodec(RecordingSerializer(), writeValueTypeHeader = false).serializeTo(
            "events", noHeader, "value", ByteBuffer.allocate(32),
        )
        noHeader.lastHeader(AbstractKafkaCodec.VALUE_TYPE_KEY).shouldBeNull()
    }

    @Test
    fun `serialization failure keeps header and propagates identity`() {
        val failure = IllegalStateException("encode failed")
        val headers = RecordHeaders()

        assertFailsWith<IllegalStateException> {
            TestBinaryCodec(ThrowingSerializer(failure)).serializeTo(
                "events", headers, "value", ByteBuffer.allocate(32),
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
                RecordHeaders().add("trace-id", "secret-header".encodeToByteArray()),
                ByteBuffer.allocate(17),
            ).shouldBeNull()
            val event = appender.list.single()
            event.level shouldBeEqualTo Level.WARN
            event.throwableProxy.shouldBeNull()
            val message = event.formattedMessage
            message.contains("topic=events") shouldBeEqualTo true
            message.contains("trace-id") shouldBeEqualTo true
            message.contains("dataSize=17") shouldBeEqualTo true
            message.contains("failureType=${IllegalArgumentException::class.java.name}") shouldBeEqualTo true
            message.contains("secret-header") shouldBeEqualTo false
            message.contains("secret-payload") shouldBeEqualTo false
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    @Test
    fun `buffer poison WARN bounds metadata and neutralizes log injection characters`() {
        val topic = "topic\r\n\t\u0000" + "T".repeat(256) + "TOPIC-TAIL"
        val headers = RecordHeaders().apply {
            repeat(20) { index ->
                val key = "key-${index.toString().padStart(2, '0')}-" +
                    "K".repeat(80) + "\r\n\t\u0001KEY-TAIL"
                add(key, "secret-header-value-$index".encodeToByteArray())
            }
        }
        val codec = TestBinaryCodec(
            ThrowingSerializer(IllegalArgumentException("secret-payload-message\r\nforged")),
        )
        val logger = AbstractKafkaCodec.log as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            codec.deserializeFrom(topic, headers, ByteBuffer.allocate(17)).shouldBeNull()

            val event = appender.list.single()
            val message = event.formattedMessage
            event.level shouldBeEqualTo Level.WARN
            event.throwableProxy.shouldBeNull()
            (message.length <= 1600) shouldBeEqualTo true
            message.none(Char::isISOControl) shouldBeEqualTo true
            message.contains("TOPIC-TAIL") shouldBeEqualTo false
            message.contains("key-15-") shouldBeEqualTo true
            message.contains("key-16-") shouldBeEqualTo false
            message.contains("KEY-TAIL") shouldBeEqualTo false
            message.contains("secret-header-value") shouldBeEqualTo false
            message.contains("secret-payload") shouldBeEqualTo false
            message.contains("failureType=${IllegalArgumentException::class.java.name}") shouldBeEqualTo true
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
