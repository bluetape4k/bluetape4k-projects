package io.bluetape4k.ktor.observability

import io.ktor.server.metrics.micrometer.MicrometerMetricsConfig
import io.micrometer.core.instrument.MeterRegistry

/**
 * [installBluetape4kKtorObservability]를 위한 명시적 opt-in 설정입니다.
 *
 * ## 계약
 * - CallId와 CallLogging은 기본으로 활성화합니다.
 * - [correlationId]는 CallId, CallLogging, tracing의 공통 기본 정책입니다.
 * - [tracing]이 있고 그 설정의 [KtorOpenTelemetryTracingConfig.correlationId]가
 *   `null`이면 [correlationId]를 상속합니다.
 * - tracing 설정에 correlation ID 정책을 지정하면 tracing 전용 override로 사용합니다.
 * - [meterRegistry]가 제공된 경우에만 Micrometer를 활성화합니다.
 * - Prometheus export는 이 installer가 아니라 route helper가 제공합니다.
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
