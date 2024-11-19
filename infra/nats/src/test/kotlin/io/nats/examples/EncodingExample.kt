package io.nats.examples

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.api.streamConfiguration
import io.bluetape4k.nats.client.tryPurgeStream
import io.nats.client.JetStream
import io.nats.client.api.StorageType
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.time.Duration

class EncodingExample: AbstractNatsTest() {

    companion object: KLogging()

    @Nested
    inner class JsonEncoding {
        private val mapper = Jackson.defaultJsonMapper

        @RepeatedTest(3)
        fun `json encoding pojo`() {
            val pojo = createPojo()
            val serialized = mapper.writeValueAsBytes(pojo)
            val deserialized = mapper.readValue<Pojo>(serialized)
            deserialized shouldBeEqualTo pojo

            log.debug { "json serialized size=${serialized.size}" }
        }

        @Test
        fun `json encoding to nats message`() {
            getConnection().use { nc ->
                val jsm = nc.jetStreamManagement()

                // purge the stream. 만약 stream 이 없다면, 예외가 발생하는데, 이 때 stream 을 생성합니다.
                jsm.tryPurgeStream("stream") {
                    streamConfiguration {
                        name("stream")
                        subjects("json", "kryo")
                        storageType(StorageType.Memory)
                    }
                }

                val js = nc.jetStream()

                val originalPojo = createPojo()
                js.publish("json", mapper.writeValueAsBytes(originalPojo))

                val sub = js.subscribe("json")
                val m = sub.nextMessage(Duration.ofSeconds(1))

                val subPojo = mapper.readValue<Pojo>(m.data)
                sub.unsubscribe()

                subPojo shouldBeEqualTo originalPojo
            }
        }
    }

    @Nested
    inner class BinaryEncoding {
        private fun serializers() = listOf(
            BinarySerializers.LZ4Jdk,
            BinarySerializers.LZ4Kryo,
            BinarySerializers.LZ4Fury,
            BinarySerializers.SnappyJdk,
            BinarySerializers.SnappyKryo,
            BinarySerializers.SnappyFury,
            BinarySerializers.ZstdJdk,
            BinarySerializers.ZstdKryo,
            BinarySerializers.ZstdFury
        )


        @ParameterizedTest
        @MethodSource("serializers")
        fun `binary encoding pojo`(serializer: BinarySerializer) {
            val pojo = createPojo()
            val serialized = serializer.serialize(pojo)
            val deserialized = serializer.deserialize<Pojo>(serialized)
            deserialized shouldBeEqualTo pojo

            log.debug { "binary serialized size=${serialized.size}" }
        }


        @ParameterizedTest
        @MethodSource("serializers")
        fun `binary encoding to nats message`(serializer: BinarySerializer) {
            getConnection().use { nc ->
                val jsm = nc.jetStreamManagement()

                // purge the stream. 만약 stream 이 없다면, 예외가 발생하는데, 이 때 stream 을 생성합니다.
                jsm.tryPurgeStream("stream") {
                    streamConfiguration {
                        name("stream")
                        subjects("json", "binary")
                        storageType(StorageType.Memory)
                    }
                }

                val js: JetStream = nc.jetStream()

                val originalPojo = createPojo()
                js.publish("binary", serializer.serialize(originalPojo))

                val sub = js.subscribe("binary")
                val m = sub.nextMessage(Duration.ofSeconds(1))

                val subPojo = serializer.deserialize<Pojo>(m.data)
                sub.unsubscribe()

                subPojo shouldBeEqualTo originalPojo
            }
        }
    }

    data class Pojo(
        val s: String,
        val l: Long,
        val b: Boolean,
        val strings: List<String>,
    ): Serializable {
        var ints: IntArray? = null
    }

    private fun createPojo(): Pojo {
        return Pojo(
            s = Fakers.randomString(2048),
            l = faker.random().nextLong(),
            b = faker.random().nextBoolean(),
            strings = listOf(faker.artist().name(), faker.artist().name(), faker.artist().name())
        ).apply {
            ints = intArrayOf(faker.random().nextInt(), faker.random().nextInt(), faker.random().nextInt())
        }
    }
}
