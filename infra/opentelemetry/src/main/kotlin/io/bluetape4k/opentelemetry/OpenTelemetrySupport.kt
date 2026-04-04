package io.bluetape4k.opentelemetry

import io.bluetape4k.support.assertNotBlank
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.api.metrics.MeterBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.TracerBuilder
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.trace.SdkTracerProvider

/**
 * OpenTelemetry no-op 인스턴스를 제공합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `OpenTelemetry.noop()` 결과를 상수로 보관합니다.
 * - 추적/메트릭/로그 API 호출 시 실제 내보내기 동작은 수행하지 않습니다.
 * - 기본값이나 테스트용 OpenTelemetry가 필요할 때 사용할 수 있습니다.
 *
 * ```kotlin
 * val otel = NoopOpenTelemetry
 * // otel === OpenTelemetry.noop()
 * ```
 */
@JvmField
val NoopOpenTelemetry: OpenTelemetry = OpenTelemetry.noop()

/**
 * 전역 [OpenTelemetry]를 조회하거나 교체합니다.
 *
 * ## 동작/계약
 * - getter는 `GlobalOpenTelemetry.get()`를 반환합니다.
 * - setter는 `GlobalOpenTelemetry.set(value)`를 호출합니다.
 * - 전역 인스턴스 교체는 프로세스 전반에 영향을 줍니다.
 *
 * ```kotlin
 * globalOpenTelemetry = NoopOpenTelemetry
 * // globalOpenTelemetry === NoopOpenTelemetry
 * ```
 */
var globalOpenTelemetry: OpenTelemetry
    get() = GlobalOpenTelemetry.get()
    set(value) {
        GlobalOpenTelemetry.set(value)
    }

/**
 * 지정한 [ContextPropagators]만 적용된 [OpenTelemetry]를 생성합니다.
 *
 * ## 동작/계약
 * - `OpenTelemetry.propagating(propagators)`를 호출합니다.
 * - 추적/메트릭 provider는 no-op이고 context 전파만 유효합니다.
 * - 호출마다 새 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val otel = openTelemetryOf(propagators)
 * // otel.propagators === propagators
 * ```
 */
fun openTelemetryOf(propagators: ContextPropagators): OpenTelemetry =
    OpenTelemetry.propagating(propagators)

/**
 * [OpenTelemetrySdkBuilder] 를 이용하여 [OpenTelemetrySdk] 인스턴스를 빌드합니다.
 *
 * ```kotlin
 * val sdk = openTelemetrySdk {
 *     setTracerProvider(sdkTracerProvider { })
 *     setMeterProvider(sdkMeterProvider { })
 * }
 * // sdk != null
 * ```
 */
inline fun openTelemetrySdk(
    builder: OpenTelemetrySdkBuilder.() -> Unit,
): OpenTelemetrySdk =
    OpenTelemetrySdk.builder().apply(builder).build()

/**
 * [OpenTelemetrySdkBuilder] 를 이용하여 [OpenTelemetrySdk] 인스턴스를 빌드하고 Global OpenTelemetry 로 지정합니다.
 *
 * ```kotlin
 * val sdk = openTelemetrySdkGlobal {
 *     setTracerProvider(sdkTracerProvider { })
 * }
 * // globalOpenTelemetry != NoopOpenTelemetry
 * ```
 */
inline fun openTelemetrySdkGlobal(
    builder: OpenTelemetrySdkBuilder.() -> Unit,
): OpenTelemetrySdk =
    OpenTelemetrySdk.builder().apply(builder).buildAndRegisterGlobal()

/**
 * [OpenTelemetrySdk]를 생성합니다.
 *
 * ```kotlin
 * val sdk = openTelemetrySdkOf(
 *     tracerProvider = sdkTracerProvider { },
 *     meterProvider = sdkMeterProvider { },
 * )
 * // sdk != null
 * ```
 *
 * @param tracerProvider [SdkTracerProvider]
 * @param meterProvider [SdkMeterProvider]
 * @param loggerProvider [SdkLoggerProvider]
 * @param propagators [ContextPropagators]
 * @return [OpenTelemetry] 인스턴스
 */
fun openTelemetrySdkOf(
    tracerProvider: SdkTracerProvider? = null,
    meterProvider: SdkMeterProvider? = null,
    loggerProvider: SdkLoggerProvider? = null,
    propagators: ContextPropagators? = null,
): OpenTelemetry {
    return openTelemetrySdk {
        tracerProvider?.run { setTracerProvider(this) }
        meterProvider?.run { setMeterProvider(this) }
        loggerProvider?.run { setLoggerProvider(this) }
        propagators?.run { setPropagators(this) }
    }
}

/**
 * [TracerBuilder]를 이용하여 [Tracer] 인스턴스를 빌드합니다.
 *
 * ```kotlin
 * val otel = NoopOpenTelemetry
 * val tracer = otel.tracer("com.example.service") {
 *     setInstrumentationVersion("1.0.0")
 * }
 * // tracer != null
 * ```
 *
 * @param tracerName tracer 이름
 * @param builder tracer 빌딩 블록
 * @return [Tracer] 인스턴스
 */
inline fun OpenTelemetry.tracer(
    tracerName: String,
    builder: TracerBuilder.() -> Unit,
): Tracer {
    tracerName.assertNotBlank("tracerName")
    return tracerProvider.tracerBuilder(tracerName).apply(builder).build()
}

/**
 * [MeterBuilder]를 이용하여 [Meter] 인스턴스를 빌드합니다.
 *
 * ```kotlin
 * val otel = NoopOpenTelemetry
 * val meter = otel.meter("com.example.metrics") {
 *     setInstrumentationVersion("1.0.0")
 * }
 * // meter != null
 * ```
 *
 * @param meterName meter 이름
 * @param builder meter 빌딩 블록
 * @return [Meter] 인스턴스
 */
inline fun OpenTelemetry.meter(
    meterName: String,
    builder: MeterBuilder.() -> Unit,
): Meter {
    meterName.assertNotBlank("meterName")
    return meterProvider.meterBuilder(meterName).apply(builder).build()
}
