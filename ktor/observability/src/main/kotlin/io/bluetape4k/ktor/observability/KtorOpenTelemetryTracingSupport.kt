package io.bluetape4k.ktor.observability

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.ApplicationRequest
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry

/**
 * Ktor에 opt-in OpenTelemetry server tracing을 설치합니다.
 *
 * ## 계약
 * - HTTP server span 생성은 OpenTelemetry Ktor 3 instrumentation에 위임합니다.
 * - 호출자가 제공한 [KtorOpenTelemetryTracingConfig.openTelemetry] 인스턴스를 사용합니다.
 * - [KtorOpenTelemetryTracingConfig.correlationId]가 지정되지 않으면
 *   [inheritedCorrelationId]를 사용하고, standalone 설치에서는 기본 정책을 사용합니다.
 * - trace 전용 정책을 명시한 경우 해당 request header만 사용해 상위 CallId 정책과 분리합니다.
 * - span 종료 전에 `correlation.present`를 기록하고, 명시적으로 활성화한 경우에만
 *   정제된 `correlation.id`를 기록합니다.
 */
fun Application.installBluetape4kKtorOpenTelemetryTracing(
    config: KtorOpenTelemetryTracingConfig,
    inheritedCorrelationId: CorrelationIdSettings? = null,
) {
    val correlationId = config.correlationId ?: inheritedCorrelationId ?: CorrelationIdSettings()
    val useCallIdFallback = config.correlationId == null

    install(KtorServerTelemetry) {
        setOpenTelemetry(config.openTelemetry)
        attributesExtractor {
            onEnd {
                val sanitizedCorrelationId = request.sanitizedCorrelationId(correlationId, useCallIdFallback)
                attributes.put(CORRELATION_PRESENT_ATTRIBUTE, sanitizedCorrelationId != null)
                if (config.captureSanitizedCorrelationId && sanitizedCorrelationId != null) {
                    attributes.put(CORRELATION_ID_ATTRIBUTE, sanitizedCorrelationId)
                }
            }
        }
    }
}

private const val CORRELATION_PRESENT_ATTRIBUTE: String = "correlation.present"
private const val CORRELATION_ID_ATTRIBUTE: String = "correlation.id"

private fun ApplicationRequest.sanitizedCorrelationId(
    settings: CorrelationIdSettings,
    useCallIdFallback: Boolean,
): String? =
    KtorCorrelationId.sanitize(
        rawValue = if (useCallIdFallback) {
            call.callId ?: headers[settings.requestHeaderName]
        } else {
            headers[settings.requestHeaderName]
        },
        maxLength = settings.maxLength
    )
