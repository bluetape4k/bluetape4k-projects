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
 * The entrypoint to telemetry functionality for tracing, metrics and baggage.
 *
 * If using the OpenTelemetry SDK, you may want to instantiate the [OpenTelemetry] to
 * provide configuration, for example of `Resource` or `Sampler`.
 * See [OpenTelemetrySdk] and [OpenTelemetrySdk.builder] for information on how to construct the
 * SDK [OpenTelemetry].
 */
@JvmField
val NoopOpenTelemetry: OpenTelemetry = OpenTelemetry.noop()

/**
 * Return the registered global [OpenTelemetry].
 */
var globalOpenTelemetry: OpenTelemetry
    get() = GlobalOpenTelemetry.get()
    set(value) {
        GlobalOpenTelemetry.set(value)
    }

/**
 * Returns an [OpenTelemetry] which will do remote propagation of
 * [io.opentelemetry.context.Context] using the provided [ContextPropagators] and is no-op
 * otherwise.
 */
fun openTelemetryOf(propagators: ContextPropagators): OpenTelemetry =
    OpenTelemetry.propagating(propagators)

/**
 * [OpenTelemetrySdkBuilder] 를 이용하여 [OpenTelemetrySdk] 인스턴스를 빌드합니다.
 */
inline fun openTelemetrySdk(
    @BuilderInference builder: OpenTelemetrySdkBuilder.() -> Unit,
): OpenTelemetrySdk =
    OpenTelemetrySdk.builder().apply(builder).build()

/**
 * [OpenTelemetrySdkBuilder] 를 이용하여 [OpenTelemetrySdk] 인스턴스를 빌드하고 Global OpenTelemetry 로 지정합니다.
 */
inline fun openTelemetrySdkGlobal(
    @BuilderInference builder: OpenTelemetrySdkBuilder.() -> Unit,
): OpenTelemetrySdk =
    OpenTelemetrySdk.builder().apply(builder).buildAndRegisterGlobal()

/**
 * [OpenTelemetrySdk]를 생성합니다.
 *
 * @param tracerProvider [SdkTracerProvider]
 * @param meterProvider [SdkMeterProvider]
 * @param loggerProvider [SdkLoggerProvider]
 * @param propagators [ContextPropagators]
 * @return
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
 * @param tracerName tracer name
 * @param builder tracer building block
 * @return [Tracer] instance
 */
inline fun OpenTelemetry.tracer(
    tracerName: String,
    @BuilderInference builder: TracerBuilder.() -> Unit,
): Tracer {
    tracerName.assertNotBlank("tracerName")
    return tracerProvider.tracerBuilder(tracerName).apply(builder).build()
}

/**
 * [MeterBuilder]를 이용하여 [Meter] 인스턴스를 빌드합니다.
 *
 * @param meterName meter name
 * @param builder meter building block
 * @return [Meter] instance
 */
inline fun OpenTelemetry.meter(
    meterName: String,
    @BuilderInference builder: MeterBuilder.() -> Unit,
): Meter {
    meterName.assertNotBlank("meterName")
    return meterProvider.meterBuilder(meterName).apply(builder).build()
}
