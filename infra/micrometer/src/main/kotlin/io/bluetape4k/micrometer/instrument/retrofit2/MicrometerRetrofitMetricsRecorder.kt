package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.time.Duration

class MicrometerRetrofitMetricsRecorder(private val meterRegistry: MeterRegistry): MetricsRecorder {

    companion object: KLogging() {
        const val METRICS_KEY = "retrofit2.requests"
        private val PERCENTILES = doubleArrayOf(0.5, 0.7, 0.9, 0.95, 0.97, 0.99)
        private fun asTags(tags: Map<String, String>): List<Tag> = tags.map { Tag.of(it.key, it.value) }
    }


    override fun recordTiming(tags: Map<String, String>, duration: Duration) {
        log.debug { "Measure $METRICS_KEY with tags $tags duration ${duration.toMillis()} ms recorded." }

        Timer.builder(METRICS_KEY)
            .tags(asTags(tags))
            .publishPercentiles(*PERCENTILES)
            .register(meterRegistry)
            .record(duration)
    }
}
