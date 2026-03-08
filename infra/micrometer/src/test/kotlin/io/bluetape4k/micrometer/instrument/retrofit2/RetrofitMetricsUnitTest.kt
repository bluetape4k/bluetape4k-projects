package io.bluetape4k.micrometer.instrument.retrofit2

import io.micrometer.core.instrument.Tag
import okhttp3.Request
import okio.Timeout
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class RetrofitMetricsUnitTest {

    @Test
    fun `measureRequestException should record consistent failure tags`() {
        val recorder = CapturingMetricsRecorder()
        val collector = RetrofitCallMetricsCollector("https://example.com", "/posts", recorder)

        collector.measureRequestException(
            Duration.ofMillis(12),
            getRequest("https://example.com/posts"),
            IOException("boom"),
            async = true,
        )

        recorder.lastTags shouldBeEqualTo mapOf(
            "method" to "GET",
            "coroutines" to "true",
            "outcome" to Outcome.UNKNOWN.name,
            "status_code" to "IO_ERROR",
            "exception" to "IOException",
            "base_url" to "https://example.com",
            "uri" to "/posts",
        )
    }

    @Test
    fun `clone should preserve measured instrumentation`() {
        val recorder = CapturingMetricsRecorder()
        val collector = RetrofitCallMetricsCollector("https://example.com", "/posts/{id}", recorder)
        val cloneCount = AtomicInteger()
        val measured =
            MeasuredCall(
                FakeCall(
                    request = getRequest("https://example.com/posts/1"),
                    response = Response.success("ok"),
                    cloneCount = cloneCount,
                ),
                collector,
            )

        val cloned = measured.clone()
        cloned.shouldBeInstanceOf<MeasuredCall<*>>()

        cloned.execute().body() shouldBeEqualTo "ok"
        cloneCount.get() shouldBeEqualTo 1
        recorder.lastTags shouldBeEqualTo mapOf(
            "method" to "GET",
            "coroutines" to "false",
            "outcome" to Outcome.SUCCESS.name,
            "status_code" to "200",
            "base_url" to "https://example.com",
            "uri" to "/posts/{id}",
        )
    }

    private fun getRequest(url: String): Request = Request.Builder().url(url).get().build()

    private class CapturingMetricsRecorder: MetricsRecorder {
        var lastTags: Map<String, String> = emptyMap()
            private set

        override fun recordTiming(
            tags: Iterable<Tag>,
            duration: Duration,
        ) {
            lastTags = tags.associate { it.key to it.value }
        }
    }

    private class FakeCall<T: Any>(
        private val request: Request,
        private val response: Response<T>,
        private val cloneCount: AtomicInteger,
    ): Call<T> {
        override fun execute(): Response<T> = response

        override fun enqueue(callback: Callback<T>) {
            callback.onResponse(this, response)
        }

        override fun isExecuted(): Boolean = false

        override fun cancel() = Unit

        override fun isCanceled(): Boolean = false

        override fun clone(): Call<T> {
            cloneCount.incrementAndGet()
            return FakeCall(request, response, cloneCount)
        }

        override fun request(): Request = request

        override fun timeout(): Timeout = Timeout.NONE
    }
}
