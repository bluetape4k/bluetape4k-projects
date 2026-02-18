package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics

/**
 * Micrometer 기반의 Retrofit 메트릭 팩토리입니다.
 *
 * Micrometer [MeterRegistry]를 사용하여 Retrofit HTTP 호출의 메트릭을 수집합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val registry = SimpleMeterRegistry()
 * val factory = MicrometerRetrofitMetricsFactory(registry)
 * val retrofit = Retrofit.Builder()
 *     .baseUrl(baseUrl)
 *     .addCallAdapterFactory(factory)
 *     .build()
 * ```
 *
 * @param meterRegistry Micrometer 메트릭 레지스트리
 */
class MicrometerRetrofitMetricsFactory private constructor(
    meterRegistry: MeterRegistry,
): RetrofitMetricsFactory(MicrometerRetrofitMetricsRecorder(meterRegistry)) {
    companion object: KLogging() {
        /**
         * [MicrometerRetrofitMetricsFactory] 인스턴스를 생성합니다.
         *
         * @param meterRegistry Micrometer 메트릭 레지스트리 (기본값: globalRegistry)
         * @return 생성된 팩토리 인스턴스
         */
        @JvmStatic
        operator fun invoke(meterRegistry: MeterRegistry = Metrics.globalRegistry): MicrometerRetrofitMetricsFactory =
            MicrometerRetrofitMetricsFactory(meterRegistry)
    }
}
