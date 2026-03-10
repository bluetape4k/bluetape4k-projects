package io.bluetape4k.redis.redisson.codec

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.redisson.client.codec.Codec
import org.redisson.client.handler.State
import kotlin.random.Random

class RedissonCodecsTest: AbstractRedissonTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
        private const val METHOD_SOURCE = "getRedissonBinaryCodecs"
    }

    private fun getRedissonBinaryCodecs() = listOf(
        RedissonCodecs.Default,

        RedissonCodecs.Jdk,
        RedissonCodecs.Fory,
        RedissonCodecs.Kryo5,

        RedissonCodecs.JdkComposite,
        RedissonCodecs.ForyComposite,
        RedissonCodecs.Kryo5Composite,

        RedissonCodecs.SnappyJdk,
        RedissonCodecs.SnappyFory,
        RedissonCodecs.SnappyKryo5,

        RedissonCodecs.SnappyJdkComposite,
        RedissonCodecs.SnappyForyComposite,
        RedissonCodecs.SnappyKryo5Composite,

        RedissonCodecs.LZ4Jdk,
        RedissonCodecs.LZ4Fory,
        RedissonCodecs.LZ4Kryo5,

        RedissonCodecs.LZ4JdkComposite,
        RedissonCodecs.LZ4ForyComposite,
        RedissonCodecs.LZ4Kryo5Composite,

        RedissonCodecs.ZstdJdk,
        RedissonCodecs.ZstdFory,
        RedissonCodecs.ZstdKryo5,

        RedissonCodecs.ZstdJdkComposite,
        RedissonCodecs.ZstdForyComposite,
        RedissonCodecs.ZstdKryo5Composite,

        RedissonCodecs.GzipJdk,
        RedissonCodecs.GzipFory,
        RedissonCodecs.GzipKryo5,

        RedissonCodecs.GzipJdkComposite,
        RedissonCodecs.GzipForyComposite,
        RedissonCodecs.GzipKryo5Composite,
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
}
