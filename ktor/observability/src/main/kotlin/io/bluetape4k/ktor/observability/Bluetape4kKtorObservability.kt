package io.bluetape4k.ktor.observability

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging

/**
 * bluetape4k Ktor 관측성 기본 구성을 설치합니다.
 *
 * ## 계약
 * - plugin 설치는 명시적이며 애플리케이션이 소유합니다.
 * - correlation ID는 MDC 또는 응답으로 전파하기 전에 정제합니다.
 * - [Bluetape4kKtorObservabilityConfig.correlationId]를 CallId, CallLogging,
 *   tracing이 기본으로 공유합니다.
 * - [KtorOpenTelemetryTracingConfig.correlationId]를 지정하면 tracing만 사용하는
 *   명시적 override로 동작합니다.
 * - Micrometer metrics는 [Bluetape4kKtorObservabilityConfig.meterRegistry]가 제공된
 *   경우에만 설치합니다.
 * - OpenTelemetry tracing은 [Bluetape4kKtorObservabilityConfig.tracing]이 제공된
 *   경우에만 설치합니다.
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
    config.tracing?.let { tracing ->
        installBluetape4kKtorOpenTelemetryTracing(
            config = tracing,
            inheritedCorrelationId = config.correlationId,
        )
    }

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
