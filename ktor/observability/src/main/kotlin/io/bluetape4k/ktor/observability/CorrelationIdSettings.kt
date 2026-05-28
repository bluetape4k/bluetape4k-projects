package io.bluetape4k.ktor.observability

import io.bluetape4k.support.requireInRange
import io.bluetape4k.support.requireNotBlank
import io.ktor.http.HttpHeaders
import java.io.Serializable

/**
 * Correlation ID policy used by CallId and CallLogging helpers.
 *
 * ## Contract
 * - Incoming header values are sanitized and capped before use.
 * - Generated values use bluetape4k Base58 random strings.
 * - Response propagation uses the sanitized/generated value, never the raw header.
 */
data class CorrelationIdSettings(
    val requestHeaderName: String = HttpHeaders.XRequestId,
    val responseHeaderName: String = requestHeaderName,
    val mdcKey: String = "correlation-id",
    val generatedLength: Int = KtorCorrelationId.DEFAULT_GENERATED_LENGTH,
    val maxLength: Int = KtorCorrelationId.DEFAULT_MAX_LENGTH,
    val propagateResponseHeader: Boolean = true,
): Serializable {

    init {
        requestHeaderName.requireNotBlank("requestHeaderName")
        responseHeaderName.requireNotBlank("responseHeaderName")
        mdcKey.requireNotBlank("mdcKey")
        generatedLength.requireInRange(8, 128, "generatedLength")
        maxLength.requireInRange(8, 256, "maxLength")
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
