package io.bluetape4k.pulsar.codec

import io.bluetape4k.logging.KLogging
import io.bluetape4k.pulsar.AbstractPulsarTest
import io.bluetape4k.pulsar.consumer.receiveSuspend
import io.bluetape4k.pulsar.producer.sendSuspend
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.pulsar.common.schema.SchemaType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.Serializable
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JacksonSchemaTest: AbstractPulsarTest() {

    companion object: KLogging() {
        private val TEST_ORDER = Order(id = "order-1", amount = 9900, item = "Bluetooth Speaker")
    }

    data class Order(
        val id: String,
        val amount: Int,
        val item: String,
    ): Serializable

    @Test
    fun `jacksonSchema - SchemaInfo 타입 확인`() {
        val schema = jacksonSchema<Order>()
        schema.schemaInfo.shouldNotBeNull()
        schema.schemaInfo.type shouldBeEqualTo SchemaType.JSON
        schema.schemaInfo.name shouldBeEqualTo "Order"
    }

    @Test
    fun `jacksonSchema - encode와 decode 왕복`() {
        val schema = jacksonSchema<Order>()
        val encoded = schema.encode(TEST_ORDER)
        val decoded = schema.decode(encoded)
        decoded shouldBeEqualTo TEST_ORDER
    }

    @Test
    fun `jacksonSchema - Pulsar 메시지 발행 및 수신`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val schema = jacksonSchema<Order>()

        val consumer = client.newConsumer(schema)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscribe()
        val producer = client.newProducer(schema)
            .topic(topic)
            .create()
        try {
            producer.sendSuspend(TEST_ORDER)
            val msg = consumer.receiveSuspend()
            msg.value shouldBeEqualTo TEST_ORDER
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `jacksonSchema - clone 후 독립 동작`() {
        val schema = jacksonSchema<Order>()
        val cloned = schema.clone()
        val encoded = cloned.encode(TEST_ORDER)
        val decoded = cloned.decode(encoded)
        decoded shouldBeEqualTo TEST_ORDER
    }
}
