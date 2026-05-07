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
class Jackson3SchemaTest: AbstractPulsarTest() {

    companion object: KLogging() {
        private val TEST_PRODUCT = Product(sku = "PROD-42", price = 29900, name = "Kotlin Coroutines Book")
    }

    data class Product(
        val sku: String,
        val price: Int,
        val name: String,
    ): Serializable

    @Test
    fun `jackson3Schema - SchemaInfo 타입 확인`() {
        val schema = jackson3Schema<Product>()
        schema.schemaInfo.shouldNotBeNull()
        schema.schemaInfo.type shouldBeEqualTo SchemaType.JSON
        schema.schemaInfo.name shouldBeEqualTo "Product"
    }

    @Test
    fun `jackson3Schema - encode와 decode 왕복`() {
        val schema = jackson3Schema<Product>()
        val encoded = schema.encode(TEST_PRODUCT)
        val decoded = schema.decode(encoded)
        decoded shouldBeEqualTo TEST_PRODUCT
    }

    @Test
    fun `jackson3Schema - Pulsar 메시지 발행 및 수신`() = runTest(timeout = 30.seconds) {
        val client = newClient()
        val topic = newTopic()
        val schema = jackson3Schema<Product>()

        val consumer = client.newConsumer(schema)
            .topic(topic)
            .subscriptionName(newSubscription())
            .subscribe()
        val producer = client.newProducer(schema)
            .topic(topic)
            .create()
        try {
            producer.sendSuspend(TEST_PRODUCT)
            val msg = consumer.receiveSuspend()
            msg.value shouldBeEqualTo TEST_PRODUCT
        } finally {
            producer.close()
            consumer.close()
            client.close()
        }
    }

    @Test
    fun `jackson3Schema - clone 후 독립 동작`() {
        val schema = jackson3Schema<Product>()
        val cloned = schema.clone()
        val encoded = cloned.encode(TEST_PRODUCT)
        val decoded = cloned.decode(encoded)
        decoded shouldBeEqualTo TEST_PRODUCT
    }
}
