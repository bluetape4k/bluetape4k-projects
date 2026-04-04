package io.bluetape4k.opentelemetry.metrics

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder

/**
 * 무기록, 무출력 하는 meter 들을 제공하는 [MeterProvider]
 *
 * ```kotlin
 * val provider = NoopMeterProvider
 * val meter = provider.get("my-meter")
 * // meter != null
 * ```
 */
@JvmField
val NoopMeterProvider: MeterProvider = MeterProvider.noop()

/**
 * [SdkMeterProvider] 를 생성합니다.
 *
 * ```kotlin
 * val exporter = inMemoryMetricExporterOf()
 * val reader = periodicMetricReader(exporter) { }
 * val provider = sdkMeterProvider {
 *     registerMetricReader(reader)
 * }
 * // provider != null
 * ```
 *
 * @param builder [SdkMeterProviderBuilder] 를 설정하는 람다입니다.
 * @return [SdkMeterProvider] 인스턴스
 */
inline fun sdkMeterProvider(
    builder: SdkMeterProviderBuilder.() -> Unit,
): SdkMeterProvider {
    return SdkMeterProvider.builder().apply(builder).build()
}
