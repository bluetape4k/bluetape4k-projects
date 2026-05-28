package io.bluetape4k.ktor.observability

import io.bluetape4k.codec.Base58
import io.bluetape4k.support.requirePositiveNumber
import io.ktor.server.plugins.callid.CallIdConfig

/**
 * Correlation ID utilities for Ktor server calls.
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
     * Sanitizes a caller-supplied correlation ID.
     *
     * Returns `null` when the value is blank or has no allowed characters.
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
     * Generates a Base58 correlation ID.
     */
    fun generate(length: Int = DEFAULT_GENERATED_LENGTH): String {
        length.requirePositiveNumber("length")
        return Base58.randomString(length)
    }

    fun isValid(value: String, maxLength: Int = DEFAULT_MAX_LENGTH): Boolean =
        value.length in 1..maxLength && value.all { it in allowedChars }
}

/**
 * Configures Ktor CallId with bluetape4k sanitization and response propagation.
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
