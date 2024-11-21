package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MDCContextExamples {

    companion object: KLogging()

    @Test
    fun `with mdc context`() = runSuspendTest {
        MDC.put("traceId", "100")
        log.debug { "Before operation" }
        MDC.get("traceId") shouldBeEqualTo "100"

        withContext(Dispatchers.IO + MDCContext()) {
            MDC.put("traceId", "200")
            log.debug { "Inside operation" }
            MDC.get("traceId") shouldBeEqualTo "200"
        }

        log.debug { "After operation" }
        MDC.get("traceId") shouldBeEqualTo "100"
    }
}
