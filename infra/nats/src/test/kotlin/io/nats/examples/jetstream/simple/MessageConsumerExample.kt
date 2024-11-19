package io.nats.examples.jetstream.simple

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.client.api.consumerConfiguration
import io.bluetape4k.nats.client.createOrReplaceStream
import io.nats.client.MessageConsumer
import io.nats.client.MessageHandler
import kotlinx.atomicfu.atomic
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class MessageConsumerExample: AbstractSimpleExample() {

    companion object: KLogging() {
        private const val STREAM = "consume-handler-stream"
        private const val SUBJECT = "consume-handler-subject"
        private const val CONSUMER_NAME = "consume-handler-consumer"
        private const val MESSAGE_TEXT = "consume-handler"
        private const val STOP_COUNT = 500
        private const val REPORT_EVERY = 100
    }

    @Test
    fun `using MessageConsumer`() {
        getConnection().use { nc ->
            nc.createOrReplaceStream(STREAM, SUBJECT)
            val js = nc.jetStream()

            // publishing so there are lots of messages
            log.debug { "Publishing ..." }
            publish(js, SUBJECT, MESSAGE_TEXT, 2500)

            // get stream context, create consumer and get the consumer context
            val streamContext = nc.getStreamContext(STREAM)
            val consumerContext = streamContext.createOrUpdateConsumer(consumerConfiguration { durable(CONSUMER_NAME) })

            val latch = CountDownLatch(1)
            val counter = atomic(0)
            val startTime = System.nanoTime()

            val handler = MessageHandler { msg ->
                msg.ack()
                val count = counter.incrementAndGet()
                if (count % REPORT_EVERY == 0) {
                    report("Handler", startTime, count)
                }
                if (count == STOP_COUNT) {
                    latch.countDown()
                }
            }

            val consumer: MessageConsumer = consumerContext.consume(handler)
            latch.await()
            log.debug { "Stop the consumer ..." }
            consumer.stop()
            Thread.sleep(1000)

            report("Final", startTime, counter.value)
        }
    }

    private fun report(label: String, start: Long, count: Int) {
        val ms = (System.nanoTime() - start) / 1000000
        log.debug { "$label: Received $count messages in ${ms}ms." }
    }
}
