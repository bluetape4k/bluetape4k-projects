package io.bluetape4k.redis.redisson.codec

import com.google.protobuf.timestamp
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.messages.DayOfTheWeek
import io.bluetape4k.redis.messages.copy
import io.bluetape4k.redis.messages.nestedMessage
import io.bluetape4k.redis.messages.simpleMessage
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.bluetape4k.redis.redisson.RedissonCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import java.io.Serializable
import java.time.Instant

class ProtobufCodecTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    private fun getTestCodecs() = listOf(
        Arguments.of(RedissonCodecs.Protobuf),
        Arguments.of(RedissonCodecs.GzipProtobuf),
        Arguments.of(RedissonCodecs.GzipProtobufComposite),
        Arguments.of(RedissonCodecs.LZ4Protobuf),
        Arguments.of(RedissonCodecs.LZ4ProtobufComposite),
        Arguments.of(RedissonCodecs.SnappyProtobuf),
        Arguments.of(RedissonCodecs.SnappyProtobufComposite),
        Arguments.of(RedissonCodecs.ZstdProtobuf),
        Arguments.of(RedissonCodecs.ZstdProtobufComposite),
    )

    data class CustomData(
        val id: Int,
        val name: String,
    ): Serializable

    private fun Instant.toProtobufTimestamp(): com.google.protobuf.Timestamp {
        val source = this
        return timestamp {
            this.seconds = source.epochSecond
            this.nanos = source.nano
        }
    }

    private fun newSimpleMessage() = simpleMessage {
        id = faker.random().nextLong()
        name = faker.name().fullName()
        description = Fakers.randomString(1024, 4096, true)
        timestamp = Instant.now().toProtobufTimestamp()
    }

    private fun newNestedMessage() = nestedMessage {
        id = faker.random().nextLong()
        name = faker.name().fullName()
        dayOfTheWeek = DayOfTheWeek.FRIDAY
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
}
