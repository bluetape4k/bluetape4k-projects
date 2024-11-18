package io.bluetape4k.resilience4j.retry

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

class RetryExamples {

    companion object: KLogging()

    interface Service {
        fun sayHello(): String
    }

    private var successEvents = 0
    private var retryEvents = 0
    private var errorEvents = 0
    private var ignoredErrorEvents = 0

    val retry: Retry by lazy {
        Retry.ofDefaults("retry-${UUID.randomUUID()}").apply {
            eventPublisher
                .onSuccess {
                    log.info { "Success to execute. retry count=${it.numberOfRetryAttempts}" }
                }
                .onRetry {
                    log.info { "Retry to execute. event=${it.eventType}" }
                }
                .onError {
                    log.error(it.lastThrowable) { "Fail to execute in retry context" }
                }
        }
    }


    fun Retry.registerEventListener() {
        eventPublisher
            .onSuccess { successEvents++ }
            .onRetry { retryEvents++ }
            .onError { errorEvents++ }
            .onIgnoredError { ignoredErrorEvents++ }
    }

    @BeforeEach
    fun setup() {
        successEvents = 0
        retryEvents = 0
        errorEvents = 0
        ignoredErrorEvents = 0
    }

    @Test
    fun `configure retry`() {
        val config = RetryConfig.custom<Any?>()
            .maxAttempts(3)
            // .waitDuration(Duration.ofMillis(0))  // 이 것도 intervalFunction 이다
            .retryOnResult { result -> result == null }
            .retryOnException { error -> error is IOException }
            .retryExceptions(IOException::class.java, TimeoutException::class.java)
            .ignoreExceptions(ExecutionException::class.java, InterruptedException::class.java)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(100), 1.5))
            .build()

        config.maxAttempts shouldBeEqualTo 3
    }

    @Test
    fun `retry when exception`() {
        val service = mockk<Service>(relaxUnitFun = true)
        every { service.sayHello() } throws IOException("Boom!")

        val supplier = retry.checkedSupplier(service::sayHello)
        // val supplier = Decorators.ofSupplier { service.sayHello() }.withRetry(retry)

        // Kotlin Version
        val result = runCatching { supplier() }.recover { "Hello word from recovery execute" }

        val maxAttemps = retry.retryConfig.maxAttempts

        verify(exactly = maxAttemps) { service.sayHello() }
        confirmVerified(service)

        result.isSuccess.shouldBeTrue()
        result.getOrThrow() shouldBeEqualTo "Hello word from recovery execute"
    }
}
