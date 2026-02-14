package io.bluetape4k.testcontainers.mq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFailsWith

class RabbitMQServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val RABBITMQ_TEST_EXCHANGE = "TestExchange"
        private const val RABBITMQ_TEST_ROUTING_KEY = "TestRoutingKey"
        private const val RABBITMQ_TEST_MESSAGE = "Hello world"
    }

    @Nested
    inner class Docker {
        private val rabbitMQ = RabbitMQServer.Launcher.rabbitMQ

        @AfterAll
        fun afterAll() {
            rabbitMQ.close()
        }

        @Test
        fun `connect to rabbitmq server`() {
            rabbitMQ.isRunning.shouldBeTrue()
            log.debug { "host=${rabbitMQ.host}, port=${rabbitMQ.port}" }

            val factory = ConnectionFactory().apply {
                host = rabbitMQ.host
                port = rabbitMQ.port
            }

            factory.newConnection().use { connection ->
                connection.shouldNotBeNull()

                connection.createChannel().use { channel ->
                    channel.shouldNotBeNull()
                    channel.exchangeDeclare(RABBITMQ_TEST_EXCHANGE, "direct", true)

                    val queueName = channel.queueDeclare().queue
                    channel.queueBind(queueName, RABBITMQ_TEST_EXCHANGE, RABBITMQ_TEST_ROUTING_KEY)

                    val messageWasReceived = AtomicBoolean(false)
                    channel.basicConsume(queueName, true, object: DefaultConsumer(channel) {
                        override fun handleDelivery(
                            consumerTag: String?,
                            envelope: Envelope?,
                            properties: AMQP.BasicProperties?,
                            body: ByteArray?,
                        ) {
                            messageWasReceived.set(Arrays.equals(body, RABBITMQ_TEST_MESSAGE.toByteArray()))
                        }
                    })

                    channel.basicPublish(
                        RABBITMQ_TEST_EXCHANGE,
                        RABBITMQ_TEST_ROUTING_KEY,
                        null,
                        RABBITMQ_TEST_MESSAGE.toByteArray()
                    )

                    await atMost Duration.ofSeconds(5) until { messageWasReceived.get() }
                    messageWasReceived.get().shouldBeTrue()
                }
            }
        }
    }

    @Nested
    inner class Default {
        @Test
        fun `run rabbitmq server with default port`() {
            RabbitMQServer(useDefaultPort = true).use { rabbitmq ->
                rabbitmq.start()
                rabbitmq.isRunning.shouldBeTrue()
                log.debug { "url = ${rabbitmq.url}" }

                rabbitmq.port shouldBeEqualTo RabbitMQServer.AMQP_PORT
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { RabbitMQServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { RabbitMQServer(tag = " ") }
    }
}
