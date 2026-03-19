package io.nats.examples.jetstream.simple

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.client.consumerContextOf
import io.bluetape4k.nats.client.createOrReplaceStream
import io.bluetape4k.nats.client.publish
import io.bluetape4k.support.toUtf8String
import io.nats.client.ConsumerContext
import io.nats.client.JetStream
import org.junit.jupiter.api.Test

class NextExample: AbstractSimpleExample() {

    companion object: KLogging() {
        private const val STREAM = "next-stream"
        private const val SUBJECT = "next-subject"
        private const val CONSUMER_NAME = "next-consumer"
    }

    @Test
    fun `consume by next`() {
        getConnection().use { nc ->
            val js = nc.jetStream()
            nc.createOrReplaceStream(STREAM, SUBJECT)

            // get stream context, create consumer and get the consumer context
            val consumerContext: ConsumerContext = consumerContextOf(nc, STREAM, CONSUMER_NAME)
            log.debug { "consumer=${consumerContext.consumerInfo}" }

            val count = 20
            val publishThread = PublishThread(js, count).apply { start() }

            var received = 0
            while (received < count) {
                val start = System.currentTimeMillis()
                val m = consumerContext.next(1000L)
                val elapsed = System.currentTimeMillis() - start
                if (m == null) {
                    log.debug { "Waited $elapsed msec for message, got null" }
                } else {
                    ++received
                    m.ack()
                    log.debug { "Waited $elapsed msec for message, got ${m.data.toUtf8String()}" }
                }
            }

            publishThread.join()
        }
    }

    // Publishing Thread
    class PublishThread(
        private val js: JetStream,
        private val count: Int = 20,
    ): Thread() {

        override fun run() {
            var sleep = 2000L
            var down = true
            for (x in 1..count) {
                Thread.sleep(sleep)
                if (down) {
                    sleep -= 200L
                    down = sleep > 0L
                } else {
                    sleep += 200
                }
                js.publish(SUBJECT, "message-$x")
            }
        }
    }
}
