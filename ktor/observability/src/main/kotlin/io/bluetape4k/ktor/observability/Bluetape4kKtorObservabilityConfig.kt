package io.bluetape4k.ktor.observability

import io.ktor.server.metrics.micrometer.MicrometerMetricsConfig
import io.micrometer.core.instrument.MeterRegistry

/**
 * Explicit opt-in configuration for [installBluetape4kKtorObservability].
 *
 * ## Contract
 * - CallId and CallLogging are enabled by default.
 * - Micrometer is enabled only when [meterRegistry] is supplied.
 * - OpenTelemetry tracing is installed only when [tracing] is supplied.
 * - Prometheus export is provided by route helpers, not by this installer.
 */
class Bluetape4kKtorObservabilityConfig(
    val correlationId: CorrelationIdSettings = CorrelationIdSettings(),
    val callLogging: CallLoggingSettings = CallLoggingSettings(correlationId = correlationId),
    val meterRegistry: MeterRegistry? = null,
    val tracing: KtorOpenTelemetryTracingConfig? = null,
    val installCorrelationId: Boolean = true,
    val installCallLogging: Boolean = true,
    val installMicrometerMetrics: Boolean = meterRegistry != null,
    val configureMicrometer: MicrometerMetricsConfig.() -> Unit = {},
) {
    init {
        require(!installMicrometerMetrics || meterRegistry != null) {
            "meterRegistry must be provided when installMicrometerMetrics is true."
        }
    }
}
