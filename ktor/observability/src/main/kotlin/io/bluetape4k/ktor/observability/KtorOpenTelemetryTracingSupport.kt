package io.bluetape4k.ktor.observability

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.ApplicationRequest
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry

/**
 * Installs opt-in OpenTelemetry server tracing for Ktor.
 *
 * ## Contract
 * - Delegates HTTP server span creation to OpenTelemetry's Ktor 3 instrumentation.
 * - Uses the caller-provided [KtorOpenTelemetryTracingConfig.openTelemetry] instance.
 * - Records `correlation.present` by default before the span ends.
 * - Records `correlation.id` only after CallId sanitization/generation and explicit opt-in.
 */
fun Application.installBluetape4kKtorOpenTelemetryTracing(
    config: KtorOpenTelemetryTracingConfig,
) {
    install(KtorServerTelemetry) {
        setOpenTelemetry(config.openTelemetry)
        attributesExtractor {
            onEnd {
                val correlationId = request.sanitizedCorrelationId(config.correlationId)
                attributes.put(CORRELATION_PRESENT_ATTRIBUTE, correlationId != null)
                if (config.captureSanitizedCorrelationId && correlationId != null) {
                    attributes.put(CORRELATION_ID_ATTRIBUTE, correlationId)
                }
            }
        }
    }
}

private const val CORRELATION_PRESENT_ATTRIBUTE: String = "correlation.present"
private const val CORRELATION_ID_ATTRIBUTE: String = "correlation.id"

private fun ApplicationRequest.sanitizedCorrelationId(
    settings: CorrelationIdSettings,
): String? =
    KtorCorrelationId.sanitize(
        rawValue = call.callId ?: headers[settings.requestHeaderName],
        maxLength = settings.maxLength
    )
