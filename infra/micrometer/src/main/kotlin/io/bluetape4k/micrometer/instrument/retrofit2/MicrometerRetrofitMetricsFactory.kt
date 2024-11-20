package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics

class MicrometerRetrofitMetricsFactory private constructor(
    meterRegistry: MeterRegistry,
): RetrofitMetricsFactory(MicrometerRetrofitMetricsRecorder(meterRegistry)) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            meterRegistry: MeterRegistry = Metrics.globalRegistry,
        ): MicrometerRetrofitMetricsFactory {
            return MicrometerRetrofitMetricsFactory(meterRegistry)
        }
    }
}
