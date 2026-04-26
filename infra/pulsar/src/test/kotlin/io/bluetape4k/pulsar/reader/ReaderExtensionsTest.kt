package io.bluetape4k.pulsar.reader

import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.producer.sendSuspend
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Schema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReaderExtensionsTest: AbstractPulsarTest() {

    companion object: KLogging()

    @Test
    fun `readNextSuspend - 단일 메시지 읽기`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        val reader = client.newReader(Schema.STRING)
            .topic(topic)
            .startMessageId(MessageId.earliest)
            .create()
        try {
            producer.sendSuspend("reader test")
            val msg = reader.readNextSuspend()
            msg.value shouldBeEqualTo "reader test"
        } finally {
            reader.close()
            producer.close()
            client.close()
        }
    }

    @Test
    fun `readAsFlow - hasMessageAvailable 기반 Flow 읽기`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val messageCount = 5

        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        try {
            // 메시지를 미리 모두 발행한 후 Reader로 읽기
            repeat(messageCount) { i -> producer.sendSuspend("read-msg-$i") }
        } finally {
            producer.close()
        }

        // Reader는 earliest 시점부터 읽으므로 미리 발행된 메시지 전부 수신
        val reader = client.newReader(Schema.STRING)
            .topic(topic)
            .startMessageId(MessageId.earliest)
            .create()
        try {
            val messages = reader.readAsFlow().toList()
            messages shouldHaveSize messageCount
            messages.forEachIndexed { i, msg ->
                msg.value shouldBeEqualTo "read-msg-$i"
            }
        } finally {
            reader.close()
            client.close()
        }
    }

    @Test
    fun `readAsFlow - 메시지 없으면 빈 Flow`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        val reader = client.newReader(Schema.STRING)
            .topic(topic)
            .startMessageId(MessageId.latest)
            .create()
        try {
            val messages = reader.readAsFlow().toList()
            messages shouldHaveSize 0
        } finally {
            reader.close()
            client.close()
        }
    }
}
