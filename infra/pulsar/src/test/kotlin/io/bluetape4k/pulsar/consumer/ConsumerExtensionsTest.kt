package io.bluetape4k.pulsar.consumer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.producer.sendSuspend
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.apache.pulsar.client.api.PulsarClientException
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsumerExtensionsTest: AbstractPulsarTest() {

    companion object: KLoggingChannel()

    @Test
    fun `receiveSuspend - 메시지 수신`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()

            val consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(newSubscription())
                .subscribe()
                .shouldNotBeNull()

            val producer = client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
                .shouldNotBeNull()

            try {
                producer.sendSuspend("hello consumer")
                val msg = consumer.receiveSuspend()
                msg.value shouldBeEqualTo "hello consumer"
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }

    @Test
    fun `acknowledgeSuspend - 메시지 ack 처리`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()
            val sub = newSubscription()

            val consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(sub)
                .subscriptionType(SubscriptionType.Exclusive)
                .subscribe()
                .shouldNotBeNull()

            val producer = client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
                .shouldNotBeNull()

            try {
                producer.sendSuspend("ack test")
                val msg = consumer.receiveSuspend()
                consumer.acknowledgeSuspend(msg)
                msg.value shouldBeEqualTo "ack test"
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }

    @Test
    fun `acknowledgeCumulativeSuspend - Exclusive subscription cumulative ack`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()

            val consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(newSubscription())
                .subscriptionType(SubscriptionType.Exclusive)
                .subscribe()
                .shouldNotBeNull()

            val producer = client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
                .shouldNotBeNull()

            try {
                val count = 10
                repeat(count) {
                    producer.sendSuspend("msg-$it")
                }
                val msgs = List(count) { consumer.receiveSuspend() }

                msgs shouldHaveSize count
                consumer.acknowledgeCumulativeSuspend(msgs.last())
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }

    @Test
    fun `acknowledgeCumulativeSuspend - Shared subscription에서 예외 발생`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()

            val consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(newSubscription())
                .subscriptionType(SubscriptionType.Shared)     // SubscriptionType 이 Shared 일 때에는 누적 ack 에서 예외가 발생합니다.
                .subscribe()
                .shouldNotBeNull()

            val producer = client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
                .shouldNotBeNull()

            try {
                producer.sendSuspend("msg")
                val msg = consumer.receiveSuspend().shouldNotBeNull()

                assertFailsWith<PulsarClientException> {
                    consumer.acknowledgeCumulativeSuspend(msg)
                }
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }

    @Test
    fun `receiveAsFlow - 취소 시 정상 종료`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()
            val messageCount = 10

            val consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(newSubscription())
                .subscribe()
                .shouldNotBeNull()

            val producer = client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
                .shouldNotBeNull()

            try {
                // 메시지를 미리 발행
                repeat(messageCount) {
                    producer.sendSuspend("flow-msg-$it")
                }

                // Flow에서 지정 개수만 수신 후 취소
                val received = consumer.receiveAsFlow()
                    .take(messageCount - 1)
                    .toList()

                received shouldHaveSize messageCount - 1
                received.mapIndexed { i, msg ->
                    msg.value shouldBeEqualTo "flow-msg-$i"
                }
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }

    @Test
    fun `receiveAsFlow - 수신 후 ack 처리`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()

            val consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(newSubscription())
                .subscriptionType(SubscriptionType.Exclusive)
                .subscribe()
                .shouldNotBeNull()

            val producer = client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
                .shouldNotBeNull()

            try {
                val messageCount = 3
                val jobs = List(messageCount) {
                    launch {
                        producer.sendSuspend("ack-msg-$it")
                    }
                }

                val received = mutableListOf<String>()
                consumer.receiveAsFlow()
                    .take(messageCount)
                    .collect { msg ->
                        received.add(msg.value)
                        consumer.acknowledgeSuspend(msg)
                    }

                await atMost 5.seconds untilSuspending {
                    jobs.all { it.isCompleted }
                }

                received shouldHaveSize messageCount
                received shouldContainSame List(messageCount) { "ack-msg-$it" }
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }
}
