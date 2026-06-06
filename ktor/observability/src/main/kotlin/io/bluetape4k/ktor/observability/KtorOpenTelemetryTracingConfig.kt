package io.bluetape4k.ktor.observability

import io.opentelemetry.api.OpenTelemetry

/**
 * Explicit opt-in configuration for Ktor OpenTelemetry server tracing.
 *
 * ## Contract
 * - The application owns the [openTelemetry] instance, SDK, exporters, and backend.
 * - This module does not register or mutate a global OpenTelemetry SDK.
 * - Sanitized correlation IDs are captured only when [captureSanitizedCorrelationId] is enabled.
 */
class KtorOpenTelemetryTracingConfig(
    val openTelemetry: OpenTelemetry,
    val correlationId: CorrelationIdSettings = CorrelationIdSettings(),
    val captureSanitizedCorrelationId: Boolean = false,
)
