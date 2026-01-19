package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

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
