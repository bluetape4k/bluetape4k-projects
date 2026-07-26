package io.bluetape4k.spring.redis.serializer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class RedisConsumerRuntimeClasspathTest {

    @Test
    fun `documented LZ4 Fory serializer works from consumer runtime classpath`() {
        val original = "Hello, bluetape4k Redis runtime!"

        val bytes = RedisBinarySerializers.LZ4Fory.serialize(original)
        bytes.shouldNotBeNull()

        RedisBinarySerializers.LZ4Fory.deserialize(bytes) shouldBeEqualTo original
    }

    @Test
    fun `documented FastFory serializer works from consumer runtime classpath`() {
        val original = mapOf("mode" to "fast")
        val bytes = RedisBinarySerializers.FastFory.serialize(original)

        RedisBinarySerializers.FastFory.deserialize(bytes) shouldBeEqualTo original
    }

    @Test
    fun `documented LZ4 Kryo serializer works from consumer runtime classpath`() {
        val original = "Hello, bluetape4k Redis Kryo runtime!"

        val bytes = RedisBinarySerializers.LZ4Kryo.serialize(original)
        bytes.shouldNotBeNull()

        RedisBinarySerializers.LZ4Kryo.deserialize(bytes) shouldBeEqualTo original
    }
}
