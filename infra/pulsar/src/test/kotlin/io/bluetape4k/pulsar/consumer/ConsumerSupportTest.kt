package io.bluetape4k.pulsar.consumer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
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
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.ConsumerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class ConsumerSupportTest: AbstractPulsarTest() {

    companion object: KLogging()

    @Test
    fun `consumer DSL - Schema와 setup으로 Consumer 생성`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()

            val consumer = client.consumer(Schema.STRING) {
                topic(topic)
                subscriptionName(newSubscription())
                subscriptionType(SubscriptionType.Exclusive)
            }
            consumer.shouldNotBeNull()
            consumer.close()
        }
    }

    @Test
    fun `withConsumer - 블록 실행 후 자동 close`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()
            val sub = newSubscription()

            // Consumer 먼저 구독 → 이후 메시지 발행
            client.withConsumer(Schema.STRING, {
                topic(topic)
                subscriptionName(sub)
                subscriptionType(SubscriptionType.Exclusive)
            }) {
                // Consumer 생성 후 메시지 발행
                this@runSuspendIO.launch {
                    client.newProducer(Schema.STRING)
                        .topic(topic)
                        .create().use { producer ->
                            producer.sendSuspend("withConsumer test")
                        }
                }
                val msg = receiveSuspend()
                msg.value shouldBeEqualTo "withConsumer test"
                acknowledgeSuspend(msg)
            }
        }
    }

    @Test
    fun `withConsumer - 복수 메시지 처리`() = runSuspendIO {
        val received = mutableListOf<String>()
        val messageCount = 3

        newClient().use { client ->
            val topic = newTopic()
            val sub = newSubscription()

            client.withConsumer(Schema.STRING, {
                topic(topic)
                subscriptionName(sub)
            }) {
                // Consumer 생성 후 메시지 발행
                this@runSuspendIO.launch {
                    client.newProducer(Schema.STRING)
                        .topic(topic)
                        .create()
                        .use { producer ->
                            repeat(messageCount) { producer.sendSuspend("msg-$it") }
                        }
                }
                repeat(messageCount) {
                    val msg = receiveSuspend()
                    received.add(msg.value)
                    acknowledgeSuspend(msg)
                }
            }
        }

        received shouldBeEqualTo List(messageCount) { "msg-$it" }
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
