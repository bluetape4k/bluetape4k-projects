package io.bluetape4k.resilience4j.retry

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.resilience4j.SuspendHelloWorldService
import io.github.resilience4j.kotlin.retry.retry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class FlowRetryTest {

    companion object: KLoggingChannel()

    @Test
    fun `성공하는 함수를 실행합니다`() = runSuspendTest {
        val retry = Retry.ofDefaults("testName")
        val metrics = retry.metrics
        val helloWorldService = SuspendHelloWorldService()

        val results = flow {
            repeat(3) {
                emit(helloWorldService.returnHelloWorld() + it)
            }
        }.retry(retry).toList()

        repeat(3) {
            results[it] shouldBeEqualTo "Hello world$it"
        }

        results.size shouldBeEqualTo 3
        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 3
    }

    @Test
    fun `함수 실행을 재시도 합니다`() = runSuspendTest {
        val retry = Retry.of("testName") {
            RetryConfig.custom<Any?>()
                .waitDuration(Duration.ofMillis(10))
                .build()
        }
        val metrics = retry.metrics
        val helloWorldService = SuspendHelloWorldService()
        // val results = mutableListOf<String>()

        val results = flow {
            repeat(3) {
                when (helloWorldService.invocationCount) {
                    0 -> helloWorldService.throwException()
                    else -> emit(helloWorldService.returnHelloWorld() + it)
                }
            }
        }.retry(retry).toList()

        repeat(3) {
            results[it] shouldBeEqualTo "Hello world$it"
        }

        results.size shouldBeEqualTo 3
        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 4
    }

    @Test
    fun `재시도 결과에 따라 실행합니다`() = runSuspendTest {
        val helloWorldService = SuspendHelloWorldService()
        val retry = Retry.of("testName") {
            RetryConfig.custom<Any?>()
                .waitDuration(Duration.ofMillis(10))
                .retryOnResult { helloWorldService.invocationCount < 2 }
                .build()
        }
        val metrics = retry.metrics
        // val results = mutableListOf<String>()

        val results = flow { emit(helloWorldService.returnHelloWorld()) }
            .retry(retry).toList()

        results.size shouldBeEqualTo 1
        results[0] shouldBeEqualTo "Hello world"

        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

        helloWorldService.invocationCount shouldBeEqualTo 2
    }

    @Test
    fun `반복적인 실패 시에도 함수는 실행됩니다`() = runSuspendTest {
        val helloWorldService = SuspendHelloWorldService()
        val retry = Retry.of("testName") {
            RetryConfig.custom<Any?>()
                .waitDuration(Duration.ofMillis(10))
                .build()
        }
        val metrics = retry.metrics
        val results = mutableListOf<String>()

        assertFailsWith<IllegalStateException> {
            flow<String> { helloWorldService.throwException() }.retry(retry).toList(results)
        }

        results.shouldBeEmpty()

        metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
        metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 1

        helloWorldService.invocationCount shouldBeEqualTo retry.retryConfig.maxAttempts
    }
}
