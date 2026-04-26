package io.bluetape4k.pulsar.reader

import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.producer.sendSuspend
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.Schema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReaderSupportTest : AbstractPulsarTest() {

    companion object : KLogging()

    @Test
    fun `reader DSL - Schema와 setup으로 Reader 생성`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        try {
            val reader = client.reader(Schema.STRING) {
                topic(topic)
                startMessageId(MessageId.earliest)
            }
            reader.shouldNotBeNull()
            reader.close()
        } finally {
            client.close()
        }
    }

    @Test
    fun `withReader - 블록 실행 후 자동 close`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        // 메시지 발행 후 Reader로 earliest부터 읽기
        val producer = client.newProducer(Schema.STRING).topic(topic).create()
        try {
            producer.sendSuspend("withReader test")
        } finally {
            producer.close()
        }

        client.withReader(Schema.STRING, {
            topic(topic)
            startMessageId(MessageId.earliest)
        }) {
            shouldNotBeNull()
            val msg = readNextSuspend()
            msg.value shouldBeEqualTo "withReader test"
        }
        client.close()
    }

    @Test
    fun `withReader - earliest부터 모든 메시지 읽기`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val messageCount = 3

        val producer = client.newProducer(Schema.STRING).topic(topic).create()
        try {
            repeat(messageCount) { i -> producer.sendSuspend("r-msg-$i") }
        } finally {
            producer.close()
        }

        client.withReader(Schema.STRING, {
            topic(topic)
            startMessageId(MessageId.earliest)
        }) {
            repeat(messageCount) { i ->
                val msg = readNextSuspend()
                msg.value shouldBeEqualTo "r-msg-$i"
            }
        }
        client.close()
    }
}
