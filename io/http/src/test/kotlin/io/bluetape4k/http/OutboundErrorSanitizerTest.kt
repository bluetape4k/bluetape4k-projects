package io.bluetape4k.http

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class OutboundErrorSanitizerTest {

    @Test
    fun `null blank and empty first line keep only status prefix`() {
        sanitizeOutboundError(503, null) shouldBeEqualTo "HTTP 503"
        sanitizeOutboundError(503, "   ") shouldBeEqualTo "HTTP 503"
        sanitizeOutboundError(503, "\nAuthorization: secret-token") shouldBeEqualTo "HTTP 503"
    }

    @Test
    fun `only the trimmed first line is retained`() {
        sanitizeOutboundError(599, "  timeout from service  \nAuthorization: secret-token") shouldBeEqualTo
            "HTTP 599 timeout from service"
    }

    @Test
    fun `credential markers and bearer variants are redacted`() {
        val message =
            "Authorization: Bearer authorization-secret Cookie= cookie-secret " +
                "Token:token-secret Secret : Bearer secret-value " +
                "API-Key=api-secret API_Key:other-secret API Key = third-secret"

        sanitizeOutboundError(503, message) shouldBeEqualTo
            "HTTP 503 Authorization:[redacted] Cookie:[redacted] Token:[redacted] " +
            "Secret:[redacted] API-Key:[redacted] API_Key:[redacted] API Key:[redacted]"
    }

    @Test
    fun `quoted escaped and json values are redacted`() {
        val message =
            "token=\"secret,with,comma\" cookie='secret\\'quote' " +
                "{\"Authorization\":\"json-secret\",\"Token\":\"second-secret\"}"

        sanitizeOutboundError(422, message) shouldBeEqualTo
            "HTTP 422 token:[redacted] cookie:[redacted] {\"Authorization:[redacted],\"Token:[redacted]}"
    }

    @Test
    fun `unquoted separators remain outside the redaction`() {
        sanitizeOutboundError(503, "token=secret-token, retrying") shouldBeEqualTo
            "HTTP 503 token:[redacted], retrying"
        sanitizeOutboundError(503, "token=secret-token; retrying") shouldBeEqualTo
            "HTTP 503 token:[redacted]; retrying"
        sanitizeOutboundError(503, "service token unavailable") shouldBeEqualTo
            "HTTP 503 service token unavailable"
    }

    @Test
    fun `malformed credential markers fail closed`() {
        val malformed = listOf(
            "Authorization:",
            "Authorization: Bearer",
            "token=\"\"",
            "token=''",
            "token=\"unterminated",
            "token=secret\\",
            "token=secret\\,raw-secret",
            "Authorization: : raw-secret",
            "Authorization: = raw-secret",
        )

        malformed.forEach { message ->
            val actual = sanitizeOutboundError(422, message)
            actual shouldBeEqualTo "HTTP 422"
            actual.contains("secret").shouldBeFalse()
        }
    }

    @Test
    fun `raw credential values never survive redaction`() {
        val rawSecret = "secret-token"
        val result = sanitizeOutboundError(503, "Authorization: $rawSecret temporary outage")

        result.contains(rawSecret).shouldBeFalse()
        result shouldBeEqualTo "HTTP 503 Authorization:[redacted] temporary outage"
    }

    @Test
    fun `output is capped at 240 UTF-16 code units without splitting surrogate pairs`() {
        val longResult = sanitizeOutboundError(599, "x".repeat(500))
        longResult.length shouldBeEqualTo 240
        longResult.startsWith("HTTP 599 ").shouldBeTrue()

        val surrogateBoundary = "a".repeat(230) + "😀"
        val unicodeResult = sanitizeOutboundError(599, surrogateBoundary)
        unicodeResult.length shouldBeEqualTo 239
        unicodeResult.endsWith("a").shouldBeTrue()
        unicodeResult.last().isHighSurrogate().shouldBeFalse()
        unicodeResult.startsWith("HTTP 599 ").shouldBeTrue()
    }
}
