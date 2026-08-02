package io.bluetape4k.ktor.observability

import io.opentelemetry.api.OpenTelemetry

/**
 * Ktor OpenTelemetry 서버 tracing을 명시적으로 활성화하는 설정입니다.
 *
 * ## 계약
 * - 애플리케이션이 [openTelemetry] 인스턴스, SDK, exporter, backend를 소유합니다.
 * - 이 모듈은 전역 OpenTelemetry SDK를 등록하거나 변경하지 않습니다.
 * - [correlationId]가 `null`이면 통합 observability installer의 정책을 상속하고,
 *   standalone tracing 설치에서는 기본 [CorrelationIdSettings]를 사용합니다.
 * - 명시한 [correlationId]는 통합 installer의 정책보다 우선하는 trace 전용 override입니다.
 * - 정제된 correlation ID는 [captureSanitizedCorrelationId]가 활성화된 경우에만 수집합니다.
 */
class KtorOpenTelemetryTracingConfig(
    val openTelemetry: OpenTelemetry,
    val correlationId: CorrelationIdSettings? = null,
    val captureSanitizedCorrelationId: Boolean = false,
)
