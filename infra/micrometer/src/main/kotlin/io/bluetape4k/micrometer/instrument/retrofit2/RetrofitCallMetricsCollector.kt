package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import okhttp3.Request
import retrofit2.Response
import java.time.Duration

class RetrofitCallMetricsCollector(
    baseUrl: String,
    uri: String,
    private val metricsRecorder: MetricsRecorder,
) {
    companion object: KLogging()

    private val baseTags = mutableMapOf(
        "base_url" to baseUrl,
        "uri" to uri
    )

    fun measureRequestDuration(duration: Duration, request: Request, response: Response<*>, async: Boolean = false) {
        val tags = mutableMapOf(
            "method" to request.method,
            "coroutines" to async.toString(),
            "outcome" to Outcome.fromHttpStatus(response.code()).name,
            "status_code" to response.code().toString()
        )
        tags.putAll(baseTags)

        return metricsRecorder.recordTiming(tags, duration)
    }

    fun measureRequestDuration(millis: Long, request: Request, response: Response<*>, async: Boolean = false) {
        measureRequestDuration(Duration.ofMillis(millis), request, response, async)
    }

    fun measureRequestException(duration: Duration, request: Request, error: Throwable, async: Boolean = false) {
        val tags = mutableMapOf(
            "method" to request.method,
            "coroutines" to async.toString(),
            "exception" to error.javaClass.simpleName
        )
        tags.putAll(baseTags)

        return metricsRecorder.recordTiming(tags, duration)
    }

    fun measureRequestException(millis: Long, request: Request, error: Throwable, async: Boolean = false) {
        measureRequestException(Duration.ofMillis(millis), request, error, async)
    }
}
