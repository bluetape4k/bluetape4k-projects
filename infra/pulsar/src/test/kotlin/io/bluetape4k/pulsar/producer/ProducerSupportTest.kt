package io.bluetape4k.pulsar.producer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.assertCleanupWaitsAfterCancellation
import io.bluetape4k.pulsar.consumer.receiveSuspend
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.pulsar.client.api.CompressionType
import org.apache.pulsar.client.api.Producer
import org.apache.pulsar.client.api.ProducerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProducerSupportTest : AbstractPulsarTest() {

    companion object : KLogging()

    @Test
    fun `producer DSL - Schema와 setup으로 Producer 생성`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        try {
            val producer = client.producer(Schema.STRING) {
                topic(topic)
                producerName("test-producer")
            }
            producer.shouldNotBeNull()
            producer.close()
        } finally {
            client.close()
        }
    }

    @Test
    fun `withProducer - 블록 실행 후 자동 close`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        // Consumer 먼저 구독 → withProducer로 발행
        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscriptionType(SubscriptionType.Exclusive)
            .subscribe()
        try {
            client.withProducer(Schema.STRING, { topic(topic) }) {
                shouldNotBeNull()
                val msgId = sendSuspend("withProducer test")
                msgId.shouldNotBeNull()
            }
            val msg = consumer.receiveSuspend()
            msg.value shouldBeEqualTo "withProducer test"
        } finally {
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `withProducer - compressionType 설정 동작`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscribe()
        try {
            client.withProducer(Schema.STRING, {
                topic(topic)
                compressionType(CompressionType.LZ4)
            }) {
                sendSuspend("compressed")
            }
            val msg = consumer.receiveSuspend()
            msg.value shouldBeEqualTo "compressed"
        } finally {
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `withProducer - 취소되어도 closeAsync 완료를 기다린다`() = runTest {
        val client = mockk<PulsarClient>()
        val builder = mockk<ProducerBuilder<String>>()
        val producer = mockk<Producer<String>>()
        val closeFuture = CompletableFuture<Void>()

        every { client.newProducer(Schema.STRING) } returns builder
        every { builder.create() } returns producer
        every { producer.closeAsync() } returns closeFuture

        assertCleanupWaitsAfterCancellation(closeFuture) { entered ->
            client.withProducer(Schema.STRING) {
                entered.complete(Unit)
                awaitCancellation()
            }
        }

        verify(exactly = 1) { producer.closeAsync() }
    }
}
