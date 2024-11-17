package io.bluetape4k.coroutines.slf4j

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNullOrEmpty
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class KotlinLoggingAsyncMDCExamples {

    companion object: KLogging()

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
}
