package io.bluetape4k.resilience4j

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.bluetape4k.resilience4j.cache.Cache2kJCacheProvider
import io.bluetape4k.resilience4j.cache.CoCache
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDateTime

class CoDecoratorsTest {

    interface Service {
        suspend fun run()
        suspend fun supply(): String
        suspend fun consume(input: String)
        suspend fun execute(input: String): LocalDateTime
        suspend fun bifunction(a: Int, b: Int): Int
    }

    companion object: KLoggingChannel()

    val retry = Retry.ofDefaults("coDecorator")
    val circuitBreaker = CircuitBreaker.ofDefaults("coDecorator")
    val rateLimiter = RateLimiter.ofDefaults("coDecorator")

    private val service = mockk<CoDecoratorsTest.Service>(relaxUnitFun = true)

    @BeforeAll
    fun beforeAll() {
        retry.eventPublisher
            .onSuccess {
                log.info { "Success to execute. retry count=${it.numberOfRetryAttempts}" }
            }
            .onRetry {
                log.info { "Retry to execute. event=$it" }
            }
            .onError {
                log.error(it.lastThrowable) { "Fail to execute in retry context" }
            }

        circuitBreaker.eventPublisher
            .onStateTransition {
                val oldState = it.stateTransition.fromState
                val newState = it.stateTransition.toState
                log.info { "CircuitBreaker state is changed. old=$oldState -> new=$newState" }
            }
            .onError {
                log.error(it.throwable) { "Fail to execute in circuitBreaker context" }
            }
    }

    @BeforeEach
    fun setup() {
        log.info { "Clear mocks ..." }
        clearMocks(service)
    }

    @AfterEach
    fun cleanup() {
        clearMocks(service)
    }

