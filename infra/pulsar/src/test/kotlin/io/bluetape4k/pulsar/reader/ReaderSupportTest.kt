package io.bluetape4k.pulsar.reader

import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.assertCleanupWaitsAfterCancellation
import io.bluetape4k.pulsar.producer.sendSuspend
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.pulsar.client.api.MessageId
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Reader
import org.apache.pulsar.client.api.ReaderBuilder
import org.apache.pulsar.client.api.Schema
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture
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
    fun `withReader - 취소되어도 closeAsync 완료를 기다린다`() = runTest {
        val client = mockk<PulsarClient>()
        val builder = mockk<ReaderBuilder<String>>()
        val reader = mockk<Reader<String>>()
        val closeFuture = CompletableFuture<Void>()

        every { client.newReader(Schema.STRING) } returns builder
        every { builder.create() } returns reader
        every { reader.closeAsync() } returns closeFuture

        assertCleanupWaitsAfterCancellation(closeFuture) { entered ->
            client.withReader(Schema.STRING) {
                entered.complete(Unit)
                awaitCancellation()
            }
        }

        verify(exactly = 1) { reader.closeAsync() }
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
