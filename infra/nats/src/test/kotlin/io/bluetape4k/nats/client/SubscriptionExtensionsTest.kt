package io.bluetape4k.nats.client

import io.mockk.every
import io.mockk.mockk
import io.nats.client.Message
import io.nats.client.Subscription
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

class SubscriptionExtensionsTest {

    private val subscription = mockk<Subscription>()

    @Test
    fun `nextMessage returns null when timed out`() {
        every { subscription.nextMessage(Duration.ofSeconds(1)) } returns null

        val message = subscription.nextMessage(1.seconds)

        assertNull(message)
    }

    @Test
    fun `nextMessage returns received message`() {
        val received = mockk<Message>()
        every { subscription.nextMessage(Duration.ofSeconds(1)) } returns received

        val message = subscription.nextMessage(1.seconds)

        assertSame(received, message)
    }
}
