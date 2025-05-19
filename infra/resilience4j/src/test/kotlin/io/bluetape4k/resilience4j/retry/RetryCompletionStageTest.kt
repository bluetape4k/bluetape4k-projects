package io.bluetape4k.resilience4j.retry

import io.bluetape4k.concurrent.failedCompletableFutureOf
import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.resilience4j.AsyncHelloWorldService
import io.bluetape4k.resilience4j.HelloWorldException
import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import kotlin.test.assertFailsWith

class RetryCompletionStageTest {

    companion object: KLoggingChannel()

    private val helloWorldService: AsyncHelloWorldService = mockk(relaxUnitFun = true)
    private val scheduler = Executors.newScheduledThreadPool(8) // Executors.newSingleThreadScheduledExecutor()

    @BeforeEach
    fun setup() {
        clearMocks(helloWorldService)
    }

    @Test
    fun `should not retry`() {
        every { helloWorldService.returnHelloWorld() } returns futureOf { "Hello world" }

        val retry = Retry.ofDefaults("id")

        val supplier = Decorators
            .ofCompletionStage { helloWorldService.returnHelloWorld() }
            .withRetry(retry, scheduler)
            .get()

        val result = supplier.toCompletableFuture().get()

        result shouldBeEqualTo "Hello world"
        verify(exactly = 1) { helloWorldService.returnHelloWorld() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `should not retry when return Void`() {

        every { helloWorldService.sayHelloWorld() } returns futureOf { null }

        val retry = Retry.ofDefaults("id")
        val supplier = Decorators
            .ofCompletionStage { helloWorldService.sayHelloWorld() }
            .withRetry(retry, scheduler)
            .get()

        supplier.toCompletableFuture().get()
        verify(exactly = 1) { helloWorldService.sayHelloWorld() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `should not retry with that result`() {
        every { helloWorldService.returnHelloWorld() } returns futureOf { "Hello world" }

        val retryConfig = RetryConfig.custom<String>()
            .retryOnResult { it.contains("NoRetry") }
            .maxAttempts(1)
            .build()

        val retry = Retry.of("id", retryConfig)

        val supplier = Decorators
            .ofCompletionStage { helloWorldService.returnHelloWorld() }
            .withRetry(retry, scheduler)
            .get()

        val result = supplier.toCompletableFuture().get()

        result shouldBeEqualTo "Hello world"
        verify(exactly = 1) { helloWorldService.returnHelloWorld() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `should Retry in case of result retry match at sync stage`() {
        retryWithAttemptsAndRetryOnResult(1, "Hello world")
    }

    @Test
    fun `should Retry two attempts in case of result retry match at sync stage`() {
        retryWithAttemptsAndRetryOnResult(2, "Hello world")
    }

    @Test
    fun `should re-throw exception in case of exception at sync stage`() {
        every { helloWorldService.returnHelloWorld() } throws IllegalArgumentException("BAM!")

        val retry = Retry.ofDefaults("id")

        assertFailsWith<IllegalArgumentException> {
            retry.executeCompletionStage(scheduler) {
                helloWorldService.returnHelloWorld()
            }
        }
    }

    @Test
    fun `should retry in case of an exception at async stage`() {
        val failedFuture = failedCompletableFutureOf<String>(HelloWorldException())

        every { helloWorldService.returnHelloWorld() } returns failedFuture andThen futureOf { "Hello world" }

        val retry = Retry.ofDefaults("id")

        val supplier = Retry.decorateCompletionStage(retry, scheduler) {
            helloWorldService.returnHelloWorld()
        }

        val result = supplier.get().toCompletableFuture().get()

        result shouldBeEqualTo "Hello world"
        verify(exactly = 2) { helloWorldService.returnHelloWorld() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `should CompleteFuture after one attempt in case of Exception At AsyncStage`() {
        retryWithAttemptsWithException(1)
    }

    @Test
    fun `should CompleteFuture after two attempt in case of Exception At AsyncStage`() {
        retryWithAttemptsWithException(2)
    }

    @Test
    fun `should CompleteFuture after three attempt in case of Exception At AsyncStage`() {
        retryWithAttemptsWithException(3)
    }

    private fun retryWithAttemptsWithException(noOfAttempts: Int) {

        val failedFuture = failedCompletableFutureOf<String>(HelloWorldException())

        every { helloWorldService.returnHelloWorld() } returns failedFuture

        val retryConfig = RetryConfig.custom<String>()
            .maxAttempts(noOfAttempts)
            .build()
        val retry = Retry.of("id", retryConfig)

        val supplier = Decorators
            .ofCompletionStage { helloWorldService.returnHelloWorld() }
            .withRetry(retry, scheduler)

        val result = runCatching { supplier.get().toCompletableFuture().get() }
        verify(exactly = noOfAttempts) { helloWorldService.returnHelloWorld() }
        result.isFailure.shouldBeTrue()
        result.exceptionOrNull()?.cause shouldBeInstanceOf HelloWorldException::class
    }

    private fun retryWithAttemptsAndRetryOnResult(noOfAttempts: Int, retryResponse: String) {
        every { helloWorldService.returnHelloWorld() } returns futureOf { "Hello world" }

        val retryConfig = RetryConfig.custom<String>()
            .maxAttempts(noOfAttempts)
            .retryOnResult { s -> s.contains(retryResponse) }
            .build()
        val retry = Retry.of("id", retryConfig)

        val supplier = Decorators
            .ofCompletionStage { helloWorldService.returnHelloWorld() }
            .withRetry(retry, scheduler)

        val result = runCatching { supplier.get().toCompletableFuture().get() }

        verify(exactly = noOfAttempts) { helloWorldService.returnHelloWorld() }
        result.isSuccess.shouldBeTrue()
    }
}
