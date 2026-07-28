package io.bluetape4k.ktor.observability

import io.bluetape4k.codec.Base58
import io.bluetape4k.support.requirePositiveNumber
import io.ktor.server.plugins.callid.CallIdConfig

/**
 * Ktor server call의 correlation ID를 다루는 유틸리티입니다.
 */
object KtorCorrelationId {

    const val DEFAULT_GENERATED_LENGTH: Int = 16
    const val DEFAULT_MAX_LENGTH: Int = 64

    private val allowedChars: Set<Char> =
        ('a'..'z').toSet() +
            ('A'..'Z').toSet() +
            ('0'..'9').toSet() +
            setOf('-', '_', '.')

    /**
     * 호출자가 전달한 correlation ID를 정제합니다.
     *
     * 값이 blank이거나 허용된 문자가 하나도 없으면 `null`을 반환합니다.
     */
    fun sanitize(rawValue: String?, maxLength: Int = DEFAULT_MAX_LENGTH): String? {
        maxLength.requirePositiveNumber("maxLength")
        val sanitized = rawValue
            ?.trim()
            ?.asSequence()
            ?.filter { it in allowedChars }
            ?.take(maxLength)
            ?.joinToString(separator = "")
            .orEmpty()

        return sanitized.takeIf { it.isNotBlank() }
    }

    /**
     * Base58 correlation ID를 생성합니다.
     */
    fun generate(length: Int = DEFAULT_GENERATED_LENGTH): String {
        length.requirePositiveNumber("length")
        return Base58.randomString(length)
    }

    fun isValid(value: String, maxLength: Int = DEFAULT_MAX_LENGTH): Boolean =
        value.length in 1..maxLength && value.all { it in allowedChars }
}

/**
 * bluetape4k sanitization과 response propagation을 적용해 Ktor CallId를 설정합니다.
 */
fun CallIdConfig.bluetape4kCorrelationIds(
    settings: CorrelationIdSettings = CorrelationIdSettings(),
) {
    retrieve { call ->
        KtorCorrelationId.sanitize(
            rawValue = call.request.headers[settings.requestHeaderName],
            maxLength = settings.maxLength
        )
    }
    generate {
        KtorCorrelationId.generate(settings.generatedLength)
    }
    verify { callId ->
        KtorCorrelationId.isValid(callId, settings.maxLength)
    }
    if (settings.propagateResponseHeader) {
        reply { call, callId ->
            call.response.headers.append(settings.responseHeaderName, callId)
        }
    }
}
