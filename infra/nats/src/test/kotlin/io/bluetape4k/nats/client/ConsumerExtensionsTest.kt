package io.bluetape4k.nats.client

import io.mockk.every
import io.mockk.mockk
import io.nats.client.Consumer
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

class ConsumerExtensionsTest {

    private lateinit var consumer: Consumer

    @BeforeEach
    fun setUp() {
        consumer = mockk<Consumer>()
    }

    @Test
    fun `drain with positive millis calls underlying drain with java Duration`() {
        every { consumer.drain(any<java.time.Duration>()) } returns CompletableFuture.completedFuture(true)

        val future = consumer.drain(500L)

        future.shouldNotBeNull()
        future.get().shouldBeTrue()
    }

    @Test
    fun `drain with zero millis is allowed`() {
        every { consumer.drain(any<java.time.Duration>()) } returns CompletableFuture.completedFuture(true)

        val future = consumer.drain(0L)

        future.shouldNotBeNull()
    }

    @Test
    fun `drain with negative millis throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            consumer.drain(-1L)
        }
    }

    @Test
    fun `drain with kotlin Duration ZERO is allowed`() {
        every { consumer.drain(any<java.time.Duration>()) } returns CompletableFuture.completedFuture(false)

        val future = consumer.drain(ZERO)

        future.shouldNotBeNull()
        future.get().shouldBeFalse()
    }

    @Test
    fun `drain with positive kotlin Duration calls underlying drain`() {
        every { consumer.drain(any<java.time.Duration>()) } returns CompletableFuture.completedFuture(true)

        val future = consumer.drain(1.seconds)

        future.shouldNotBeNull()
        future.get().shouldBeTrue()
    }

    @Test
    fun `drainSuspending with millis awaits future result`() = runTest {
        every { consumer.drain(any<java.time.Duration>()) } returns CompletableFuture.completedFuture(true)

        val result = consumer.drainSuspending(100L)

        result.shouldBeTrue()
    }

    @Test
    fun `drainSuspending with kotlin Duration awaits future result`() = runTest {
        every { consumer.drain(any<java.time.Duration>()) } returns CompletableFuture.completedFuture(false)

        val result = consumer.drainSuspending(1.seconds)

        result.shouldBeFalse()
    }

    @Test
    fun `drainSuspending with java Duration awaits future result`() = runTest {
        every { consumer.drain(any<java.time.Duration>()) } returns CompletableFuture.completedFuture(true)

        val result = consumer.drainSuspending(java.time.Duration.ofSeconds(1))

        result.shouldBeTrue()
    }
}
