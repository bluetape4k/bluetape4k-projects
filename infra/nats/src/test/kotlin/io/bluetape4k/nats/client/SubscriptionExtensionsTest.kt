package io.bluetape4k.nats.client

import io.mockk.every
import io.mockk.mockk
import io.nats.client.Message
import io.nats.client.Subscription
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class SubscriptionExtensionsTest {
    private val subscription = mockk<Subscription>()

    @Test
    fun `nextMessage returns null when timed out`() {
        every { subscription.nextMessage(Duration.ofSeconds(1)) } returns null

        val message = subscription.nextMessage(1.seconds)

        message.shouldBeNull()
    }

    @Test
    fun `nextMessage returns received message`() {
        val received = mockk<Message>()
        every { subscription.nextMessage(Duration.ofSeconds(1)) } returns received

        val message = subscription.nextMessage(1.seconds)

        message shouldBeSameInstanceAs received
    }

    @Test
    fun `nextMessage with zero timeout`() {
        every { subscription.nextMessage(Duration.ofSeconds(0)) } returns null

        val message = subscription.nextMessage(kotlin.time.Duration.ZERO)

        message.shouldBeNull()
    }
}
