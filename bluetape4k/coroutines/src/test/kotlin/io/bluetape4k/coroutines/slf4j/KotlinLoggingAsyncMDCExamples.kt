package io.bluetape4k.coroutines.slf4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class KotlinLoggingAsyncMDCExamples {

    companion object: KLoggingChannel()

    @AfterEach
    fun cleanupMdc() {
        MDC.remove("traceId")
        MDC.remove("spanId")
    }

    @Test
    fun `withMDCConterxt in traceId`() = runTest {
        log.debug { "Before operation - no traceId" }
        withCoroutineLoggingContext(mapOf("traceId" to "100", "spanId" to "200")) {
            log.debug { "Inside with MDCContext" }
            withCoroutineLoggingContext(mapOf("traceId" to "200", "spanId" to "300")) {
                // MDC.put("traceId", "200")
                log.debug { "Nested with MDCContext" }
                MDC.get("traceId") shouldBeEqualTo "200"
                MDC.get("spanId") shouldBeEqualTo "300"
            }
            log.debug { "Inside with MDCContext" }
            MDC.get("traceId") shouldBeEqualTo "100"
            MDC.get("spanId") shouldBeEqualTo "200"
        }
        log.debug { "After operation - no traceId" }
        MDC.get("traceId").shouldBeNullOrEmpty()
        MDC.get("spanId").shouldBeNullOrEmpty()
    }

    @Test
    fun `nested MDCConterxt restore previous value`() = runTest {
        log.debug { "Before operation - no traceId" }
        withCoroutineLoggingContext(mapOf("traceId" to "outer", "spanId" to "123")) {
            log.debug { "Inside with MDCContext" }
            withCoroutineLoggingContext(mapOf("traceId" to "nested", "spanId" to "456")) {
                MDC.put("traceId", "nested")
                log.debug { "Nested with MDCContext" }
                MDC.get("traceId") shouldBeEqualTo "nested"
                MDC.get("spanId") shouldBeEqualTo "456"
            }
            log.debug { "Inside with MDCContext" }
            MDC.get("traceId") shouldBeEqualTo "outer"
            MDC.get("spanId") shouldBeEqualTo "123"
        }
        log.debug { "After operation - no traceId" }
        MDC.get("traceId").shouldBeNullOrEmpty()
        MDC.get("spanId").shouldBeNullOrEmpty()
    }

    @Test
    fun `pair overload는 단일 MDC 값을 설정하고 복원한다`() = runTest {
        withCoroutineLoggingContext("traceId" to "pair-1") {
            MDC.get("traceId") shouldBeEqualTo "pair-1"
        }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `vararg overload는 여러 MDC 값을 설정한다`() = runTest {
        withCoroutineLoggingContext("traceId" to "var-1", "spanId" to "var-2") {
            MDC.get("traceId") shouldBeEqualTo "var-1"
            MDC.get("spanId") shouldBeEqualTo "var-2"
        }
        MDC.get("traceId").shouldBeNullOrEmpty()
        MDC.get("spanId").shouldBeNullOrEmpty()
    }

    @Test
    fun `restorePrevious가 false이면 이전 값을 복원하지 않는다`() = runTest {
        MDC.put("traceId", "original")
        withCoroutineLoggingContext("traceId" to "nested", restorePrevious = false) {
            MDC.get("traceId") shouldBeEqualTo "nested"
        }
        // withContext(MDCContext())가 코루틴 시작 전 MDC를 복원하므로 "original" 이 유지됩니다.
        MDC.get("traceId") shouldBeEqualTo "original"
    }
}
