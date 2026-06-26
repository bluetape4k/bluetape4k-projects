package io.bluetape4k.http.okhttp3

import okhttp3.Headers
import java.util.Locale

/** Redacted marker used for sensitive HTTP header values in diagnostic logs. */
const val REDACTED_HTTP_HEADER_VALUE: String = "<redacted>"

private val DEFAULT_SENSITIVE_HEADER_NAMES = setOf(
    "authorization",
    "proxy-authorization",
    "cookie",
    "set-cookie",
    "x-api-key",
    "api-key",
)

/**
 * Returns true when [headerName] should have its value redacted in logs.
 *
 * ## Contract
 * - Matches well-known credential headers such as `Authorization`, `Cookie`, and `Set-Cookie`.
 * - Matches API-key and token-like header names by normalized fragments.
 * - [additionalSensitiveHeaderNames] lets callers extend the policy for project-specific headers.
 */
fun isSensitiveHttpHeaderName(
    headerName: String,
    additionalSensitiveHeaderNames: Set<String> = emptySet(),
): Boolean {
    val normalized = headerName.normalizedHeaderName()
    return normalized in DEFAULT_SENSITIVE_HEADER_NAMES ||
            normalized in additionalSensitiveHeaderNames.mapTo(HashSet()) { it.normalizedHeaderName() } ||
            "api-key" in normalized ||
            "apikey" in normalized ||
            "token" in normalized
}

/**
 * Returns [REDACTED_HTTP_HEADER_VALUE] for sensitive headers and the original [headerValue] otherwise.
 */
fun redactHttpHeaderValue(
    headerName: String,
    headerValue: String,
    additionalSensitiveHeaderNames: Set<String> = emptySet(),
): String =
    if (isSensitiveHttpHeaderName(headerName, additionalSensitiveHeaderNames)) {
        REDACTED_HTTP_HEADER_VALUE
    } else {
        headerValue
    }

/**
 * Formats OkHttp [Headers] for logs while redacting sensitive values.
 */
fun Headers.toRedactedString(additionalSensitiveHeaderNames: Set<String> = emptySet()): String =
    joinToString(separator = "") { (name, value) ->
        "$name: ${redactHttpHeaderValue(name, value, additionalSensitiveHeaderNames)}\n"
    }

private fun String.normalizedHeaderName(): String =
    trim().lowercase(Locale.ROOT)
