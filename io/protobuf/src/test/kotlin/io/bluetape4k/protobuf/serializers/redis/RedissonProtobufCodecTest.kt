@file:Suppress("DEPRECATION")

package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.protobuf.redis.messages.RedisSimpleMessage
import io.bluetape4k.protobuf.redis.messages.copy
import io.bluetape4k.protobuf.redis.messages.redisNestedMessage
import io.bluetape4k.protobuf.redis.messages.redisSimpleMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.SwappedByteBuf
import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.redisson.client.codec.BaseCodec
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder
import java.io.Serializable
import java.nio.ByteBuffer
import java.time.Instant

class RedissonProtobufCodecTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    private fun getStrictTestCodecs() = listOf(
        Arguments.of(RedissonProtobufCodecs.Protobuf),
        Arguments.of(RedissonProtobufCodecs.GzipProtobuf),
        Arguments.of(RedissonProtobufCodecs.GzipProtobufComposite),
        Arguments.of(RedissonProtobufCodecs.LZ4Protobuf),
        Arguments.of(RedissonProtobufCodecs.LZ4ProtobufComposite),
        Arguments.of(RedissonProtobufCodecs.SnappyProtobuf),
        Arguments.of(RedissonProtobufCodecs.SnappyProtobufComposite),
        Arguments.of(RedissonProtobufCodecs.ZstdProtobuf),
        Arguments.of(RedissonProtobufCodecs.ZstdProtobufComposite),
    )

    private fun getTrustedInternalTestCodecs() = listOf(
        Arguments.of(RedissonProtobufCodecs.TrustedInternalProtobuf),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalGzipProtobuf),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalGzipProtobufComposite),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalLZ4Protobuf),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalLZ4ProtobufComposite),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalSnappyProtobuf),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalSnappyProtobufComposite),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalZstdProtobuf),
        Arguments.of(RedissonProtobufCodecs.TrustedInternalZstdProtobufComposite),
    )

    data class CustomData(
        val id: Int,
        val name: String,
    ): Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private class SentinelFallbackCodec(
        private val decoded: Any,
    ): BaseCodec() {
        private val encoder = Encoder { Unpooled.EMPTY_BUFFER }
        private val decoder = Decoder<Any> { _, _ -> decoded }

        override fun getValueEncoder(): Encoder = encoder

        override fun getValueDecoder(): Decoder<Any> = decoder
    }

    private class TrackingFallbackCodec(
        private val result: Any? = null,
        private val failure: Throwable? = null,
    ): BaseCodec() {
        var seen: ByteBuf? = null
            private set

        private val decoder = Decoder<Any> { input, _ ->
            seen = input
            failure?.let { throw it }
            checkNotNull(result)
        }

        override fun getValueEncoder(): Encoder = Encoder { Unpooled.EMPTY_BUFFER }

        override fun getValueDecoder(): Decoder<Any> = decoder
    }

    private class TrackingNioViewByteBuf(delegate: ByteBuf): SwappedByteBuf(delegate) {
        var boundedNioCalls: Int = 0
            private set

        override fun nioBuffer(index: Int, length: Int): ByteBuffer {
            boundedNioCalls++
            index shouldBeEqualTo readerIndex()
            length shouldBeEqualTo readableBytes()
            return super.nioBuffer(index, length)
        }
    }

    private class ThrowingNioViewByteBuf(
        delegate: ByteBuf,
        private val failure: Error,
    ): SwappedByteBuf(delegate) {
        override fun nioBuffer(index: Int, length: Int): ByteBuffer = throw failure
    }

    private enum class HostileBehavior {
        RETAIN_AND_RETURN,
        EARLY_RELEASE_AND_RETURN,
        RETURN_SLICE,
        THROW_AFTER_RETAIN,
    }

    private class HostileFallbackCodec(
        private val behavior: HostileBehavior,
    ): BaseCodec() {
        val sentinel = IllegalStateException("throw-after-retain")
        var calls: Int = 0
            private set
        var seen: ByteBuf? = null
            private set

        override fun getValueEncoder(): Encoder = Encoder { Unpooled.EMPTY_BUFFER }

        override fun getValueDecoder(): Decoder<Any> = Decoder { input, _ ->
            calls++
            seen = input
            when (behavior) {
                HostileBehavior.RETAIN_AND_RETURN -> input.retain().let { "retained" }
                HostileBehavior.EARLY_RELEASE_AND_RETURN -> input.release().let { "released" }
                HostileBehavior.RETURN_SLICE -> input.slice()
                HostileBehavior.THROW_AFTER_RETAIN -> {
                    input.retain()
                    throw sentinel
                }
            }
        }
    }

    private class ThrowOnFinalReleaseByteBuf(
        private val delegate: ByteBuf,
        val cleanupFailure: Throwable,
    ): SwappedByteBuf(delegate) {
        override fun release(): Boolean {
            if (refCnt() == 1) throw cleanupFailure
            return super.release()
        }

        fun forceReleaseDelegate() {
            while (delegate.refCnt() > 0) delegate.release()
        }
    }

    private inline fun <T> ByteBuf.withPreservedDecoderState(block: () -> T): T {
        val originalReaderIndex = readerIndex()
        val originalWriterIndex = writerIndex()
        val originalRefCnt = refCnt()
        return try {
            block()
        } finally {
            readerIndex() shouldBeEqualTo originalReaderIndex
            writerIndex() shouldBeEqualTo originalWriterIndex
            refCnt() shouldBeEqualTo originalRefCnt
        }
    }

    private fun Instant.toProtobufTimestamp(): com.google.protobuf.Timestamp {
        val source = this
        return timestamp {
            this.seconds = source.epochSecond
            this.nanos = source.nano
        }
    }

    private fun newSimpleMessage() = redisSimpleMessage {
        id = faker.random().nextLong()
        name = faker.name().fullName()
        description = Fakers.randomString(1024, 4096)
        timestamp = Instant.now().toProtobufTimestamp()
    }

    private fun newNestedMessage() = redisNestedMessage {
        id = faker.random().nextLong()
        name = faker.name().fullName()
        dayOfTheWeek = io.bluetape4k.protobuf.redis.messages.DayOfTheWeek.FRIDAY
        optionalMessage = newSimpleMessage()
        nestedMessages.add(newSimpleMessage().copy { id = faker.random().nextLong() })
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Codec.verifyCodec(origin: T) {
        val buf = valueEncoder.encode(origin)
        try {
            val actual = valueDecoder.decode(buf, State()) as? T
            actual shouldBeEqualTo origin
        } finally {
            buf.release()
        }
    }

    @ParameterizedTest(name = "codec for simple string with {0}")
    @MethodSource("getTrustedInternalTestCodecs")
    fun `codec for simple string with fallback codec`(codec: Codec) {
        val origin = "Hello world! 동해물과 백두산이"
        codec.verifyCodec(origin)
    }

    @ParameterizedTest(name = "codec for kotlin data class with {0}")
    @MethodSource("getTrustedInternalTestCodecs")
    fun `codec for kotlin data class with fallback codec`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            val origin = CustomData(faker.random().nextInt(), faker.name().fullName())
            codec.verifyCodec(origin)
        }
    }

    @ParameterizedTest(name = "codec for protobuf simple message with {0}")
    @MethodSource("getStrictTestCodecs")
    fun `codec for protobuf simple message`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            codec.verifyCodec(newSimpleMessage())
        }
    }

    @ParameterizedTest(name = "codec for protobuf nested message with {0}")
    @MethodSource("getStrictTestCodecs")
    fun `codec for protobuf nested message`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            codec.verifyCodec(newNestedMessage())
        }
    }

    @Test
    fun `default codec rejects non-protobuf values by default`() {
        val codec = RedissonProtobufCodec()

        assertFailsWith<IllegalArgumentException> {
            codec.valueEncoder.encode("Hello world! 동해물과 백두산이")
        }
    }

    @Test
    fun `default codec rejects non-protobuf bytes by default`() {
        val trustedInternalCodec = RedissonProtobufCodec.trustedInternal()
        val fallbackBytes = trustedInternalCodec.valueEncoder.encode("fallback-bytes")
        val strictCodec = RedissonProtobufCodec()

        try {
            assertFailsWith<SecurityException> {
                strictCodec.valueDecoder.decode(fallbackBytes, State())
            }
        } finally {
            fallbackBytes.release()
        }
    }

    @Test
    fun `protobuf encoder writes packed message directly to byte buffer`() {
        val codec = RedissonProtobufCodec()
        val origin = newSimpleMessage()

        val buf = codec.encodeProtobufMessage(origin)
        try {
            buf.readableBytes() shouldBeEqualTo AnyMessage.pack(origin).serializedSize
        } finally {
            buf.release()
        }
    }

    @Test
    fun `strict contiguous heap and direct decode use one bounded nio view`() {
        val codec = RedissonProtobufCodec()
        val origin = newSimpleMessage()
        val encoded = codec.valueEncoder.encode(origin)
        val wire = try {
            ByteArray(encoded.readableBytes()).also {
                encoded.getBytes(encoded.readerIndex(), it)
            }
        } finally {
            encoded.release()
        }
        val delegates = listOf(
            Unpooled.wrappedBuffer(byteArrayOf(0x11, 0x22) + wire + byteArrayOf(0x33)),
            Unpooled.directBuffer(wire.size + 3).apply {
                writeByte(0x11).writeByte(0x22).writeBytes(wire).writeByte(0x33)
            },
        )

        delegates.forEach { delegate ->
            delegate.setIndex(2, 2 + wire.size)
            val tracked = TrackingNioViewByteBuf(delegate)
            val decoded = try {
                tracked.withPreservedDecoderState {
                    codec.valueDecoder.decode(tracked, State()) as RedisSimpleMessage
                }
            } finally {
                tracked.release()
            }
            tracked.boundedNioCalls shouldBeEqualTo 1
            decoded shouldBeEqualTo origin
            decoded.name shouldBeEqualTo origin.name
        }
    }

    @Test
    fun `strict decode preserves contiguous input indices and reference count`() {
        val codec = RedissonProtobufCodec()
        val origin = newSimpleMessage()
        val encoded = codec.valueEncoder.encode(origin)
        val decoded = try {
            encoded.withPreservedDecoderState {
                codec.valueDecoder.decode(encoded, State()) as RedisSimpleMessage
            }
        } finally {
            encoded.release()
        }

        decoded shouldBeEqualTo origin
        decoded.description shouldBeEqualTo origin.description
    }

    @Test
    fun `strict malformed and truncated Any use one bounded view without mutating input`() {
        listOf(
            byteArrayOf(1, 2, 3),
            byteArrayOf(0x0A, 0x05, 0x01),
        ).forEach { bytes ->
            val tracked = TrackingNioViewByteBuf(Unpooled.wrappedBuffer(bytes))
            try {
                tracked.withPreservedDecoderState {
                    assertFailsWith<SecurityException> {
                        RedissonProtobufCodec().valueDecoder.decode(tracked, State())
                    }
                }
                tracked.boundedNioCalls shouldBeEqualTo 1
            } finally {
                tracked.release()
            }
        }
    }

    @Test
    fun `composite input uses the compatibility copy and still decodes`() {
        val codec = RedissonProtobufCodec()
        val origin = newSimpleMessage()
        val encoded = codec.valueEncoder.encode(origin)
        val bytes = try {
            ByteArray(encoded.readableBytes()).also {
                encoded.getBytes(encoded.readerIndex(), it)
            }
        } finally {
            encoded.release()
        }
        val split = bytes.size / 2
        val composite = Unpooled.compositeBuffer().addComponents(
            true,
            Unpooled.wrappedBuffer(bytes, 0, split),
            Unpooled.wrappedBuffer(bytes, split, bytes.size - split),
        )
        val tracked = TrackingNioViewByteBuf(composite)
        val decoded = try {
            tracked.withPreservedDecoderState {
                codec.valueDecoder.decode(tracked, State()) as RedisSimpleMessage
            }
        } finally {
            tracked.release()
        }

        tracked.boundedNioCalls shouldBeEqualTo 0
        decoded shouldBeEqualTo origin
        decoded.name shouldBeEqualTo origin.name
    }

    @Test
    fun `trusted fallback releases its isolated copy on success and failure`() {
        val operationFailure = IllegalStateException("boom")
        listOf(
            TrackingFallbackCodec(result = "fallback") to null,
            TrackingFallbackCodec(failure = operationFailure) to operationFailure,
        ).forEach { (fallback, expectedFailure) ->
            val codec = RedissonProtobufCodec(fallback)
            val input = TrackingNioViewByteBuf(Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3)))
            try {
                input.withPreservedDecoderState {
                    if (expectedFailure == null) {
                        codec.valueDecoder.decode(input, State()) shouldBeEqualTo "fallback"
                    } else {
                        val actual = assertFailsWith<IllegalStateException> {
                            codec.valueDecoder.decode(input, State())
                        }
                        (actual === expectedFailure) shouldBeEqualTo true
                    }
                }
                input.boundedNioCalls shouldBeEqualTo 1
                fallback.seen!!.refCnt() shouldBeEqualTo 0
            } finally {
                input.release()
            }
        }
    }

    @Test
    fun `wrong protobuf message payload falls back without mutating input`() {
        val fallback = TrackingFallbackCodec(result = "fallback")
        val codec = RedissonProtobufCodec(fallback)
        val bytes = AnyMessage.newBuilder()
            .setTypeUrl("type.googleapis.com/com.google.protobuf.Timestamp")
            .setValue(ByteString.copyFrom(byteArrayOf(0x0A, 0x05, 0x01)))
            .build()
            .toByteArray()
        val input = TrackingNioViewByteBuf(Unpooled.wrappedBuffer(bytes))
        try {
            input.withPreservedDecoderState {
                codec.valueDecoder.decode(input, State()) shouldBeEqualTo "fallback"
            }
            input.boundedNioCalls shouldBeEqualTo 1
            fallback.seen!!.refCnt() shouldBeEqualTo 0
        } finally {
            input.release()
        }
    }

    @Test
    fun `allowlisted non message class never reaches fallback`() {
        val fallback = TrackingFallbackCodec(result = "must-not-run")
        val codec = RedissonProtobufCodec(fallback, setOf("java.lang."))
        val bytes = AnyMessage.newBuilder()
            .setTypeUrl("type.googleapis.com/java.lang.String")
            .build()
            .toByteArray()
        val input = TrackingNioViewByteBuf(Unpooled.wrappedBuffer(bytes))
        try {
            input.withPreservedDecoderState {
                assertFailsWith<SecurityException> {
                    codec.valueDecoder.decode(input, State())
                }
            }
            input.boundedNioCalls shouldBeEqualTo 1
            fallback.seen shouldBeEqualTo null
        } finally {
            input.release()
        }
    }

    @Test
    fun `error from strict decode never reaches fallback`() {
        val fallback = TrackingFallbackCodec(result = "must-not-run")
        val codec = RedissonProtobufCodec(fallback)
        val origin = newSimpleMessage()
        val encoded = codec.valueEncoder.encode(origin)
        val failure = AssertionError("terminal")
        val input = ThrowingNioViewByteBuf(encoded, failure)
        try {
            input.withPreservedDecoderState {
                val actual = assertFailsWith<AssertionError> {
                    codec.valueDecoder.decode(input, State())
                }
                (actual === failure) shouldBeEqualTo true
            }
            fallback.seen shouldBeEqualTo null
        } finally {
            input.release()
        }
    }

    @Test
    fun `hostile trusted fallback cannot escape or leak its isolated input`() {
        HostileBehavior.entries.forEach { behavior ->
            val fallback = HostileFallbackCodec(behavior)
            val codec = RedissonProtobufCodec(fallback)
            val callerInput = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))
            val reader = callerInput.readerIndex()
            val writer = callerInput.writerIndex()
            val refs = callerInput.refCnt()
            try {
                val failure = assertFailsWith<Throwable> {
                    codec.valueDecoder.decode(callerInput, State())
                }
                if (behavior == HostileBehavior.THROW_AFTER_RETAIN) {
                    (failure === fallback.sentinel) shouldBeEqualTo true
                } else {
                    (failure is SecurityException) shouldBeEqualTo true
                }
                fallback.calls shouldBeEqualTo 1
                fallback.seen!!.refCnt() shouldBeEqualTo 0
                callerInput.readerIndex() shouldBeEqualTo reader
                callerInput.writerIndex() shouldBeEqualTo writer
                callerInput.refCnt() shouldBeEqualTo refs
            } finally {
                callerInput.release()
            }
        }
    }

    @Test
    fun `cleanup failure is suppressed without replacing the operation failure`() {
        val operationFailure = IllegalStateException("operation")
        val cleanupFailure = IllegalStateException("cleanup")
        val owned = ThrowOnFinalReleaseByteBuf(
            Unpooled.buffer(1).writeByte(1),
            cleanupFailure,
        )
        try {
            releaseOwnedBuffer(owned, operationFailure)

            operationFailure.suppressed.size shouldBeEqualTo 1
            (operationFailure.suppressed.single() === cleanupFailure) shouldBeEqualTo true
            owned.refCnt() shouldBeEqualTo 1
        } finally {
            owned.forceReleaseDelegate()
        }
    }

    @Test
    fun `default codec rejects untrusted protobuf typeUrl before class loading`() {
        val bytes = AnyMessage.newBuilder()
            .setTypeUrl("type.googleapis.com/untrusted.payload.UntrustedPayload")
            .build()
            .toByteArray()

        val fallback = TrackingFallbackCodec(result = "must-not-run")
        val codec = RedissonProtobufCodec(fallback)
        val input = TrackingNioViewByteBuf(Unpooled.wrappedBuffer(bytes))
        try {
            input.withPreservedDecoderState {
                assertFailsWith<SecurityException> {
                    codec.valueDecoder.decode(input, State())
                }
            }
            input.boundedNioCalls shouldBeEqualTo 1
            fallback.seen shouldBeEqualTo null
        } finally {
            input.release()
        }
    }

    @Test
    fun `unsafe opt-in bypasses allowlist and keeps fallback behavior`() {
        val fallbackValue = "fallback-after-legacy-class-lookup"
        val bytes = AnyMessage.newBuilder()
            .setTypeUrl("type.googleapis.com/untrusted.payload.MissingProtoMessage")
            .build()
            .toByteArray()

        val codec = RedissonProtobufCodec.trustedInternal(
            fallbackCodec = SentinelFallbackCodec(fallbackValue),
            allowedClassPrefixes = RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE,
        )

        val input = Unpooled.wrappedBuffer(bytes)
        try {
            input.withPreservedDecoderState {
                codec.valueDecoder.decode(input, State()) shouldBeEqualTo fallbackValue
            }
        } finally {
            input.release()
        }
    }

    @Test
    fun `custom allowlist constructor supports named allowedClassPrefixes`() {
        val codec = RedissonProtobufCodec(
            allowedClassPrefixes = setOf("io.bluetape4k.protobuf.redis.messages")
        )
        val origin = newSimpleMessage()

        codec.verifyCodec(origin)
    }

    @Test
    fun `allowlist rejects prefix spoofing`() {
        val bytes = AnyMessage.newBuilder()
            .setTypeUrl("type.googleapis.com/io.bluetape4kevil.Payload")
            .build()
            .toByteArray()

        val codec = RedissonProtobufCodec(
            allowedClassPrefixes = setOf("io.bluetape4k")
        )

        val input = Unpooled.wrappedBuffer(bytes)
        try {
            input.withPreservedDecoderState {
                assertFailsWith<SecurityException> {
                    codec.valueDecoder.decode(input, State())
                }
            }
        } finally {
            input.release()
        }
    }

    @Test
    fun `allowlist rejects blank prefixes`() {
        assertFailsWith<IllegalArgumentException> {
            RedissonProtobufCodec(allowedClassPrefixes = setOf(""))
        }
    }
}
