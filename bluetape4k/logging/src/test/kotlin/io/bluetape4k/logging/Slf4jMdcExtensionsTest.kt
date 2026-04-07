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
class Slf4jMdcExtensionsTest {
    companion object: KLogging()

    @Test
    fun `debug logging with mdc`() {
        log.debug { "Before operation - no traceId" }

        val traceId100 = mapOf("traceId" to 100, "spanId" to 200)

        log.debugMdc({ traceId100 }) { "Inside with MDCContext" }
        MDC.get("traceId").shouldBeNullOrEmpty()

        val traceId200 = mapOf("traceId" to 200, "spanId" to 300)
        log.debugMdc({ traceId200 }) { "Inside with MDCContext 2" }
        MDC.get("traceId").shouldBeNullOrEmpty()

        log.debug { "After operation - no traceId" }
    }

    @Test
    fun `info logging with mdc`() {
        log.debug { "Before operation - no traceId" }

        val traceId100 = mapOf("traceId" to 100, "spanId" to 200)

        log.infoMdc({ traceId100 }) { "Inside with MDCContext" }
        MDC.get("traceId").shouldBeNullOrEmpty()

        val traceId200 = mapOf("traceId" to 200, "spanId" to 300)
        log.infoMdc({ traceId200 }) { "Inside with MDCContext 2" }
        MDC.get("traceId").shouldBeNullOrEmpty()

        log.debug { "After operation - no traceId" }
    }

    @Test
    fun `error logging with mdc`() {
        log.debug { "Before operation - no traceId" }

        val traceId100 = mapOf("traceId" to 100, "spanId" to 200)
        val error100 = RuntimeException("error100")
        log.errorMdc({ traceId100 }, error100) { "Inside with MDCContext" }
        MDC.get("traceId").shouldBeNullOrEmpty()

        val traceId200 = mapOf("traceId" to 200, "spanId" to 300)
        val error200 = RuntimeException("error200")
        log.errorMdc({ traceId200 }, error200) { "Inside with MDCContext 2" }
        MDC.get("traceId").shouldBeNullOrEmpty()

        log.debug { "After operation - no traceId" }
    }

    @Test
    fun `trace logging with mdc`() {
        val mdc = mapOf("traceId" to "t-trace", "spanId" to "s-trace")
        log.traceMdc({ mdc }) { "trace with mdc" }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `trace logging with mdc and cause`() {
        val mdc = mapOf("traceId" to "t-trace")
        val cause = RuntimeException("trace-cause")
        log.traceMdc({ mdc }, cause) { "trace with mdc and cause" }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `debug logging with mdc and cause`() {
        val mdc = mapOf("traceId" to "t-debug")
        val cause = RuntimeException("debug-cause")
        log.debugMdc({ mdc }, cause) { "debug with mdc and cause" }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `info logging with mdc and cause`() {
        val mdc = mapOf("traceId" to "t-info")
        val cause = RuntimeException("info-cause")
        log.infoMdc({ mdc }, cause) { "info with mdc and cause" }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `warn logging with mdc`() {
        val mdc = mapOf("traceId" to "t-warn")
        log.warnMdc({ mdc }) { "warn with mdc" }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `warn logging with mdc and cause`() {
        val mdc = mapOf("traceId" to "t-warn")
        val cause = RuntimeException("warn-cause")
        log.warnMdc({ mdc }, cause) { "warn with mdc and cause" }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `error logging with mdc without cause`() {
        val mdc = mapOf("traceId" to "t-error")
        log.errorMdc({ mdc }) { "error with mdc only" }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }

    @Test
    fun `mdc는 블록 실행 후 정리된다`() {
        log.infoMdc({ mapOf("traceId" to "cleanup-test") }) {
            MDC.get("traceId") shouldBeEqualTo "cleanup-test"
            "inside block"
        }
        MDC.get("traceId").shouldBeNullOrEmpty()
    }
}
