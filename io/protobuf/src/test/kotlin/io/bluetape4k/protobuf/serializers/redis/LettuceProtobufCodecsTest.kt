package io.bluetape4k.protobuf.serializers.redis

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.protobuf.redis.messages.RedisSimpleMessage
import io.bluetape4k.protobuf.redis.messages.redisSimpleMessage
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.lettuce.core.codec.RedisCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.random.Random

class LettuceProtobufCodecsTest: AbstractLettuceTest() {

    companion object: KLogging()

    private fun getRedisCodecs(): List<LettuceBinaryCodec<out Any>> = listOf(
        LettuceProtobufCodecs.protobuf(),
        LettuceProtobufCodecs.deflateProtobuf(),
        LettuceProtobufCodecs.gzipProtobuf(),
        LettuceProtobufCodecs.lz4Protobuf(),
        LettuceProtobufCodecs.snappyProtobuf(),
        LettuceProtobufCodecs.zstdProtobuf(),
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
    fun `codec for protobuf message`(codec: RedisCodec<String, Any>) {
        client.connect(codec).use { connection ->
            val commands = connection.sync()

            val key = randomName()
            val origin = getRedisSimpleMessage()

            commands.set(key, origin)
            commands.get(key) shouldBeEqualTo origin

            commands.del(key)
        }
    }

    @ParameterizedTest(name = "codec={0}")
    @MethodSource("getRedisCodecs")
    fun `codec for collection of protobuf message`(codec: RedisCodec<String, Any>) {
        client.connect(codec).use { connection ->
            val commands = connection.sync()

            val key = randomName()
            val origin: List<RedisSimpleMessage> = List(10) { getRedisSimpleMessage() }

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

    @ParameterizedTest(name = "codec={0}")
    @MethodSource("getRedisCodecs")
    fun `codec for hset with protobuf message`(codec: RedisCodec<String, Any>) {
        client.connect(codec).use { connection ->
            val commands = connection.sync()

            val key = randomName()
            val origin: List<RedisSimpleMessage> = List(10) { getRedisSimpleMessage() }
            val originMap: Map<String, RedisSimpleMessage> = origin.associateBy { it.id.toString() }

            commands.hset(key, originMap)
            commands.hgetall(key) shouldContainSame originMap
            commands.del(key)
        }
    }

    data class CustomData(
        val id: Int,
        val name: String,
    ): java.io.Serializable

    private fun getRedisSimpleMessage() = redisSimpleMessage {
        id = Random.nextLong()
        name = Fakers.randomString(1024, 4096)
        description = Fakers.randomString(1024, 4096)
        timestamp = Instant.now().toTimestamp()
    }

    private fun Instant.toTimestamp(): Timestamp = timestamp {
        this.seconds = this@toTimestamp.epochSecond
        this.nanos = this@toTimestamp.nano
    }
}
