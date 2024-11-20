package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HTTP
import retrofit2.http.OPTIONS
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import java.lang.reflect.Type

open class RetrofitMetricsFactory(var metricsRecorder: MetricsRecorder): CallAdapter.Factory() {

    companion object: KLogging() {
        private fun getUri(annotations: Array<Annotation>): String {
            return annotations.asSequence()
                .mapNotNull { annotation ->
                    when (annotation) {
                        is GET     -> annotation.value
                        is POST    -> annotation.value
                        is PUT     -> annotation.value
                        is DELETE  -> annotation.value
                        is PATCH   -> annotation.value
                        is OPTIONS -> annotation.value
                        is HEAD    -> annotation.value
                        is HTTP    -> annotation.path
                        else       -> null
                    }
                }
                .firstOrNull()
                ?: throw UnsupportedOperationException("No Retrofit Annotation is provided.")
        }
    }

    /**
     * Returns a call adapter for interface methods that return `returnType`, or null if it
     * cannot be handled by this factory.
     */
    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) {
            return null
        }

        val nextCallAdapter = retrofit.nextCallAdapter(this, returnType, annotations)
        val collector = RetrofitCallMetricsCollector(
            retrofit.baseUrl().toString(),
            getUri(annotations),
            metricsRecorder
        )

        return MeasuredCallAdapter(nextCallAdapter, collector)
    }
}
