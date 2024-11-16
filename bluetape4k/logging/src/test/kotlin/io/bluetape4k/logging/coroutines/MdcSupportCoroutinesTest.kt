package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
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
class MdcSupportCoroutinesTest {

    companion object: KLogging()

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
}
