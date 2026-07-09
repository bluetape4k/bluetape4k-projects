package io.bluetape4k.pulsar.consumer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.assertCleanupWaitsAfterCancellation
import io.bluetape4k.pulsar.producer.sendSuspend
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.ConsumerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsumerSupportTest : AbstractPulsarTest() {

    companion object : KLogging()

    @Test
    fun `consumer DSL - Schema와 setup으로 Consumer 생성`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        try {
            val consumer = client.consumer(Schema.STRING) {
                topic(topic)
                subscriptionName(newSubscription())
                subscriptionType(SubscriptionType.Exclusive)
            }
            consumer.shouldNotBeNull()
            consumer.close()
        } finally {
            client.close()
        }
    }

    @Test
    fun `withConsumer - 블록 실행 후 자동 close`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val sub = newSubscription()

        // Consumer 먼저 구독 → 이후 메시지 발행
        client.withConsumer(Schema.STRING, {
            topic(topic)
            subscriptionName(sub)
            subscriptionType(SubscriptionType.Exclusive)
        }) {
            shouldNotBeNull()
            // Consumer 생성 후 메시지 발행
            launch {
                val producer = client.newProducer(Schema.STRING).topic(topic).create()
                producer.sendSuspend("withConsumer test")
                producer.close()
            }
            val msg = receiveSuspend()
            msg.value shouldBeEqualTo "withConsumer test"
            acknowledgeSuspend(msg)
        }
        client.close()
    }

    @Test
    fun `withConsumer - 복수 메시지 처리`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val sub = newSubscription()

        val received = mutableListOf<String>()
        client.withConsumer(Schema.STRING, {
            topic(topic)
            subscriptionName(sub)
        }) {
            // Consumer 생성 후 메시지 발행
            launch {
                val producer = client.newProducer(Schema.STRING).topic(topic).create()
                repeat(3) { i -> producer.sendSuspend("msg-$i") }
                producer.close()
            }
            repeat(3) {
                val msg = receiveSuspend()
                received.add(msg.value)
                acknowledgeSuspend(msg)
            }
        }
        client.close()

        received.forEachIndexed { i, v -> v shouldBeEqualTo "msg-$i" }
    }

    @Test
    fun `withConsumer - 취소되어도 closeAsync 완료를 기다린다`() = runTest {
        val client = mockk<PulsarClient>()
        val builder = mockk<ConsumerBuilder<String>>()
        val consumer = mockk<Consumer<String>>()
        val closeFuture = CompletableFuture<Void>()

        every { client.newConsumer(Schema.STRING) } returns builder
        every { builder.subscribe() } returns consumer
        every { consumer.closeAsync() } returns closeFuture

        assertCleanupWaitsAfterCancellation(closeFuture) { entered ->
            client.withConsumer(Schema.STRING) {
                entered.complete(Unit)
                awaitCancellation()
            }
        }

        verify(exactly = 1) { consumer.closeAsync() }
    }
}
