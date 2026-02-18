package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import okhttp3.Request
import retrofit2.Response
import java.time.Duration

/**
 * Retrofit 호출 메트릭을 수집하는 클래스입니다.
 *
 * 각 HTTP 요청의 메서드, URI, 응답 코드, 결과 등의 정보를 수집하여
 * [MetricsRecorder]에 전달합니다.
 *
 * @param baseUrl 기본 URL
 * @param uri 요청 URI
 * @param metricsRecorder 메트릭을 기록할 레코더
 */
class RetrofitCallMetricsCollector(
    baseUrl: String,
    uri: String,
    private val metricsRecorder: MetricsRecorder,
) {
    companion object: KLogging()

    private val baseTags =
        mutableMapOf(
            "base_url" to baseUrl,
            "uri" to uri,
        )

    fun measureRequestDuration(
        duration: Duration,
        request: Request,
        response: Response<*>,
        async: Boolean = false,
    ) {
        val tags =
            mutableMapOf(
                "method" to request.method,
                "coroutines" to async.toString(),
                "outcome" to Outcome.fromHttpStatus(response.code()).name,
                "status_code" to response.code().toString(),
            )
        tags.putAll(baseTags)

        return metricsRecorder.recordTiming(tags, duration)
    }

    fun measureRequestDuration(
        millis: Long,
        request: Request,
        response: Response<*>,
        async: Boolean = false,
    ) {
        measureRequestDuration(Duration.ofMillis(millis), request, response, async)
    }

    fun measureRequestException(
        duration: Duration,
        request: Request,
        error: Throwable,
        async: Boolean = false,
    ) {
        val tags =
            mutableMapOf(
                "method" to request.method,
                "coroutines" to async.toString(),
                "exception" to error.javaClass.simpleName,
            )
        tags.putAll(baseTags)

        return metricsRecorder.recordTiming(tags, duration)
    }

    fun measureRequestException(
        millis: Long,
        request: Request,
        error: Throwable,
        async: Boolean = false,
    ) {
        measureRequestException(Duration.ofMillis(millis), request, error, async)
    }
}
