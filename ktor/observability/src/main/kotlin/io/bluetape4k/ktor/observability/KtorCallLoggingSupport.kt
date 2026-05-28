package io.bluetape4k.ktor.observability

import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLoggingConfig
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path

/**
 * Configures Ktor CallLogging with sanitized correlation ID MDC.
 *
 * ## Contract
 * - Caller headers are not logged directly.
 * - Query strings are not included in the default log message.
 * - Common health/metrics scrape paths can be filtered out.
 */
fun CallLoggingConfig.bluetape4kCallLogging(
    settings: CallLoggingSettings = CallLoggingSettings(),
) {
    level = settings.level
    disableDefaultColors()
    callIdMdc(settings.correlationId.mdcKey)
    filter { call ->
        call.request.path() !in settings.excludedPaths
    }
    format { call ->
        val status = call.response.status()?.value ?: "unhandled"
        val callId = call.callId ?: "-"
        "${call.request.httpMethod.value} ${call.request.path()} status=$status elapsed=${call.processingTimeMillis()}ms correlationId=$callId"
    }
}
