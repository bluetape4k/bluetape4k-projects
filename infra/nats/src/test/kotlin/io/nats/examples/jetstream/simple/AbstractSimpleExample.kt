package io.nats.examples.jetstream.simple

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.publish
import io.nats.client.JetStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

abstract class AbstractSimpleExample: AbstractNatsTest() {

    companion object: KLoggingChannel()

    protected fun publish(js: JetStream, subject: String, messageText: String, count: Int) {
        for (i in 1..count) {
            js.publish(subject, "$messageText-$i")
        }
    }

    class Publisher(
        private val js: JetStream,
        private val subject: String,
        private val messageText: String,
        private val jitter: Int,
    ): Runnable {

        private var pubNo: Int = 0
        private val keepGoing = AtomicBoolean(true)

        fun stopPublishing() {
            keepGoing.compareAndSet(true, false)
        }

        override fun run() {
            while (keepGoing.get()) {
                Thread.sleep(Random.nextLong(jitter.toLong()))
                js.publish(subject, "$messageText-${++pubNo}")
            }
        }
    }
}
