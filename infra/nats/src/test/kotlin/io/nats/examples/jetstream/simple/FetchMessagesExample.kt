package io.nats.examples.jetstream.simple

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.nats.client.api.fetchConsumeOptionsOf
import io.bluetape4k.nats.client.createOrReplaceStream
import io.bluetape4k.support.toUtf8String
import io.nats.client.Connection
import io.nats.client.JetStream
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * This example will demonstrate simplified fetch
 * SIMPLIFICATION IS EXPERIMENTAL AND SUBJECT TO CHANGE
 */
class FetchMessagesExample: AbstractSimpleExample() {

    companion object: KLogging() {
        private const val STREAM = "fetch-messages-stream"
        private const val SUBJECT = "fetch-messages-subject"
        private const val MESSAGE_TEXT = "fetch-messages"
        private const val CONSUMER_NAME_PREFIX = "fetch-messages-consumer"
        private const val MESSAGES = 20
        private const val EXPIRES_SECONDS = 2
    }

    @Test
    fun `fetch messages`() {
        getConnection().use { nc ->
            val js = nc.jetStream()

            // set up the stream and publish data
            nc.createOrReplaceStream(STREAM, SUBJECT)
            publish(js, SUBJECT, MESSAGE_TEXT, MESSAGES)

            // Different fetch max messages demonstrate expiration behavior

            // A. equal number of message to the fetch max message
            simpleFetch(nc, js, "A", MESSAGES)

            // B. more messages than the fetch max messages
            simpleFetch(nc, js, "B", 10)

            // C. fewer message than the fetch max messages
            simpleFetch(nc, js, "C", 40)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun simpleFetch(nc: Connection, js: JetStream, label: String, maxMessages: Int) {
        val consumerName = "$CONSUMER_NAME_PREFIX-$maxMessages-messages"

        // get stream context, create consumer and get the consumer context
        val streamContext = nc.getStreamContext(STREAM)
        val consumerContext = streamContext.createOrUpdateConsumer(consumerConfiguration { durable(consumerName) })

        val fetchOptions = fetchConsumeOptionsOf(maxMessages, EXPIRES_SECONDS * 1000L)
        printExplanation(label, consumerName, maxMessages)

        var receivedMessages = 0
        val elapsed = measureTimeMillis {
            consumerContext.fetch(fetchOptions).use { consumer ->
                var msg = consumer.nextMessage()
                while (msg != null) {
                    log.trace { "msg=${msg.data.toUtf8String()}" }
                    msg.ack()
                    if (++receivedMessages == maxMessages) {
                        msg = null
                    } else {
                        msg = consumer.nextMessage()
                    }
                }
            }
        }
        printSummary(receivedMessages, elapsed)
    }

    private fun printSummary(received: Int, elapsed: Long) {
        log.debug { "Fetch executed and $received message(s) took $elapsed msec" }
    }

    private fun printExplanation(label: String, name: String, maxMessages: Int) {
        log.debug { "--------------------------------------------------------------------------------" }
        log.debug { "$label. $name" }
        when (label) {
            "A", "B" -> {
                log.debug { "=== Fetch ($maxMessages) is less than or equal to available messages ($MESSAGES)" }
                log.debug { "=== nextMessage() will return null when consume is done" }
            }

            "C"      -> {
                log.debug { "=== Fetch ($maxMessages) is larger than available messages ($MESSAGES)" }
                log.debug { "=== FetchConsumeOption \"expires in\" is $EXPIRES_SECONDS seconds." }
                log.debug { "=== nextMessage() blocks until expiration when there are no messages available, then returns null." }
            }

            "D"      -> {
                log.debug { "=== Fetch ($maxMessages) is larger than available messages (0)" }
                log.debug { "=== FetchConsumeOption \"expires in\" is $EXPIRES_SECONDS seconds." }
                log.debug { "=== nextMessage() blocks until expiration when there are no messages available, then returns null." }
            }
        }
    }
}
