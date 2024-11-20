package io.bluetape4k.micrometer.instrument.retrofit2

import java.time.Duration

fun interface MetricsRecorder {

    fun recordTiming(tags: Map<String, String>, duration: Duration)

}
