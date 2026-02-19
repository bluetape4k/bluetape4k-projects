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
 * ট্রেসিং, মেট্রিক্স, এবং ব্যাগেজের জন্য টেলিমেট্রি কার্যকারিতার প্রবেশদ্বার।
 *
 * ওপেনটেলিমেট্রি SDK ব্যবহার করলে, আপনি কনফিগারেশন প্রদানের জন্য [OpenTelemetry] 인스턴স তৈরি করতে চাইতে পারেন,
 * উদাহরণস্বরূপ `Resource` বা `Sampler`। SDK [OpenTelemetry] কীভাবে তৈরি করবেন সে সম্পর্কে তথ্যের জন্য
 * [OpenTelemetrySdk] এবং [OpenTelemetrySdk.builder] দেখুন।
 */
@JvmField
val NoopOpenTelemetry: OpenTelemetry = OpenTelemetry.noop()

/**
 * 등록된 글로벌 [OpenTelemetry]를 반환합니다.
 */
var globalOpenTelemetry: OpenTelemetry
    get() = GlobalOpenTelemetry.get()
    set(value) {
        GlobalOpenTelemetry.set(value)
    }

/**
 * 제공된 [ContextPropagators]를 사용하여 [io.opentelemetry.context.Context]의 원격 전파를 수행하고
 * 그 외에는 no-op인 [OpenTelemetry]를 반환합니다.
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
 * @param tracerName tracer 이름
 * @param builder tracer 빌딩 블록
 * @return [Tracer] 인스턴스
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
 * @param meterName meter 이름
 * @param builder meter 빌딩 블록
 * @return [Meter] 인스턴스
 */
inline fun OpenTelemetry.meter(
    meterName: String,
    @BuilderInference builder: MeterBuilder.() -> Unit,
): Meter {
    meterName.assertNotBlank("meterName")
    return meterProvider.meterBuilder(meterName).apply(builder).build()
}
