package io.bluetape4k.kafka.codec

import ch.qos.logback.classic.Level
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
                RecordHeaders().add("trace-id", byteArrayOf(1)),
                ByteBuffer.allocate(17),
            ).shouldBeNull()
            val event = appender.list.single()
            event.level shouldBeEqualTo Level.WARN
            val message = event.formattedMessage
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
