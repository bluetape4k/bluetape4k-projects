package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

/**
 * 메트릭 수집이 가능한 Retrofit CallAdapter 구현체입니다.
 *
 * 원본 CallAdapter를 감싸서 반환되는 Call을 [MeasuredCall]로 변환합니다.
 * 이를 통해 모든 HTTP 호출의 메트릭 수집이 자동으로 이루어집니다.
 *
 * @param R 응답 타입
 * @param T 반환 타입
 * @param nextCallAdapter 다음 CallAdapter
 * @param metricsCollector 메트릭 수집기
 */
class MeasuredCallAdapter<R: Any, T: Any> internal constructor(
    private val nextCallAdapter: CallAdapter<R, T>,
    private val metricsCollector: RetrofitCallMetricsCollector,
): CallAdapter<R, T> {
    companion object: KLogging()

    /**
     * Returns the value type that this adapter uses when converting the HTTP response body to a Java
     * object. For example, the response type for `Call<Repo>` is `Repo`. This type is
     * used to prepare the `call` passed to `#adapt`.
     *
     *
     * Note: This is typically not the same type as the `returnType` provided to this call
     * adapter's factory.
     */
    override fun responseType(): Type = nextCallAdapter.responseType()

    /**
     * Returns an instance of `T` which delegates to `call`.
     *
     *
     * For example, given an instance for a hypothetical utility, `Async`, this instance
     * would return a new `Async<R>` which invoked `call` when run.
     *
     * <pre>`
     * &#64;Override
     * public <R> Async<R> adapt(final Call<R> call) {
     * return Async.create(new Callable<Response<R>>() {
     * &#64;Override
     * public Response<R> call() throws Exception {
     * return call.execute();
     * }
     * });
     * }
    `</pre> *
     */
    override fun adapt(call: Call<R>): T {
        log.debug { "Adapt call with MeasuredCall ... call=$call" }
        return nextCallAdapter.adapt(MeasuredCall(call, metricsCollector))
    }
}
