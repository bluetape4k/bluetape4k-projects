package io.bluetape4k.protobuf

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.messages.NestedMessage
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.nestedMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

class MessageSupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `pack simple message`() {
        val message = testMessage {
            id = Fakers.random.nextLong()
            name = Fakers.randomString(1024, 2048, true)
        }

        val bytes = packMessage(message)
        log.debug { "bytes size=${bytes.size}" }

        val actual = unpackMessage<TestMessage>(bytes)!!
        actual shouldBeEqualTo message
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `pack nested message`() {
        val message = testMessage {
            id = Fakers.random.nextLong()
            name = "test message"
        }
        val nestedMessage = nestedMessage {
            id = Fakers.random.nextLong()
            name = Fakers.randomString(1024, 2048)
            nested = message
        }

        val bytes = packMessage(nestedMessage)
        log.debug { "bytes size=${bytes.size}" }

        val actual = unpackMessage<NestedMessage>(bytes)!!
        actual shouldBeEqualTo nestedMessage
        actual.nested shouldBeEqualTo message
    }
}
