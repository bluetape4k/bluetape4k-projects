package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.apache.pulsar.client.api.PulsarClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PulsarServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val TEST_TOPIC_NAME = "pulsar.test-topic.1"
    }

    @Nested
    inner class Docker {
        @Test
        fun `Pulsar Server 실행`() {
            PulsarServer().use { pulsar ->
                pulsar.start()
                pulsar.isRunning.shouldBeTrue()

                verifyPulsarMessaging(pulsar)
            }
        }
    }

    @Nested
    inner class Default {
        @Test
        fun `Pulsar Server 실행`() {
            PulsarServer(useDefaultPort = true).use { pulsar ->
                pulsar.start()
                pulsar.isRunning.shouldBeTrue()

                pulsar.port shouldBeEqualTo PulsarServer.PORT
                pulsar.brokerPort shouldBeEqualTo PulsarServer.PORT
                pulsar.brokerHttpPort shouldBeEqualTo PulsarServer.HTTP_PORT

                verifyPulsarMessaging(pulsar)
            }
        }
    }

    private fun createPulsarClient(brokerUrl: String): PulsarClient {
        return PulsarServer.Launcher.PulsarClient(brokerUrl) {
            startingBackoffInterval(10, TimeUnit.MILLISECONDS)
            enableBusyWait(true)
            enableTcpNoDelay(true)
        }
    }

    private fun verifyPulsarMessaging(pulsar: PulsarServer) {
        val brokerUrl = pulsar.pulsarBrokerUrl
        val client = createPulsarClient(brokerUrl)

        val consumer = client.newConsumer()
            .topic(TEST_TOPIC_NAME)
            .subscriptionName("test-stubs")
            .subscribe()

        val producer = client.newProducer().topic(TEST_TOPIC_NAME).create()

        try {
            val messageBody = "안녕하세요. Pulsar!"
            producer.send(messageBody.toByteArray(Charsets.UTF_8))

            val future = consumer.receiveAsync()
            val message = future.get(5, TimeUnit.SECONDS)

            message.data.toString(Charsets.UTF_8) shouldBeEqualTo messageBody
        } finally {
            consumer.close()
            producer.close()
            client.close()
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { PulsarServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { PulsarServer(tag = " ") }
    }

    @Test
    fun `전달받은 image 를 사용한다`() {
        val server = PulsarServer(image = "apachepulsar/pulsar", tag = "3.3.5")
        assertEquals("apachepulsar/pulsar:3.3.5", server.dockerImageName)
    }
}
