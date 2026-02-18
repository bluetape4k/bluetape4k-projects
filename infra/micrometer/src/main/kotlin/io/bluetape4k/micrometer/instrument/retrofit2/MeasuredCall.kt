package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import okhttp3.Request
import org.apache.commons.lang3.time.StopWatch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * 메트릭 수집이 가능한 Retrofit Call 래퍼 클래스입니다.
 *
 * 원본 Call을 감싸서 실행 시간과 결과를 메트릭으로 수집합니다.
 * 동기([execute]) 및 비동기([enqueue]) 실행 모두를 지원합니다.
 *
 * @param T 응답 본문의 타입
 * @param wrappedClient 원본 Retrofit Call
 * @param metrics 메트릭 수집기
 */
class MeasuredCall<T: Any> internal constructor(
    private val wrappedCall: Call<T>,
    private val metrics: RetrofitCallMetricsCollector,
): Call<T> by wrappedCall {
    companion object: KLogging()

    /**
     * Synchronously send the request and return its response.
     *
     * @throws [okio.IOException] if a problem occurred talking to the server.
     * @throws [RuntimeException] (and subclasses) if an unexpected error occurs creating the request or
     * decoding the response.
     */
    override fun execute(): Response<T> {
        log.debug { "Execute call ... wrappedCall=$wrappedCall" }

        val stopwatch = StopWatch.createStarted()
        val request = wrappedCall.request()
        try {
            val response = wrappedCall.execute()
            metrics.measureRequestDuration(stopwatch.duration, request, response, false)
            return response
        } catch (e: Throwable) {
            metrics.measureRequestException(stopwatch.duration, request, e, false)
            throw e
        }
    }

    /**
     * Asynchronously send the request and notify `callback` of its response or if an error
     * occurred talking to the server, creating the request, or processing the response.
     */
    override fun enqueue(callback: Callback<T>) {
        log.debug { "Enqueue call ... wrappedCall=$wrappedCall" }
        wrappedCall.enqueue(measuredCallback(wrappedCall.request(), callback))
    }

    private fun measuredCallback(
        request: Request,
        callback: Callback<T>,
    ): Callback<T> =
        object: Callback<T> {
            val stopwatch = StopWatch.createStarted()

            override fun onResponse(
                call: Call<T>,
                response: Response<T>,
            ) {
                metrics.measureRequestDuration(stopwatch.duration, request, response, true)
                callback.onResponse(call, response)
            }

            override fun onFailure(
                call: Call<T>,
                error: Throwable,
            ) {
                metrics.measureRequestException(stopwatch.duration, request, error, true)
                callback.onFailure(call, error)
            }
        }
}
