package io.bluetape4k.redis.lettuce.codec

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.messages.SimpleMessage
import io.bluetape4k.redis.messages.simpleMessage
import io.lettuce.core.codec.RedisCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.random.Random

class LettuceBinaryCodecTest: AbstractLettuceTest() {

    companion object: KLogging()

    private fun getRedisCodecs(): List<LettuceBinaryCodec<out Any>> = listOf(
        LettuceBinaryCodecs.jdk(),
        LettuceBinaryCodecs.kryo(),
        LettuceBinaryCodecs.protobuf(),
        LettuceBinaryCodecs.fury(),

        LettuceBinaryCodecs.gzipJdk(),
        LettuceBinaryCodecs.gzipKryo(),
        LettuceBinaryCodecs.gzipProtobuf(),
        LettuceBinaryCodecs.gzipFury(),

        LettuceBinaryCodecs.deflateJdk(),
        LettuceBinaryCodecs.deflateKryo(),
        LettuceBinaryCodecs.deflateProtobuf(),
        LettuceBinaryCodecs.deflateFury(),

        LettuceBinaryCodecs.lz4Jdk(),
        LettuceBinaryCodecs.lz4Kryo(),
        LettuceBinaryCodecs.lz4Protobuf(),
        LettuceBinaryCodecs.lz4Fury(),

        LettuceBinaryCodecs.snappyJdk(),
        LettuceBinaryCodecs.snappyKryo(),
        LettuceBinaryCodecs.snappyProtobuf(),
        LettuceBinaryCodecs.snappyFury(),

        LettuceBinaryCodecs.zstdJdk(),
        LettuceBinaryCodecs.zstdKryo(),
        LettuceBinaryCodecs.zstdProtobuf(),
        LettuceBinaryCodecs.zstdFury(),
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
            val origin = getSimpleMessage()

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
            val origin: List<SimpleMessage> = List(10) { getSimpleMessage() }

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
            val origin: List<SimpleMessage> = List(10) { getSimpleMessage() }
            val originMap: Map<String, SimpleMessage> = origin.associateBy { it.id.toString() }

            commands.hset(key, originMap)
            commands.hgetall(key) shouldContainSame originMap
            commands.del(key)
        }
    }

    data class CustomData(
        val id: Int,
        val name: String,
    ): java.io.Serializable

    private fun getSimpleMessage(): SimpleMessage = simpleMessage {
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
