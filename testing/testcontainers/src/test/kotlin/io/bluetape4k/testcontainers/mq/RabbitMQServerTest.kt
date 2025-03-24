package io.bluetape4k.testcontainers.mq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.*

@Execution(ExecutionMode.SAME_THREAD)
class RabbitMQServerTest {

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

            val connection = factory.newConnection()
            connection.shouldNotBeNull()

            val channel = connection.createChannel()
            channel.shouldNotBeNull()
            channel.exchangeDeclare(RABBITMQ_TEST_EXCHANGE, "direct", true)

            val queueName = channel.queueDeclare().queue
            channel.queueBind(queueName, RABBITMQ_TEST_EXCHANGE, RABBITMQ_TEST_ROUTING_KEY)

            // Set up a consumer on the queue
            var messageWasReceived = false
            channel.basicConsume(queueName, false, object: DefaultConsumer(channel) {
                override fun handleDelivery(
                    consumerTag: String?,
                    envelope: Envelope?,
                    properties: AMQP.BasicProperties?,
                    body: ByteArray?,
                ) {
                    messageWasReceived = Arrays.equals(body, RABBITMQ_TEST_MESSAGE.toByteArray())
                }
            })

            // post a message
            channel.basicPublish(
                RABBITMQ_TEST_EXCHANGE,
                RABBITMQ_TEST_ROUTING_KEY,
                null,
                RABBITMQ_TEST_MESSAGE.toByteArray()
            )

            // check the message was received
            Thread.sleep(1000)
            messageWasReceived.shouldBeTrue()

            connection.close()
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
}
