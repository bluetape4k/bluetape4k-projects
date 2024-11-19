package io.nats.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.bluetape4k.nats.client.publish
import io.bluetape4k.support.toUtf8String
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PubSubExample: AbstractNatsTest() {

    companion object: KLogging() {
        private const val TEST_SUBJECT = "greet.joe"
    }

    @Test
    fun `publish and subscription`() {
        getConnection().use { nc ->
            val latch = CountDownLatch(3)

            val body = "hello"
            nc.publish(TEST_SUBJECT, body)

            // Create a message dispatcher for handling messages in a
            // separate thread and then subscribe to the target subject
            // which leverages a wildcard `greet.*`.
            val dispatcher = nc.createDispatcher { msg ->
                log.debug { "`${msg.data.toUtf8String()}` on subject `${msg.subject}`" }
                latch.countDown()
            }

            dispatcher.subscribe("greet.*")

            // Publish more messages that will be received by the subscription
            // since they match the wildcard. Note the first message on
            // `greet.joe` was not received because we were not subscribed when
            // it was published
            nc.publish("greet.bob", body)
            nc.publish("greet.sue", body)
            nc.publish("greet.pam", body)

            latch.await(500, TimeUnit.MILLISECONDS)
        }
    }
}
