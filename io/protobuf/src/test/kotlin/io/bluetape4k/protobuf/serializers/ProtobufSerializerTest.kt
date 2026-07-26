package io.bluetape4k.protobuf.serializers

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.messages.NestedMessage
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.nestedMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class ProtobufSerializerTest {
    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val serializer = ProtobufSerializer()

    data class SimpleData(
        val id: Int,
        val name: String,
    ): java.io.Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize proto message`() {
        val message =
            testMessage {
                id = Fakers.random.nextLong()
                name = Fakers.randomString(1024, 2048, true)
            }

        val bytes = serializer.serialize(message)
        log.debug { "bytes size=${bytes.size}" }

        val actual = serializer.deserialize<TestMessage>(bytes)!!
        actual shouldBeEqualTo message
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize proto nested message`() {
        val message =
            testMessage {
                id = Fakers.random.nextLong()
                name = Fakers.randomString(1024, 2048, true)
            }
        val nestedMessage =
            nestedMessage {
                id = Fakers.random.nextLong()
                name = Fakers.randomString(1024, 2048, true)
                nested = message
            }

        val bytes = serializer.serialize(nestedMessage)
        log.debug { "bytes size=${bytes.size}" }

        val actual = serializer.deserialize<NestedMessage>(bytes)!!
        actual shouldBeEqualTo nestedMessage
        actual.nested shouldBeEqualTo message
    }

    @Test
    fun `빈 바이트 배열 역직렬화 시 null을 반환한다`() {
        val result = serializer.deserialize<TestMessage>(ByteArray(0))
        result.shouldBeNull()
    }

    @Test
    fun `strict serializer rejects non-protobuf values by default`() {
        val origin = SimpleData(1, "hello")

        assertFailsWith<BinarySerializationException> {
            serializer.serialize(origin)
        }
    }

    @Test
    fun `strict serializer rejects non-protobuf bytes by default`() {
        val origin = SimpleData(1, "hello")
        val bytes = BinarySerializers.Kryo.serialize(origin)

        assertFailsWith<BinarySerializationException> {
            serializer.deserialize<SimpleData>(bytes)
        }
    }

    @Test
    fun `trusted internal serializer keeps fallback compatibility`() {
        val origin = SimpleData(1, "hello")
        val trustedInternalSerializer = ProtobufSerializer.trustedInternalProtobuf()

        val bytes = trustedInternalSerializer.serialize(origin)
        bytes.shouldNotBeNull()
        (bytes.isNotEmpty()).shouldBeTrue()

        val actual = trustedInternalSerializer.deserialize<SimpleData>(bytes)
        actual shouldBeEqualTo origin
    }

    @Test
    fun `serialize - 직렬화된 바이트 배열은 비어있지 않다`() {
        val message =
            testMessage {
                id = 42L
                name = "test"
            }
        val bytes = serializer.serialize(message)
        bytes.shouldNotBeNull()
        (bytes.isNotEmpty()).shouldBeTrue()
    }
}
