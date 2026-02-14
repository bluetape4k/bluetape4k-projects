package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import kotlin.test.assertFailsWith

/**
 * logback log pattern 을 다음과 같이 `traceId=%X{traceId}` 를 추가해야 MDC `traceId` 가 로그애 출력됩니다.
 *
 * ```
 * %d{HH:mm:ss.SSS} %highlight(%-5level)[traceId=%X{traceId}][%.24thread] %logger{36}:%line: %msg%n%throwable
 * ```
 */
class MdcSupportCoroutinesTest {

    companion object: KLogging()

    @AfterEach
    fun cleanupMdc() {
        MDC.remove("traceId")
        MDC.remove("spanId")
    }

    @Test
    fun `withMDCConterxt in traceId`() = runTest {
        log.debug { "Before operation - no traceId" }
        withCoroutineLoggingContext("traceId" to 100, "spanId" to 200) {
            log.debug { "Inside with MDCContext" }
            withCoroutineLoggingContext("traceId" to "200", "spanId" to 300) {
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
        withCoroutineLoggingContext("traceId" to "outer", "spanId" to 123) {
            log.debug { "Inside with MDCContext" }
            withCoroutineLoggingContext("traceId" to "nested", "spanId" to 456) {
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
    fun `pair overload는 단일 key를 설정한다`() = runTest {
        withCoroutineLoggingContext("traceId" to "pair-100") {
            MDC.get("traceId") shouldBeEqualTo "pair-100"
        }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `restorePrevious가 false일 때 withContext 이전 값은 유지된다`() = runTest {
        MDC.put("traceId", "origin")

        withCoroutineLoggingContext("traceId" to "inner", restorePrevious = false) {
            MDC.get("traceId") shouldBeEqualTo "inner"
        }

        // MDCContext가 코루틴 진입 전 상태를 복원하므로 origin이 유지됩니다.
        MDC.get("traceId") shouldBeEqualTo "origin"
    }

    @Test
    fun `예외가 발생해도 코루틴 MDC는 복원된다`() = runTest {
        assertFailsWith<IllegalStateException> {
            withCoroutineLoggingContext("traceId" to "inner") {
                MDC.get("traceId") shouldBeEqualTo "inner"
                error("boom")
            }
        }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }
}
