package io.bluetape4k.protobuf.serializers

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.messages.NestedMessage
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.nestedMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

class ProtobufSerializerTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private val serializer = ProtobufSerializer()

    @RepeatedTest(REPEAT_SIZE)
    fun `serialize proto message`() {
        val message = testMessage {
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
        val message = testMessage {
            id = Fakers.random.nextLong()
            name = Fakers.randomString(1024, 2048, true)
        }
        val nestedMessage = nestedMessage {
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
}
