package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.nats.client.Subscription
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import io.bluetape4k.assertions.assertFailsWith

class NatsServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class Docker {
        @Test
        fun `create nats server`() {
            NatsServer().use { nats ->
                nats.start()
                nats.isRunning.shouldBeTrue()

                connectToNats(nats)
            }
        }
    }

    @Nested
    inner class Default {
        @Test
        fun `create nats server with default port`() {
            NatsServer(useDefaultPort = true).use { nats ->
                nats.start()
                nats.isRunning.shouldBeTrue()

                connectToNats(nats)
            }
        }
    }

    private fun connectToNats(nats: NatsServer) {
        withNats(nats.url) {
            // await until { this.status == Connection.Status.CONNECTED }

            val subscription: Subscription = this.subscribe("subject")
            subscription.isActive.shouldBeTrue()

            this.publish("subject", "Hello world".toUtf8Bytes())
            val message = subscription.nextMessage(Duration.ofMillis(500))

            message.subject shouldBeEqualTo "subject"
            message.subscription shouldBeEqualTo subscription
            message.replyTo.shouldBeNull()
            message.data.toUtf8String() shouldBeEqualTo "Hello world"
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { NatsServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { NatsServer(tag = " ") }
    }
}
