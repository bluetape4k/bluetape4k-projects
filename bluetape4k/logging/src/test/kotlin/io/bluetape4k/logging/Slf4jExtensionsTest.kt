package io.bluetape4k.logging

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.Test
import org.slf4j.Marker
import org.slf4j.MarkerFactory

class Slf4jExtensionsTest {
    companion object : KLogging()

    private val error = RuntimeException("Boom!")

    @Test
    fun `logMessageSafe는 메시지를 안전하게 생성한다`() {
        val message = logMessageSafe { "hello" }
        message shouldBeEqualTo "hello"
    }

    @Test
    fun `logMessageSafe는 예외가 발생해도 fallback 메시지를 반환한다`() {
        val message =
            logMessageSafe(
                fallbackMessage = "fallback",
                msg = { error("boom") },
            )

        message.startsWith("fallback:").shouldBeTrue()
    }

    @Test
    fun `logMessageSafe는 null을 반환하면 문자열 null을 반환한다`() {
        val message = logMessageSafe { null }
        message shouldBeEqualTo "null"
    }

    @Test
    fun `logMessageSafe는 기본 fallback 문구를 사용한다`() {
        val message = logMessageSafe { error("boom") }
        message shouldStartWith LOG_FALLBACK_MSG
    }

    @Test
    fun `trace 레벨 로그 확장 함수가 동작한다`() {
        log.trace { "trace message" }
        log.trace(error) { "trace with error" }
        log.trace(null as Marker?, null as Throwable?) { "trace with marker and error" }
    }

    @Test
    fun `debug 레벨 로그 확장 함수가 동작한다`() {
        log.debug { "debug message" }
        log.debug(error) { "debug with error" }
        log.debug(null as Marker?, null as Throwable?) { "debug with marker and error" }
    }

    @Test
    fun `info 레벨 로그 확장 함수가 동작한다`() {
        log.info { "info message" }
        log.info(error) { "info with error" }
        log.info(null as Marker?, null as Throwable?) { "info with marker and error" }
    }

    @Test
    fun `warn 레벨 로그 확장 함수가 WARN_ERROR_PREFIX를 붙인다`() {
        log.warn { "warn message" }
        log.warn(error) { "warn with error" }
        log.warn(null as Marker?, null as Throwable?) { "warn with marker and error" }
    }

    @Test
    fun `error 레벨 로그 확장 함수가 WARN_ERROR_PREFIX를 붙인다`() {
        log.error { "error message" }
        log.error(error) { "error with error" }
        log.error(null as Marker?, null as Throwable?) { "error with marker and error" }
    }

    @Test
    fun `marker 오버로드 확장 함수가 동작한다`() {
        val marker = MarkerFactory.getMarker("TEST")
        log.trace(marker, error) { "trace with marker" }
        log.debug(marker, error) { "debug with marker" }
        log.info(marker, error) { "info with marker" }
        log.warn(marker, error) { "warn with marker" }
        log.error(marker, error) { "error with marker" }
    }
}
