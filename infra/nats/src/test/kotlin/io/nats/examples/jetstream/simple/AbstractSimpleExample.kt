package io.nats.examples.jetstream.simple

import io.bluetape4k.logging.KLogging
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.publish
import io.nats.client.JetStream
import kotlinx.atomicfu.atomic
import kotlin.random.Random

abstract class AbstractSimpleExample: AbstractNatsTest() {

    companion object: KLogging()

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
        private val keepGoing = atomic(true)

        fun stopPublishing() {
            keepGoing.compareAndSet(expect = true, update = false)
        }

        override fun run() {
            while (keepGoing.value) {
                Thread.sleep(Random.nextLong(jitter.toLong()))
                js.publish(subject, "$messageText-${++pubNo}")
            }
        }
    }
}
