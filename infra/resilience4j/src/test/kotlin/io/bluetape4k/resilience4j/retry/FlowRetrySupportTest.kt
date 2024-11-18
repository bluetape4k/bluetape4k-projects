package io.bluetape4k.resilience4j.retry

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class FlowRetrySupportTest {

    companion object: KLogging()

    private lateinit var retry: Retry

    @BeforeEach
    fun beforeEach() {
        retry = Retry.of("flow") {
            RetryConfig.custom<Any?>()
                .waitDuration(Duration.ofMillis(10))
                .maxAttempts(5)
                .build()
        }.apply {
            eventPublisher.onRetry {
                log.debug { "onRetry... $it" }
            }
        }
    }

    @Test
    fun `retry with map`() = runSuspendTest {
        var errorCount = 0
        listOf(0, 1, 2)
            .asFlow()
            .onEach { errorCount = 0 }
            .mapWithRetry(retry) {
                log.debug { "execute map($it)" }
                if (errorCount < 2) {
                    errorCount += 1
                    throw RuntimeException()
                } else {
                    log.info { "success($it)" }
                }
            }
            .toList()

        retry.verifyMetrics(0, 3, 0, 0)
    }

    @Test
    fun `retry with collect`() = runSuspendTest {
        var errorCount = 0
        listOf(0, 1, 2)
            .asFlow()
            .onEach { errorCount = 0 }
            .collectWithRetry(retry) {
                log.debug { "execute collect($it)" }
                if (errorCount < 2) {
                    errorCount++
                    throw RuntimeException()
                } else {
                    log.info { "success($it)" }
                }
            }

        retry.verifyMetrics(0, 3, 0, 0)
    }

    private fun Retry.verifyMetrics(
        successCallsWithoutRetryAttempt: Long = 0,
        successCallsWithRetryAttempt: Long = 0,
        failedCallWithoutRetryAttempt: Long = 0,
        failedCallWithRetryAttempt: Long = 0,
    ) {
        with(metrics) {
            numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo successCallsWithoutRetryAttempt
            numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo successCallsWithRetryAttempt
            numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo failedCallWithoutRetryAttempt
            numberOfFailedCallsWithRetryAttempt shouldBeEqualTo failedCallWithRetryAttempt
        }
    }
}
