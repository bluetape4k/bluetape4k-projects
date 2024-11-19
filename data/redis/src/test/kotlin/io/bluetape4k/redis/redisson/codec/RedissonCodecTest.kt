package io.bluetape4k.redis.redisson.codec

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.messages.DayOfTheWeek
import io.bluetape4k.redis.messages.NestedMessage
import io.bluetape4k.redis.messages.SimpleMessage
import io.bluetape4k.redis.messages.copy
import io.bluetape4k.redis.messages.nestedMessage
import io.bluetape4k.redis.messages.simpleMessage
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.bluetape4k.redis.redisson.RedissonCodecs
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import java.time.Instant
import kotlin.random.Random

class RedissonCodecTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
        private const val METHOD_SOURCE = "getRedissonBinaryCodecs"
    }

    private fun getRedissonBinaryCodecs() = listOf(
        RedissonCodecs.Default,

        RedissonCodecs.Kryo5,
        RedissonCodecs.Fury,
        RedissonCodecs.Jdk,

        RedissonCodecs.Kryo5Composite,
        RedissonCodecs.FuryComposite,
        RedissonCodecs.JdkComposite,

        RedissonCodecs.SnappyKryo5,
        RedissonCodecs.SnappyFury,
        RedissonCodecs.SnappyJdk,

        RedissonCodecs.SnappyKryo5Composite,
        RedissonCodecs.SnappyFuryComposite,
        RedissonCodecs.SnappyJdkComposite,

        RedissonCodecs.LZ4Kryo5,
        RedissonCodecs.LZ4Fury,
        RedissonCodecs.LZ4Jdk,

        RedissonCodecs.LZ4Kryo5Composite,
        RedissonCodecs.LZ4FuryComposite,
        RedissonCodecs.LZ4JdkComposite,

        RedissonCodecs.ZstdKryo5,
        RedissonCodecs.ZstdFury,
        RedissonCodecs.ZstdJdk,

        RedissonCodecs.ZstdKryo5Composite,
        RedissonCodecs.ZstdFuryComposite,
        RedissonCodecs.ZstdJdkComposite,

        RedissonCodecs.GzipKryo5,
        RedissonCodecs.GzipFury,
        RedissonCodecs.GzipJdk,

        RedissonCodecs.GzipKryo5Composite,
        RedissonCodecs.GzipFuryComposite,
        RedissonCodecs.GzipJdkComposite,
    )

    @ParameterizedTest(name = "codec={0}")
    @MethodSource(METHOD_SOURCE)
    fun `codec for kotlin data class`(codec: Codec) {
        val data = CustomData(
            id = Random.nextInt(),
            name = Fakers.randomString(1024, 4096),
        )
        codec.verifyCodec(data)
    }

    @ParameterizedTest(name = "codec for simple string with {0}")
    @MethodSource(METHOD_SOURCE)
    fun `codec for simple string with fallback codec`(codec: Codec) {
        val origin = "Hello world! 동해물과 백두산이"
        codec.verifyCodec(origin)
    }

    @ParameterizedTest(name = "codec for kotlin data class with {0}")
    @MethodSource(METHOD_SOURCE)
    fun `codec for kotlin data class with fallback codec`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            codec.verifyCodec(newCustomData())
        }
    }

    @ParameterizedTest(name = "codec for protobuf simple message with {0}")
    @MethodSource(METHOD_SOURCE)
    fun `codec for protobuf simple message`(codec: Codec) {
        repeat(REPEAT_SIZE) {
            codec.verifyCodec(newSimpleMessage())
        }
    }

    @ParameterizedTest(name = "codec for protobuf nested message with {0}")
    @MethodSource(METHOD_SOURCE)
    fun `codec for protobuf nested message`(codec: Codec) {
        // FIXME: Kryo5 Codec 은 실패한다.
        // Fury Codec 은 nested message 를 직렬화를 지원한다 (Kryo5 는 지원하지 않음)
        Assumptions.assumeTrue(codec.javaClass.name.contains("Fury"))
        repeat(REPEAT_SIZE) {
            codec.verifyCodec(newNestedMessage())
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Codec.verifyCodec(origin: T) {
        val buf = valueEncoder.encode(origin)
        val actual = valueDecoder.decode(buf, State()) as? T
        actual shouldBeEqualTo origin
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    data class CustomData(
        val id: Int,
        val name: String,
    ): java.io.Serializable

    private fun newCustomData(): CustomData = CustomData(
        faker.random().nextInt(),
        faker.name().fullName()
    )

    private fun newSimpleMessage(): SimpleMessage = simpleMessage {
        id = Random.nextLong()
        name = Fakers.randomString(1024, 4096)
        description = Fakers.randomString(1024, 4096)
        timestamp = Instant.now().toTimestamp()
    }

    private fun newNestedMessage(): NestedMessage = nestedMessage {
        id = faker.random().nextLong()
        name = faker.name().fullName()
        dayOfTheWeek = DayOfTheWeek.FRIDAY
        optionalMessage = newSimpleMessage()
        nestedMessages.add(newSimpleMessage().copy { id = faker.random().nextLong() })
    }

    private fun Instant.toTimestamp(): Timestamp = timestamp {
        this.seconds = this@toTimestamp.epochSecond
        this.nanos = this@toTimestamp.nano
    }
}
