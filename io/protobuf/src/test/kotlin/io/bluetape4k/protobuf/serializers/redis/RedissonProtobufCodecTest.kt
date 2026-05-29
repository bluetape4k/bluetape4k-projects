package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.timestamp
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.protobuf.redis.messages.copy
import io.bluetape4k.protobuf.redis.messages.redisNestedMessage
import io.bluetape4k.protobuf.redis.messages.redisSimpleMessage
import io.bluetape4k.assertions.shouldBeEqualTo
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
import java.time.Instant

class RedissonProtobufCodecTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    private fun getTestCodecs() = listOf(
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
        val actual = valueDecoder.decode(buf, State()) as? T
        actual shouldBeEqualTo origin
    }

    @ParameterizedTest(name = "codec for simple string with {0}")
    @MethodSource("getTestCodecs")
    fun `codec for simple string with fallback codec`(codec: Codec) {
        val origin = "Hello world! 동해물과 백두산이"
        codec.verifyCodec(origin)
    }

    @ParameterizedTest(name = "codec for kotlin data class with {0}")
    @MethodSource("getTestCodecs")
    fun `codec for kotlin data class with fallback codec`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            val origin = CustomData(faker.random().nextInt(), faker.name().fullName())
            codec.verifyCodec(origin)
        }
    }

    @ParameterizedTest(name = "codec for protobuf simple message with {0}")
    @MethodSource("getTestCodecs")
    fun `codec for protobuf simple message`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            codec.verifyCodec(newSimpleMessage())
        }
    }

    @ParameterizedTest(name = "codec for protobuf nested message with {0}")
    @MethodSource("getTestCodecs")
    fun `codec for protobuf nested message`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            codec.verifyCodec(newNestedMessage())
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
    fun `default codec rejects untrusted protobuf typeUrl before class loading`() {
        val bytes = AnyMessage.newBuilder()
            .setTypeUrl("type.googleapis.com/untrusted.payload.UntrustedPayload")
            .build()
            .toByteArray()

        val codec = RedissonProtobufCodec()

        assertFailsWith<SecurityException> {
            codec.valueDecoder.decode(Unpooled.wrappedBuffer(bytes), State())
        }
    }

    @Test
    fun `unsafe opt-in bypasses allowlist and keeps fallback behavior`() {
        val fallbackValue = "fallback-after-legacy-class-lookup"
        val bytes = AnyMessage.newBuilder()
            .setTypeUrl("type.googleapis.com/untrusted.payload.MissingProtoMessage")
            .build()
            .toByteArray()

        val codec = RedissonProtobufCodec(
            fallbackCodec = SentinelFallbackCodec(fallbackValue),
            allowedClassPrefixes = RedissonProtobufCodec.ALLOW_ALL_CLASSES_UNSAFE,
        )

        val actual = codec.valueDecoder.decode(Unpooled.wrappedBuffer(bytes), State())

        actual shouldBeEqualTo fallbackValue
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

        assertFailsWith<SecurityException> {
            codec.valueDecoder.decode(Unpooled.wrappedBuffer(bytes), State())
        }
    }

    @Test
    fun `allowlist rejects blank prefixes`() {
        assertFailsWith<IllegalArgumentException> {
            RedissonProtobufCodec(allowedClassPrefixes = setOf(""))
        }
    }
}
