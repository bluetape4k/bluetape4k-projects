package io.bluetape4k.ktor.observability

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging

/**
 * Installs the bluetape4k Ktor observability baseline.
 *
 * ## Contract
 * - Plugin installation is explicit and application-owned.
 * - Correlation IDs are sanitized before MDC or response propagation.
 * - Micrometer metrics are installed only when a [Bluetape4kKtorObservabilityConfig.meterRegistry] is provided.
 * - OpenTelemetry tracing is installed only when a [Bluetape4kKtorObservabilityConfig.tracing] config is provided.
 *
 * ```kotlin
 * fun Application.module(registry: MeterRegistry) {
 *     installBluetape4kKtorObservability(
 *         Bluetape4kKtorObservabilityConfig(meterRegistry = registry)
 *     )
 * }
 * ```
 */
fun Application.installBluetape4kKtorObservability(
    config: Bluetape4kKtorObservabilityConfig = Bluetape4kKtorObservabilityConfig(),
) {
    config.tracing?.let(::installBluetape4kKtorOpenTelemetryTracing)

    if (config.installCorrelationId) {
        install(CallId) {
            bluetape4kCorrelationIds(config.correlationId)
        }
    }

    if (config.installCallLogging) {
        install(CallLogging) {
            bluetape4kCallLogging(config.callLogging)
        }
    }

    val registry = config.meterRegistry
    if (config.installMicrometerMetrics && registry != null) {
        install(MicrometerMetrics) {
            this.registry = registry
            config.configureMicrometer(this)
        }
    }
}
