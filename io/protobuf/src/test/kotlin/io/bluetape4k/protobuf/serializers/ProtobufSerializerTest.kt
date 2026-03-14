package io.bluetape4k.protobuf.serializers

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.messages.NestedMessage
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.nestedMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class ProtobufSerializerTest {
    companion object : KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val serializer = ProtobufSerializer()

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
    fun `non-protobuf 타입은 fallback serializer로 직렬화 및 역직렬화된다`() {
        data class SimpleData(
            val id: Int,
            val name: String,
        ) : java.io.Serializable

        val origin = SimpleData(1, "hello")
        val bytes = serializer.serialize(origin)
        bytes.shouldNotBeNull()
        (bytes.isNotEmpty()).shouldBeTrue()

        val actual = serializer.deserialize<SimpleData>(bytes)
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
