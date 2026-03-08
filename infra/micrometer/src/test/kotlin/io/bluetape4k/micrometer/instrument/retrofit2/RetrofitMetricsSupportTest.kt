package io.bluetape4k.micrometer.instrument.retrofit2

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.Request
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import retrofit2.Response
import java.io.IOException
import java.time.Duration

class RetrofitMetricsSupportTest {

    @Test
    fun `collector should add stable tags for successful responses`() {
        val recorded = mutableListOf<List<Tag>>()
        val collector =
            RetrofitCallMetricsCollector("https://example.com", "/posts") { tags, _ ->
                recorded += tags.toList()
            }

        collector.measureRequestDuration(
            Duration.ofMillis(25),
            Request.Builder().url("https://example.com/posts").get().build(),
            Response.success("ok"),
            async = true,
        )

        val tags = recorded.single().associate { it.key to it.value }
        tags["base_url"] shouldBeEqualTo "https://example.com"
        tags["uri"] shouldBeEqualTo "/posts"
        tags["method"] shouldBeEqualTo "GET"
        tags["coroutines"] shouldBeEqualTo "true"
        tags["outcome"] shouldBeEqualTo Outcome.SUCCESS.name
        tags["status_code"] shouldBeEqualTo "200"
    }

    @Test
    fun `collector should classify exceptions without omitting status tags`() {
        val recorded = mutableListOf<List<Tag>>()
        val collector =
            RetrofitCallMetricsCollector("https://example.com", "/posts") { tags, _ ->
                recorded += tags.toList()
            }

        collector.measureRequestException(
            Duration.ofMillis(25),
            Request.Builder().url("https://example.com/posts").get().build(),
            IOException("boom"),
        )

        val tags = recorded.single().associate { it.key to it.value }
        tags["outcome"] shouldBeEqualTo Outcome.UNKNOWN.name
        tags["status_code"] shouldBeEqualTo "IO_ERROR"
        tags["exception"] shouldBeEqualTo IOException::class.java.simpleName
    }

    @Test
    fun `recorder should reuse registered timer for identical tag sets`() {
        val registry = SimpleMeterRegistry()
        val recorder = MicrometerRetrofitMetricsRecorder(registry)
        val tags =
            listOf(
                Tag.of("base_url", "https://example.com"),
                Tag.of("uri", "/posts"),
                Tag.of("method", "GET"),
                Tag.of("coroutines", "false"),
                Tag.of("outcome", Outcome.SUCCESS.name),
                Tag.of("status_code", "200"),
            )

        recorder.recordTiming(tags, Duration.ofMillis(10))
        recorder.recordTiming(tags, Duration.ofMillis(20))

        registry.find(MicrometerRetrofitMetricsRecorder.METRICS_KEY)
            .tags(tags)
            .timer()
            .shouldNotBeNull()
            .count() shouldBeEqualTo 2L

        registry.find(MicrometerRetrofitMetricsRecorder.METRICS_KEY).meters().size shouldBeEqualTo 1
    }
}
