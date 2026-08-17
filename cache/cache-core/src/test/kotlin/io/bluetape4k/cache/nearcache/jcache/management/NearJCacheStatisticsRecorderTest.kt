package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

class NearJCacheStatisticsRecorderTest {

    @Test
    fun `operation은 시작 때 얻은 generation에만 기록한다`() {
        val time = CountingTimeSource(10L, 1_010L)
        val recorder = ActiveNearJCacheStatisticsRecorder(time)
        val old = recorder.current()
        val startedAt = old.startTimeNanos()

        recorder.clear()
        old.recordGet(
            startedAt = startedAt,
            hits = 1,
            misses = 0,
            frontHits = 1,
            frontMisses = 0,
            backHits = 0,
            backMisses = 0,
        )

        recorder.current().cacheGets shouldBeEqualTo 0L
        recorder.current().frontHits shouldBeEqualTo 0L
    }

    @Test
    fun `1 microsecond 미만의 평균 시간도 nanosecond 정밀도를 유지한다`() {
        val time = CountingTimeSource(1_000L, 1_500L)
        val recorder = ActiveNearJCacheStatisticsRecorder(time)
        val generation = recorder.current()
        val startedAt = generation.startTimeNanos()
        generation.recordGet(startedAt, 1, 0, 1, 0, 0, 0)
        val bean = NearJCacheStatisticsMXBean.fromRecorder(recorder)

        bean.averageGetTime shouldBeEqualTo 0.5F
    }

    @Test
    fun `count가 0이면 평균 시간과 percentage는 0이다`() {
        val bean = NearJCacheStatisticsMXBean.fromRecorder(
            ActiveNearJCacheStatisticsRecorder(CountingTimeSource()),
        )

        bean.averageGetTime shouldBeEqualTo 0F
        bean.averagePutTime shouldBeEqualTo 0F
        bean.averageRemoveTime shouldBeEqualTo 0F
        bean.cacheHitPercentage shouldBeEqualTo 0F
        bean.cacheMissPercentage shouldBeEqualTo 0F
    }

    @Test
    fun `clear와 concurrent update가 서로 다른 generation을 섞지 않는다`() {
        val recorder = ActiveNearJCacheStatisticsRecorder(IncrementingTimeSource())
        val old = recorder.current()
        val workers = Executors.newFixedThreadPool(2)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<Throwable>()

        try {
            workers.submit {
                ready.countDown()
                start.await()
                runCatching { repeat(1_000) { old.recordPut(old.startTimeNanos(), 1) } }
                    .exceptionOrNull()?.let(failures::add)
            }
            recorder.clear()
            val current = recorder.current()
            workers.submit {
                ready.countDown()
                start.await()
                runCatching { repeat(1_000) { current.recordPut(current.startTimeNanos(), 1) } }
                    .exceptionOrNull()?.let(failures::add)
            }
            ready.await(2, TimeUnit.SECONDS).shouldBeTrue()
            start.countDown()
        } finally {
            workers.shutdown()
            workers.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
        }

        failures.isEmpty().shouldBeTrue()
        recorder.current().cachePuts shouldBeEqualTo 1_000L
    }

    @Test
    fun `NoOp context는 실제 NearJCache operation에서도 clock을 읽지 않는다`() {
        val time = CountingTimeSource()
        val fixture = disabledStatisticsFixture(time)

        fixture.cache.get("key")
        fixture.cache.getAll(setOf("key", "missing"))
        fixture.cache.put("put", "value")
        fixture.cache.putAll(mapOf("one" to "1", "two" to "2"))
        fixture.cache.getAndPut("compound", "value")

        time.invocations shouldBeEqualTo 0
        fixture.statistics.cacheGets shouldBeEqualTo 0L
        fixture.statistics.cachePuts shouldBeEqualTo 0L
    }

    @Test
    fun `NoOp context와 Empty bean은 모든 counter를 0으로 유지한다`() {
        val recorder = NoOpNearJCacheStatisticsRecorder
        val context = recorder.current()
        context.recordGet(0L, 1, 1, 1, 1, 1, 1)
        context.recordPut(0L, 1)
        context.recordRemove(0L, 1)
        recorder.clear()

        context.cacheGets shouldBeEqualTo 0L
        context.cachePuts shouldBeEqualTo 0L
        context.cacheRemovals shouldBeEqualTo 0L
    }

    @Test
    fun `public constructor descriptor와 synthetic test factory를 유지한다`() {
        val cacheConstructors = NearJCache::class.java.constructors.filterNot { it.isSynthetic }
        cacheConstructors.size shouldBeEqualTo 2
        cacheConstructors.map { it.parameterTypes.toList() }.toSet() shouldBeEqualTo setOf(
            listOf(
                javax.cache.Cache::class.java,
                javax.cache.Cache::class.java,
                NearJCacheConfig::class.java,
            ),
            listOf(
                javax.cache.Cache::class.java,
                javax.cache.Cache::class.java,
                NearJCacheConfig::class.java,
                NearJCacheClearAuthority::class.java,
            ),
        )
        NearJCache.Companion::class.java.declaredMethods
            .single { it.name.startsWith("withTimeSource") && !it.name.contains("default") }
            .isSynthetic.shouldBeTrue()

        val beanConstructors = NearJCacheStatisticsMXBean::class.java.constructors.filterNot { it.isSynthetic }
        beanConstructors.size shouldBeEqualTo 1
        beanConstructors.single().parameterCount shouldBeEqualTo 0
        NearJCacheStatisticsMXBean.Companion::class.java.declaredMethods
            .single { it.name.startsWith("fromRecorder") }
            .isSynthetic.shouldBeTrue()
    }

    private fun disabledStatisticsFixture(timeSource: NearJCacheTimeSource): StatisticsFixture {
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStoreByValue(false)
            .setStatisticsEnabled(false)
        val front = configuredCache(configuration)
        val back = configuredCache(configuration)
        val cache = NearJCache.withTimeSource(
            frontCache = front,
            backCache = back,
            config = NearJCacheConfig(frontCacheConfiguration = configuration, isSynchronous = true),
            timeSource = timeSource,
        )
        return StatisticsFixture(cache, NearJCacheStatisticsMXBean.fromRecorder(cache.statisticsRecorder))
    }

    @Suppress("UNCHECKED_CAST")
    private fun configuredCache(configuration: MutableConfiguration<String, String>): JCache<String, String> {
        val cache = mockk<JCache<String, String>>(relaxed = true)
        val configurationClass = Configuration::class.java as Class<Configuration<String, String>>
        val completeConfigurationClass =
            CompleteConfiguration::class.java as Class<CompleteConfiguration<String, String>>
        every { cache.getConfiguration(configurationClass) } returns configuration
        every { cache.getConfiguration(completeConfigurationClass) } returns configuration
        return cache
    }

    private data class StatisticsFixture(
        val cache: NearJCache<String, String>,
        val statistics: NearJCacheStatisticsMXBean,
    )

    private class CountingTimeSource(
        private vararg val values: Long,
    ): NearJCacheTimeSource {
        private val index = AtomicInteger()
        val invocations: Int get() = index.get()

        override fun nanoTime(): Long {
            val current = index.getAndIncrement()
            return values.getOrElse(current) { values.lastOrNull() ?: 0L }
        }
    }

    private class IncrementingTimeSource: NearJCacheTimeSource {
        private val value = java.util.concurrent.atomic.AtomicLong()
        override fun nanoTime(): Long = value.incrementAndGet()
    }
}