    @Nested
    inner class CoDecoratorsForRunnableTest {

        @Test
        fun `성공하는 runnable에 대한 retry decoration 하기`() = runSuspendTest {
            // relaxUnitFun = true 를 지정하면 이 작업은 필요없다
            // coEvery { service.run() } returns Unit

            val result = runCatching {
                CoDecorators.ofRunnable { service.run() }
                    .withRetry(retry)
                    .invoke()
            }

            result.isSuccess.shouldBeTrue()
            coVerify(exactly = 1) { service.run() }
            confirmVerified(service)
        }

        @Test
        fun `성공하는 runnable에 대해 retry 와 circuit breaker를 decoration 하기`() = runSuspendTest {
            // relaxUnitFun = true 를 지정하면 이 작업은 필요없다
            coEvery { service.run() } returns Unit

            val decorated = CoDecorators.ofRunnable { service.run() }
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .withRateLimit(rateLimiter)
                .decoreate()

            val result = runCatching { decorated() }

            result.isSuccess.shouldBeTrue()
            coVerify(exactly = 1) { service.run() }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 runnable에 대한 retry decoration하기`() = runSuspendTest {
            coEvery { service.run() } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofRunnable(service::run)
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated() }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class

            coVerify(exactly = retry.retryConfig.maxAttempts) { service.run() }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 runnable에 대한 retry와 circuit breaker를 decoration하기`() = runSuspendTest {
            coEvery { service.run() } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofRunnable(service::run)
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated() }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.run() }
            confirmVerified(service)
        }
    }

    @Nested
    inner class CoDecoratorsForSupplierTest {

        val expected = "Hello world!"

        @Test
        fun `성공하는 supplier 대한 retry decoration 하기`() = runSuspendTest {
            coEvery { service.supply() } coAnswers { expected }

            val decorated = CoDecorators
                .ofSupplier { service.supply() }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo expected
            coVerify(exactly = 1) { service.supply() }
            confirmVerified(service)
        }

        @Test
        fun `성공하는 supplier 대해 retry 와 circuit breaker를 decoration 하기`() = runSuspendTest {
            coEvery { service.supply() } coAnswers { expected }

            val decorated = CoDecorators
                .ofSupplier { service.supply() }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo expected
            coVerify(exactly = 1) { service.supply() }
            confirmVerified(service)
        }

        @Test
        fun `성공하는 supplier 대해 fallback decoration 하기`() = runSuspendTest {
            coEvery { service.supply() } coAnswers { expected }

            val decorated = CoDecorators
                .ofSupplier { service.supply() }
                .withFallback({ it == expected }, { "fallback" })
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo "fallback"
            coVerify(exactly = 1) { service.supply() }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 supplier 대한 retry decoration하기`() = runSuspendTest {
            coEvery { service.supply() } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofSupplier { service.supply() }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated() }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.supply() }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 supplier 대한 retry와 circuit breaker를 decoration하기`() = runSuspendTest {
            coEvery { service.supply() } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofSupplier { service.supply() }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated() }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.supply() }
            confirmVerified(service)
        }
    }

    @Nested
    inner class CoDecoratorsForConsumerTest {

        val input = "Hello world!"

        @Test
        fun `성공하는 consumer 대한 retry decoration 하기`() = runSuspendTest {
            coEvery { service.consume(input) } returns Unit

            val decorated = CoDecorators
                .ofRunnable { service.consume(input) }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated() }

            result.isSuccess.shouldBeTrue()
            coVerify(exactly = 1) { service.consume(input) }
            confirmVerified(service)
        }

        @Test
        fun `성공하는 consumer 대해 retry 와 circuit breaker를 decoration 하기`() = runSuspendTest {
            coEvery { service.consume(input) } returns Unit

            val decorated = CoDecorators
                .ofRunnable { service.consume(input) }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated() }

            result.isSuccess.shouldBeTrue()
            coVerify(exactly = 1) { service.consume(input) }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 consumer 대한 retry decoration하기`() = runSuspendTest {
            coEvery { service.consume(input) } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofRunnable { service.consume(input) }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated() }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.consume(input) }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 consumer 대한 retry와 circuit breaker를 decoration하기`() = runSuspendTest {
            coEvery { service.consume(input) } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofRunnable { service.consume(input) }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated() }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.consume(input) }
            confirmVerified(service)
        }
    }

    @Nested
    inner class CoDecoratorsForFunction1Test {

        val input = "Hello world!"
        val output = LocalDateTime.now()

        @Test
        fun `성공하는 function 대한 retry decoration 하기`() = runSuspendTest {
            coEvery { service.execute(input) } returns output

            val decorated = CoDecorators
                .ofFunction1 { s: String -> service.execute(s) }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated(input) }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo output
            coVerify(exactly = 1) { service.execute(input) }
            confirmVerified(service)
        }

        @Test
        fun `성공하는 function 대해 retry 와 circuit breaker를 decoration 하기`() = runSuspendTest {
            coEvery { service.execute(input) } returns output

            val decorated = CoDecorators
                .ofFunction1 { s: String -> service.execute(s) }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated(input) }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo output
            coVerify(exactly = 1) { service.execute(input) }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 function 대한 retry decoration하기`() = runSuspendTest {
            coEvery { service.execute(input) } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofFunction1 { s: String -> service.execute(s) }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated(input) }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.execute(input) }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 function 대한 retry와 circuit breaker를 decoration하기`() = runSuspendTest {
            coEvery { service.execute(input) } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofFunction1 { s: String -> service.execute(s) }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated(input) }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.execute(input) }
            confirmVerified(service)
        }

        @Test
        fun `성공하는 function 에 대해 CoroutinesCache decorate하기`() = runSuspendTest {
            coEvery { service.execute(input) } returns output

            val jcache = Cache2kJCacheProvider.getJCache<String, LocalDateTime>("CoDecorators")
            jcache.clear()
            val coroutinesCache = CoCache.of(jcache)

            val decorated = CoDecorators
                .ofFunction1 { s: String -> service.execute(s) }
                .withCoroutinesCache(coroutinesCache)
                .decoreate()


            val result = runCatching { decorated.invoke(input) }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo output

            val result2 = runCatching { decorated.invoke(input) }

            result2.isSuccess.shouldBeTrue()
            result2.getOrNull() shouldBeEqualTo output

            coroutinesCache.metrics.getNumberOfCacheMisses() shouldBeEqualTo 1
            coroutinesCache.metrics.getNumberOfCacheHits() shouldBeEqualTo 1

            // Cache 된 값을 가져 오기 때문에 한번만 수행합니다.
            coVerify(exactly = 1) { service.execute(input) }
            confirmVerified(service)
        }
    }

    @Nested
    inner class CoDecoratorsForBiFunctionTest {

        val input1 = 21
        val input2 = 23
        val output = 44

        @Test
        fun `성공하는 bi-function 대한 retry decoration 하기`() = runSuspendTest {
            coEvery { service.bifunction(input1, input2) } returns output

            val decorated = CoDecorators
                .ofFunction2 { a: Int, b: Int -> service.bifunction(a, b) }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated(input1, input2) }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo output
            coVerify(exactly = 1) { service.bifunction(input1, input2) }
            confirmVerified(service)
        }

        @Test
        fun `성공하는 bi-function 대해 retry 와 circuit breaker를 decoration 하기`() = runSuspendTest {
            coEvery { service.bifunction(input1, input2) } returns output

            val decorated = CoDecorators
                .ofFunction2 { a: Int, b: Int -> service.bifunction(a, b) }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated(input1, input2) }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo output
            coVerify(exactly = 1) { service.bifunction(input1, input2) }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 bi-function 대한 retry decoration하기`() = runSuspendTest {
            coEvery { service.bifunction(input1, input2) } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofFunction2 { a: Int, b: Int -> service.bifunction(a, b) }
                .withRetry(retry)
                .decoreate()

            val result = runCatching { decorated(input1, input2) }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.bifunction(input1, input2) }
            confirmVerified(service)
        }

        @Test
        fun `예외를 발생하는 bi-function 대한 retry와 circuit breaker를 decoration하기`() = runSuspendTest {
            coEvery { service.bifunction(input1, input2) } throws IOException("BAM!")

            val decorated = CoDecorators
                .ofFunction2 { a: Int, b: Int -> service.bifunction(a, b) }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decoreate()

            val result = runCatching { decorated(input1, input2) }

            result.isFailure.shouldBeTrue()
            result.exceptionOrNull() shouldBeInstanceOf IOException::class
            coVerify(exactly = retry.retryConfig.maxAttempts) { service.bifunction(input1, input2) }
            confirmVerified(service)
        }
    }
}
