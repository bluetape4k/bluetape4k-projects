package io.bluetape4k.pulsar.producer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.consumer.receiveSuspend
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.apache.pulsar.client.api.Schema
import org.apache.pulsar.client.api.SubscriptionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProducerExtensionsTest: AbstractPulsarTest() {

    companion object: KLogging()

    @Test
    fun `sendSuspend - 단일 메시지 발행`() = runSuspendIO {
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
                val msgId = producer.sendSuspend("hello pulsar")
                msgId.shouldNotBeNull()

                val msg = consumer.receiveSuspend()
                msg.value shouldBeEqualTo "hello pulsar"
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }

    @Test
    fun `sendSuspend - TypedMessageBuilder DSL 발행`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()

            val consumer = client.newConsumer(Schema.STRING)
                .topic(topic)
                .subscriptionName(newSubscription())
                .subscribe()
            val producer = client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
            try {
                val msgId = producer.sendSuspend {
                    value("DSL message")
                    key("key-1")
                    property("version", "1")
                }
                msgId.shouldNotBeNull()

                val msg = consumer.receiveSuspend()
                msg.value shouldBeEqualTo "DSL message"
                msg.key shouldBeEqualTo "key-1"
                msg.properties["version"] shouldBeEqualTo "1"
            } finally {
                producer.close()
                consumer.close()
            }
        }
    }

    @Test
    fun `sendAsFlow - Flow 기반 배치 발행`() = runSuspendIO {
        newClient().use { client ->
            val topic = newTopic()
            val messages = List(5) { "msg-$it" }

            client.newProducer(Schema.STRING)
                .topic(topic)
                .create()
                .use { producer ->
                    val ids = producer.sendAsFlow(messages.asFlow()).toList()
                    ids shouldHaveSize 5
                    ids.forEach { it.shouldNotBeNull() }
                }
        }
    }
}
