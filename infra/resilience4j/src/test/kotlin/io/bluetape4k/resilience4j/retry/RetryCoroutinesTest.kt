package io.bluetape4k.resilience4j.retry

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.resilience4j.SuspendHelloWorldService
import io.github.resilience4j.kotlin.retry.decorateSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class RetryCoroutinesTest {

    companion object: KLoggingChannel()

    @Test
    fun `성공한 함수는 retry를 하지 않습니다`() = runSuspendTest {
        val retry = Retry.ofDefaults("testName")
        val metrics = retry.metrics
        val helloWorldService = SuspendHelloWorldService()

        val result = retry.executeSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        result shouldBeEqualTo "Hello world"
        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }

    @Test
    fun `예외가 발생하면 retry를 통해 재시도합니다`() = runSuspendTest {
        val retry = Retry.ofDefaults("testName")
        val metrics = retry.metrics
        val helloWorldService = SuspendHelloWorldService()

        val result = retry.executeSuspendFunction {
            when (helloWorldService.invocationCount) {
                0 -> helloWorldService.throwException()
                else -> helloWorldService.returnHelloWorld()
            }
        }

        result shouldBeEqualTo "Hello world"
        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 2
    }

    @Test
    fun `retryOnResult 를 기준으로 재시도를 수행합니다`() = runSuspendTest {
        val helloWorldService = SuspendHelloWorldService()
        val retry = Retry.of("testName") {
            RetryConfig.custom<Any?>()
                .waitDuration(Duration.ofMillis(10))
                .retryOnResult { helloWorldService.invocationCount < 2 }
                .build()
        }
        val metrics = retry.metrics

        val result = retry.executeSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        result shouldBeEqualTo "Hello world"
        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 2
    }

    @Test
    fun `반복된 예외 시에는 함수 실행이 실패한다`() = runSuspendTest {
        val helloWorldService = SuspendHelloWorldService()
        val retry = Retry.of("testName") {
            RetryConfig.custom<Any?>()
                .waitDuration(Duration.ofMillis(10))
                .build()
        }

        val metrics = retry.metrics

        assertFailsWith<IllegalStateException> {
            retry.executeSuspendFunction {
                helloWorldService.throwException()
            }
        }

        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 1

        helloWorldService.invocationCount shouldBeEqualTo retry.retryConfig.maxAttempts
    }

    @Test
    fun `decorate suspend function`() = runSuspendTest {
        val retry = Retry.ofDefaults("testName")
        val metrics = retry.metrics
        val helloWorldService = SuspendHelloWorldService()

        val function = retry.decorateSuspendFunction {
            helloWorldService.returnHelloWorld()
        }

        function() shouldBeEqualTo "Hello world"
        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 1
    }
}
