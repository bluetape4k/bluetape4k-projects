package io.bluetape4k.nats

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.client.flush
import io.bluetape4k.nats.client.publish
import io.bluetape4k.support.toUtf8String
import io.nats.client.Message
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SimplePublishExample: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `publish and subscribe`() {
        getConnection().use { conn ->
            val subscription = conn.subscribe(TEST_SUBJECT)
            subscription.isActive.shouldBeTrue()

            val sendBody = "Hello world"
            conn.publish(TEST_SUBJECT, sendBody)
            conn.flush(5.seconds)

            val message: Message = subscription.nextMessage(500.milliseconds.toJavaDuration())

            log.debug { "message=$message" }
            message.subject shouldBeEqualTo TEST_SUBJECT
            message.subscription shouldBeEqualTo subscription
            message.replyTo.shouldBeNull()
            message.data.toUtf8String() shouldBeEqualTo sendBody
        }
    }

    @Test
    fun `use dispatcher`() {
        getConnection().use { conn ->
            val dispatcher = conn.createDispatcher { _: Message? -> }

            val latch = CountDownLatch(1)
            val subscription = dispatcher.subscribe(TEST_SUBJECT) { msg ->
                val response = msg.data.toUtf8String()
                log.debug { "Message received (up to 100 times): $response" }
                latch.countDown()
            }

            val sendBody = "Hello world"
            conn.publish(TEST_SUBJECT, sendBody)
            conn.flush(1.seconds)

            subscription.isActive.shouldBeTrue()
            latch.await(1000, TimeUnit.MILLISECONDS)
        }
    }
}
