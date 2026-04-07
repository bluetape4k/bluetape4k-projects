package io.bluetape4k.protobuf

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.protobuf.messages.NestedMessage
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.nestedMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class MessageSupportTest {
    companion object: KLogging() {
        private const val REPEAT_SIZE = 10
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `pack simple message`() {
        val message =
            testMessage {
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
        val message =
            testMessage {
                id = Fakers.random.nextLong()
                name = "test message"
            }
        val nestedMessage =
            nestedMessage {
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

    @Test
    fun `packMessage - 직렬화된 바이트 배열은 비어있지 않다`() {
        val message =
            testMessage {
                id = 1L
                name = "test"
            }
        val bytes = packMessage(message)
        bytes.shouldNotBeNull().shouldNotBeEmpty()
    }

    @Test
    fun `unpackMessage - 잘못된 타입으로 언패킹 시 null을 반환한다`() {
        val message =
            testMessage {
                id = 1L
                name = "wrong type test"
            }
        val bytes = packMessage(message)

        // TestMessage를 NestedMessage로 언패킹하면 null 반환
        val result = unpackMessage<NestedMessage>(bytes)
        result.shouldBeNull()
    }
}
