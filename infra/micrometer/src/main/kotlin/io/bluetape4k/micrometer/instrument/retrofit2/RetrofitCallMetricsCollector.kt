package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.micrometer.core.instrument.Tag
import okhttp3.Request
import retrofit2.Response
import java.time.Duration

/**
 * Retrofit 호출 메트릭을 수집하는 클래스입니다.
 *
 * 각 HTTP 요청의 메서드, URI, 응답 코드, 결과 등의 정보를 수집하여
 * [MetricsRecorder]에 전달합니다. 고정 태그는 생성 시점에 캐시해 두고, 호출 시점에는 응답/예외별 태그만 추가합니다.
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
        listOf(
            Tag.of("base_url", baseUrl),
            Tag.of("uri", uri),
        )

    /**
     * 정상 응답에 대한 태그를 조합해 실행 시간을 기록합니다.
     *
     * 고정 태그는 생성 시점에 1회만 만들어 두고, 호출 시점에는 응답별 태그만 추가해
     * 불필요한 `Map` 재할당을 줄입니다.
     *
     * @param duration 요청 실행 시간
     * @param request 원본 HTTP 요청
     * @param response HTTP 응답
     * @param async 코루틴/비동기 경로 여부
     */
    fun measureRequestDuration(
        duration: Duration,
        request: Request,
        response: Response<*>,
        async: Boolean = false,
    ) {
        metricsRecorder.recordTiming(
            buildList(6) {
                add(Tag.of("method", request.method))
                add(Tag.of("coroutines", async.toString()))
                add(Tag.of("outcome", Outcome.fromHttpStatus(response.code()).name))
                add(Tag.of("status_code", response.code().toString()))
                addAll(baseTags)
            },
            duration,
        )
    }

    /**
     * 밀리초 단위 실행 시간을 [Duration] 으로 변환해 정상 응답 메트릭을 기록합니다.
     */
    fun measureRequestDuration(
        millis: Long,
        request: Request,
        response: Response<*>,
        async: Boolean = false,
    ) {
        measureRequestDuration(Duration.ofMillis(millis), request, response, async)
    }

    /**
     * 예외가 발생한 요청에 대한 메트릭을 기록합니다.
     *
     * 실패 경로에서도 `outcome`, `status_code` 태그를 항상 채워 시계열 스키마가 흔들리지 않도록 유지합니다.
     *
     * @param duration 요청 실행 시간
     * @param request 원본 HTTP 요청
     * @param error 발생한 예외
     * @param async 코루틴/비동기 경로 여부
     */
    fun measureRequestException(
        duration: Duration,
        request: Request,
        error: Throwable,
        async: Boolean = false,
    ) {
        metricsRecorder.recordTiming(
            buildList(7) {
                add(Tag.of("method", request.method))
                add(Tag.of("coroutines", async.toString()))
                add(Tag.of("outcome", Outcome.UNKNOWN.name))
                add(Tag.of("status_code", "IO_ERROR"))
                add(Tag.of("exception", error.javaClass.simpleName))
                addAll(baseTags)
            },
            duration,
        )
    }

    /**
     * 밀리초 단위 실행 시간을 [Duration] 으로 변환해 예외 메트릭을 기록합니다.
     */
    fun measureRequestException(
        millis: Long,
        request: Request,
        error: Throwable,
        async: Boolean = false,
    ) {
        measureRequestException(Duration.ofMillis(millis), request, error, async)
    }
}
