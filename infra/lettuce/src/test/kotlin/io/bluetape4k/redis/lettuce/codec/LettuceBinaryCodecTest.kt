package io.bluetape4k.redis.lettuce.codec

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.RedisCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

class LettuceBinaryCodecTest: AbstractLettuceTest() {

    companion object: KLogging()

    private fun getRedisCodecs(): List<LettuceBinaryCodec<out Any>> = listOf(
        LettuceBinaryCodecs.jdk(),
        LettuceBinaryCodecs.fory(),
        LettuceBinaryCodecs.kryo(),

        LettuceBinaryCodecs.gzipJdk(),
        LettuceBinaryCodecs.gzipFory(),
        LettuceBinaryCodecs.gzipKryo(),

        LettuceBinaryCodecs.deflateJdk(),
        LettuceBinaryCodecs.deflateFory(),
        LettuceBinaryCodecs.deflateKryo(),

        LettuceBinaryCodecs.lz4Jdk(),
        LettuceBinaryCodecs.lz4Fory(),
        LettuceBinaryCodecs.lz4Kryo(),

        LettuceBinaryCodecs.snappyJdk(),
        LettuceBinaryCodecs.snappyFory(),
        LettuceBinaryCodecs.snappyKryo(),

        LettuceBinaryCodecs.zstdJdk(),
        LettuceBinaryCodecs.zstdFory(),
        LettuceBinaryCodecs.zstdKryo(),
    )

    @ParameterizedTest(name = "codec={0}")
    @MethodSource("getRedisCodecs")
    fun `codec for kotlin data class`(codec: RedisCodec<String, Any>) {
        client.connect(codec).use { connection ->
            // client.connect(codec).use { connection ->
            val commands = connection.sync()

            val key = randomName()
            val origin = CustomData(Random.nextInt(), Fakers.randomString(1024, 4096))

            commands.set(key, origin)
            commands.get(key) shouldBeEqualTo origin

            commands.del(key)
        }
    }

    @ParameterizedTest(name = "codec={0}")
    @MethodSource("getRedisCodecs")
    fun `codec for collection of kotlin data class`(codec: RedisCodec<String, Any>) {
        client.connect(codec).use { connection ->
            // client.connect(codec).use { connection ->
            val commands = connection.sync()

            val key = randomName()
            val origin = List(10) {
                CustomData(Random.nextInt(), Fakers.randomString(1024, 4096))
            }

            commands.set(key, origin)
            commands.get(key) shouldBeEqualTo origin

            commands.del(key)
        }
    }

    @ParameterizedTest(name = "codec={0}")
    @MethodSource("getRedisCodecs")
    fun `codec for hset with data class`(codec: RedisCodec<String, Any>) {
        client.connect(codec).use { connection ->
            val commands = connection.sync()

            val key = randomName()
            val origin: List<CustomData> = List(10) { CustomData(it, "Name-$it") }
            val originMap: Map<String, CustomData> = origin.associateBy { it.id.toString() }

            commands.hset(key, originMap)
            commands.hgetall(key) shouldContainSame originMap
            commands.del(key)
        }
    }

    data class CustomData(
        val id: Int,
        val name: String,
    ): java.io.Serializable

}
