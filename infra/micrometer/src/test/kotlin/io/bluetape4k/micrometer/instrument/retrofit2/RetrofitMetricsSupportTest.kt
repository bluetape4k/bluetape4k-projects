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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

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

    @Test
    fun `recorder should not create extra meters when called concurrently with same tags`() {
        // 동일 태그 집합에 대해 동시 호출 시 Timer 인스턴스가 중복 생성되지 않음을 검증
        // ConcurrentHashMap.computeIfAbsent 가 레이스 컨디션 없이 단일 Timer 를 등록해야 한다
        val registry = SimpleMeterRegistry()
        val recorder = MicrometerRetrofitMetricsRecorder(registry)
        val tags =
            listOf(
                Tag.of("base_url", "https://concurrent.example.com"),
                Tag.of("uri", "/items"),
                Tag.of("method", "GET"),
                Tag.of("coroutines", "false"),
                Tag.of("outcome", Outcome.SUCCESS.name),
                Tag.of("status_code", "200"),
            )

        val threadCount = 8
        val callsPerThread = 20
        val latch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)

        val futures = List(threadCount) {
            executor.submit {
                latch.await()
                repeat(callsPerThread) {
                    recorder.recordTiming(tags, Duration.ofMillis(5))
                }
            }
        }
        latch.countDown()
        futures.forEach { it.get() }
        executor.shutdown()

        val timer = registry.find(MicrometerRetrofitMetricsRecorder.METRICS_KEY).tags(tags).timer()
        timer.shouldNotBeNull()
        timer.count() shouldBeEqualTo (threadCount * callsPerThread).toLong()
        // 동일 태그 집합에 대해 하나의 meter 만 등록되어야 한다
        registry.find(MicrometerRetrofitMetricsRecorder.METRICS_KEY).meters().size shouldBeEqualTo 1
    }

    @Test
    fun `collector coroutines tag should be false for sync calls`() {
        // async=false 일 때 coroutines 태그가 "false" 임을 검증
        val recorded = mutableListOf<List<Tag>>()
        val collector =
            RetrofitCallMetricsCollector("https://example.com", "/sync") { tags, _ ->
                recorded += tags.toList()
            }

        collector.measureRequestDuration(
            Duration.ofMillis(10),
            Request.Builder().url("https://example.com/sync").get().build(),
            Response.success("ok"),
            async = false,
        )

        val tags = recorded.single().associate { it.key to it.value }
        tags["coroutines"] shouldBeEqualTo "false"
    }

    @Test
    fun `collector millis overload should produce same tags as duration overload`() {
        // millis 오버로드가 Duration 오버로드와 동일한 태그를 생성하는지 검증
        val durationTags = mutableListOf<List<Tag>>()
        val millisTags = mutableListOf<List<Tag>>()
        val request = Request.Builder().url("https://example.com/data").get().build()
        val response = Response.success<String>("ok")

        RetrofitCallMetricsCollector("https://example.com", "/data") { tags, _ ->
            durationTags += tags.toList()
        }.measureRequestDuration(Duration.ofMillis(15), request, response)

        RetrofitCallMetricsCollector("https://example.com", "/data") { tags, _ ->
            millisTags += tags.toList()
        }.measureRequestDuration(15L, request, response)

        val durationMap = durationTags.single().associate { it.key to it.value }
        val millisMap = millisTags.single().associate { it.key to it.value }
        durationMap shouldBeEqualTo millisMap
    }

    @Test
    fun `collector exception millis overload should produce same tags as duration overload`() {
        // 예외 경로에서 millis 오버로드가 Duration 오버로드와 동일한 태그를 생성하는지 검증
        val durationTags = mutableListOf<List<Tag>>()
        val millisTags = mutableListOf<List<Tag>>()
        val request = Request.Builder().url("https://example.com/err").get().build()
        val error = IOException("timeout")

        RetrofitCallMetricsCollector("https://example.com", "/err") { tags, _ ->
            durationTags += tags.toList()
        }.measureRequestException(Duration.ofMillis(30), request, error, async = true)

        RetrofitCallMetricsCollector("https://example.com", "/err") { tags, _ ->
            millisTags += tags.toList()
        }.measureRequestException(30L, request, error, async = true)

        val durationMap = durationTags.single().associate { it.key to it.value }
        val millisMap = millisTags.single().associate { it.key to it.value }
        durationMap shouldBeEqualTo millisMap
        durationMap["coroutines"] shouldBeEqualTo "true"
    }

    @Test
    fun `collector async exception tag should reflect async parameter`() {
        // 예외 경로에서 async 파라미터가 coroutines 태그에 올바르게 반영되는지 검증
        val syncTags = mutableListOf<Map<String, String>>()
        val asyncTags = mutableListOf<Map<String, String>>()
        val request = Request.Builder().url("https://example.com/err").get().build()
        val error = RuntimeException("fail")

        RetrofitCallMetricsCollector("https://example.com", "/err") { tags, _ ->
            syncTags += tags.associate { it.key to it.value }
        }.measureRequestException(Duration.ofMillis(1), request, error, async = false)

        RetrofitCallMetricsCollector("https://example.com", "/err") { tags, _ ->
            asyncTags += tags.associate { it.key to it.value }
        }.measureRequestException(Duration.ofMillis(1), request, error, async = true)

        syncTags.single()["coroutines"] shouldBeEqualTo "false"
        asyncTags.single()["coroutines"] shouldBeEqualTo "true"
    }

    @Test
    fun `recorder distinct tag sets produce separate meters`() {
        // 서로 다른 태그 집합은 별도의 Timer 로 등록되어야 한다 (엔드포인트별 분리 보장)
        val registry = SimpleMeterRegistry()
        val recorder = MicrometerRetrofitMetricsRecorder(registry)

        val tagsGet =
            listOf(
                Tag.of("method", "GET"),
                Tag.of("uri", "/a"),
                Tag.of("base_url", "https://example.com"),
                Tag.of("coroutines", "false"),
                Tag.of("outcome", Outcome.SUCCESS.name),
                Tag.of("status_code", "200"),
            )
        val tagsPost =
            listOf(
                Tag.of("method", "POST"),
                Tag.of("uri", "/b"),
                Tag.of("base_url", "https://example.com"),
                Tag.of("coroutines", "false"),
                Tag.of("outcome", Outcome.SUCCESS.name),
                Tag.of("status_code", "201"),
            )

        recorder.recordTiming(tagsGet, Duration.ofMillis(10))
        recorder.recordTiming(tagsPost, Duration.ofMillis(15))

        registry.find(MicrometerRetrofitMetricsRecorder.METRICS_KEY).meters().size shouldBeEqualTo 2
    }
}
