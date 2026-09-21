package io.bluetape4k.nats.client

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.nats.AbstractNatsTest
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.nats.client.Subscription
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class SubscriptionExtensionsTest: AbstractNatsTest() {

    companion object: KLogging()

    private val subscription = mockk<Subscription>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(subscription)
    }

    @Test
    fun `nextMessage returns null when timed out`() {
        every { subscription.nextMessage(Duration.ofSeconds(1)) } returns null

        val message = subscription.nextMessage(1.seconds)
        message.shouldBeNull()
    }

    @Test
    fun `nextMessage returns received message`() {
        val received = natsMessage {
            this.subject("subject")
            this.replyTo("replyTo")
            this.data(Base58.randomString(8))
        }
        every { subscription.nextMessage(Duration.ofSeconds(1)) } returns received

        val message = subscription.nextMessage(1.seconds).shouldNotBeNull()

        log.debug { "message=$message" }
        message shouldBeEqualTo received
    }

    @Test
    fun `nextMessage with zero timeout`() {
        every { subscription.nextMessage(Duration.ofSeconds(0)) } returns null

        val message = subscription.nextMessage(kotlin.time.Duration.ZERO)
        message.shouldBeNull()
    }
}
