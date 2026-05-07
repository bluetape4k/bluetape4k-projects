package io.bluetape4k.pulsar.consumer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.producer.sendSuspend
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.apache.pulsar.client.api.PulsarClientException
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import io.bluetape4k.assertions.assertFailsWith
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConsumerExtensionsTest: AbstractPulsarTest() {

    companion object: KLogging()

    @Test
    fun `receiveSuspend - 메시지 수신`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscribe()
        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        try {
            producer.sendSuspend("hello consumer")
            val msg = consumer.receiveSuspend()
            msg.value shouldBeEqualTo "hello consumer"
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `acknowledgeSuspend - 메시지 ack 처리`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val sub = newSubscription()

        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(sub)
            .subscriptionType(SubscriptionType.Exclusive)
            .subscribe()
        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        try {
            producer.sendSuspend("ack test")
            val msg = consumer.receiveSuspend()
            consumer.acknowledgeSuspend(msg)
            msg.value shouldBeEqualTo "ack test"
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `acknowledgeCumulativeSuspend - Exclusive subscription cumulative ack`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscriptionType(SubscriptionType.Exclusive)
            .subscribe()
        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        try {
            repeat(3) { i -> producer.sendSuspend("msg-$i") }

            val msgs = (1..3).map { consumer.receiveSuspend() }
            msgs shouldHaveSize 3
            consumer.acknowledgeCumulativeSuspend(msgs.last())
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `acknowledgeCumulativeSuspend - Shared subscription에서 예외 발생`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscriptionType(SubscriptionType.Shared)
            .subscribe()
        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        try {
            producer.sendSuspend("msg")
            val msg = consumer.receiveSuspend()

            assertFailsWith<PulsarClientException> {
                consumer.acknowledgeCumulativeSuspend(msg)
            }
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `receiveAsFlow - 취소 시 정상 종료`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val messageCount = 5

        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscribe()
        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        try {
            // 메시지를 미리 발행
            repeat(messageCount) { i -> producer.sendSuspend("flow-msg-$i") }

            // Flow에서 지정 개수만 수신 후 취소
            val received = consumer.receiveAsFlow()
                .take(messageCount)
                .toList()

            received shouldHaveSize messageCount
            received.mapIndexed { i, msg -> msg.value shouldBeEqualTo "flow-msg-$i" }
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `receiveAsFlow - 수신 후 ack 처리`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()

        val consumer = client.newConsumer(Schema.STRING)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscriptionType(SubscriptionType.Exclusive)
            .subscribe()
        val producer = client.newProducer(Schema.STRING)
            .topic(topic)
            .create()
        try {
            launch {
                repeat(3) { i -> producer.sendSuspend("ack-msg-$i") }
            }

            val received = mutableListOf<String>()
            consumer.receiveAsFlow().take(3).collect { msg ->
                received.add(msg.value)
                consumer.acknowledgeSuspend(msg)
            }

            received shouldHaveSize 3
            received.forEachIndexed { i, v -> v shouldBeEqualTo "ack-msg-$i" }
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }
}
