package io.bluetape4k.logging

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
class MdcSupportTest {

    companion object: KLogging()

    @AfterEach
    fun cleanupMdc() {
        MDC.remove("traceId")
        MDC.remove("spanId")
    }

    @Test
    fun `withMDCConterxt in traceId`() {
        log.debug { "Before operation - no traceId" }

        withLoggingContext("traceId" to 100, "spanId" to 200) {
            // MDC.put("traceId", "200")
            log.debug { "Inside with MDCContext" }

            withLoggingContext("traceId" to "200", "spanId" to 300) {
                // MDC.put("traceId", "200")
                log.debug { "Nested with MDCContext" }
                MDC.get("traceId") shouldBeEqualTo "200"
                MDC.get("spanId") shouldBeEqualTo "300"
            }

            MDC.get("traceId") shouldBeEqualTo "100"
            MDC.get("spanId") shouldBeEqualTo "200"
        }
        MDC.get("traceId").shouldBeNullOrEmpty()
        log.debug { "After operation - no traceId" }
    }

    @Test
    fun `nested MDCConterxt restore previous value`() {
        log.debug { "Before operation - no traceId" }
        withLoggingContext("traceId" to "outer", "spanId" to 123) {
            log.debug { "Inside with MDCContext" }

            withLoggingContext("traceId" to "nested", "spanId" to 456) {
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
    }

    @Test
    fun `exception이 발생해도 이전 MDC 값은 복원된다`() {
        MDC.put("traceId", "origin")

        assertFailsWith<IllegalStateException> {
            withLoggingContext("traceId" to "inner") {
                MDC.get("traceId") shouldBeEqualTo "inner"
                error("boom")
            }
        }

        MDC.get("traceId") shouldBeEqualTo "origin"
    }

    @Test
    fun `restorePrevious가 false면 key를 제거한다`() {
        MDC.put("traceId", "origin")

        withLoggingContext("traceId" to "inner", restorePrevious = false) {
            MDC.get("traceId") shouldBeEqualTo "inner"
        }

        MDC.get("traceId").shouldBeNullOrEmpty()
    }
}
