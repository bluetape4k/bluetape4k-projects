package io.bluetape4k.http

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OutboundHttpErrorSanitizerTest {

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\t"])
    fun `null 또는 blank 메시지는 HTTP status만 반환한다`(rawMessage: String) {
        sanitizeOutboundHttpError(503, rawMessage) shouldBeEqualTo "HTTP 503"
        sanitizeOutboundHttpError(503, null) shouldBeEqualTo "HTTP 503"
    }

    @Test
    fun `credential 변형을 모두 redacted marker로 치환한다`() {
        val rawMessage =
            "Authorization : Bearer auth-secret, Cookie=cookie-secret; " +
                "Token = token-secret, Secret: secret-value, API-Key = api-key-value"

        val sanitized = sanitizeOutboundHttpError(502, rawMessage)

        sanitized shouldBeEqualTo "HTTP 502 Authorization:[redacted]"
        listOf("auth-secret", "cookie-secret", "token-secret", "secret-value", "api-key-value")
            .forEach(sanitized::shouldNotContain)
    }

    @Test
    fun `credential 이름은 대소문자를 구분하지 않고 api key 변형을 허용한다`() {
        val rawMessage = "authorization=Bearer lower-secret, API_key: underscore-secret, api key=space-secret"

        sanitizeOutboundHttpError(401, rawMessage) shouldBeEqualTo
            "HTTP 401 authorization:[redacted]"
    }

    @Test
    fun `Basic authorization scheme도 credential 전체를 제거한다`() {
        val rawCredential = listOf("dXNlcj", "pwYXNzd29yZA==").joinToString("")

        val sanitized = sanitizeOutboundHttpError(401, "Authorization: Basic $rawCredential")

        sanitized shouldBeEqualTo "HTTP 401 Authorization:[redacted]"
        sanitized shouldNotContain rawCredential
    }

    @Test
    fun `quoted authorization 값의 공백과 credential을 함께 제거한다`() {
        val rawCredential = "quoted secret value"

        val sanitized = sanitizeOutboundHttpError(401, "Authorization: \"$rawCredential\"")

        sanitized shouldBeEqualTo "HTTP 401 Authorization:[redacted]"
        sanitized shouldNotContain rawCredential
    }

    @Test
    fun `Cookie header의 여러 credential을 fail closed로 제거한다`() {
        val rawMessage = "Cookie: session=first-secret; csrf=second-secret"

        val sanitized = sanitizeOutboundHttpError(403, rawMessage)

        sanitized shouldBeEqualTo "HTTP 403 Cookie:[redacted]"
        sanitized shouldNotContain "first-secret"
        sanitized shouldNotContain "second-secret"
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "Authorization: \"Bearer leaked-secret",
            "Token: two word-secret",
        ],
    )
    fun `malformed 또는 공백 포함 credential은 label 뒤의 나머지를 제거한다`(rawMessage: String) {
        val sanitized = sanitizeOutboundHttpError(401, rawMessage)

        sanitized shouldBeEqualTo "HTTP 401 ${rawMessage.substringBefore(':')}:[redacted]"
        sanitized shouldNotContain "leaked-secret"
        sanitized shouldNotContain "word-secret"
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{\"authorization\":\"json-secret\"}",
            "\"Authorization\": \"quoted-label-secret\"",
            "'Token': 'single-quoted-secret'",
            "Authorization delimiter-less-secret",
        ],
    )
    fun `quoted label과 delimiter 없는 credential도 fail closed로 제거한다`(rawMessage: String) {
        val sanitized = sanitizeOutboundHttpError(401, rawMessage)

        listOf("json-secret", "quoted-label-secret", "single-quoted-secret", "delimiter-less-secret")
            .forEach(sanitized::shouldNotContain)
    }

    @Test
    fun `여러 줄 메시지는 첫 줄만 보존한다`() {
        val sanitized = sanitizeOutboundHttpError(
            504,
            "upstream timeout\nAuthorization: Bearer hidden-secret\nstack trace",
        )

        sanitized shouldBeEqualTo "HTTP 504 upstream timeout"
        sanitized shouldNotContain "hidden-secret"
    }

    @ParameterizedTest
    @ValueSource(strings = ["\u000B", "\u000C", "\u0085", "\u2028", "\u2029"])
    fun `Unicode line separator도 credential redaction을 우회하지 못한다`(separator: String) {
        val rawCredential = "TOPSECRET123"

        val sanitized = sanitizeOutboundHttpError(
            401,
            "Authorization: Bearer $rawCredential${separator}stack",
        )

        sanitized shouldBeEqualTo "HTTP 401 Authorization:[redacted]"
        sanitized shouldNotContain rawCredential
        sanitized shouldNotContain "stack"
    }

    @Test
    fun `정제 결과는 status prefix를 포함해 240자로 제한한다`() {
        val sanitized = sanitizeOutboundHttpError(599, "x".repeat(500))

        sanitized.length shouldBeEqualTo OUTBOUND_HTTP_ERROR_MAX_LENGTH
        sanitized.take(9) shouldBeEqualTo "HTTP 599 "
    }

    @Test
    fun `malformed credential 입력도 예외 없이 정제한다`() {
        sanitizeOutboundHttpError(500, "Authorization:= malformed-secret") shouldBeEqualTo
            "HTTP 500 Authorization:[redacted]"
    }

    @Test
    fun `Int 범위의 합성 status도 prefix와 전체 길이 계약을 유지한다`() {
        val sanitized = sanitizeOutboundHttpError(Int.MIN_VALUE, "x".repeat(500))

        sanitized.take(17) shouldBeEqualTo "HTTP -2147483648 "
        sanitized.length shouldBeEqualTo OUTBOUND_HTTP_ERROR_MAX_LENGTH
    }
}
