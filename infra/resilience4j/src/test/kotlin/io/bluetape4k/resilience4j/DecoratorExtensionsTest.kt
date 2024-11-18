package io.bluetape4k.resilience4j

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.logging.KLogging
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class DecoratorsExtensionsTest {

    companion object: KLogging()

    private var state = false
    private val helloWorldService = mockk<HelloWorldService>(relaxUnitFun = true)

    @BeforeEach
    fun setup() {
        clearMocks(helloWorldService)
    }

    @Test
    fun `decorate completableFuture Fuction`() {

        every { helloWorldService.returnHelloWorldWithName("world") } returns "Hello world!"

        val circuitBreaker = CircuitBreaker.ofDefaults("deafults")

        val func = { name: String ->
            futureOf { helloWorldService.returnHelloWorldWithName(name) }
        }

        val decorated = decorateCompletableFutureFunction(func)
            .withRetry(Retry.ofDefaults("defaults"), Executors.newSingleThreadScheduledExecutor())
            .withCircuitBreaker(circuitBreaker)
            .withBulkhead(Bulkhead.ofDefaults("default"))
            .withRateLimiter(RateLimiter.ofDefaults("default"))
            .decorate()

        val result = decorated.invoke("world").join()
        result shouldBeEqualTo "Hello world!"

        with(circuitBreaker.metrics) {
            numberOfBufferedCalls shouldBeEqualTo 1
            numberOfSuccessfulCalls shouldBeEqualTo 1
            numberOfFailedCalls shouldBeEqualTo 0
        }

        verify(exactly = 1) { helloWorldService.returnHelloWorldWithName("world") }
        confirmVerified(helloWorldService)
    }
}
