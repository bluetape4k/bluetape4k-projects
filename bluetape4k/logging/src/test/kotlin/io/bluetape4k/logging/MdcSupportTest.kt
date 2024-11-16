package io.bluetape4k.logging

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.junit.jupiter.api.Test
import org.slf4j.MDC

/**
 * logback log pattern 을 다음과 같이 `traceId=%X{traceId}` 를 추가해야 MDC `traceId` 가 로그애 출력됩니다.
 *
 * ```
 * %d{HH:mm:ss.SSS} %highlight(%-5level)[traceId=%X{traceId}][%.24thread] %logger{36}:%line: %msg%n%throwable
 * ```
 */
class MdcSupportTest {

    companion object: KLogging()

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
}
