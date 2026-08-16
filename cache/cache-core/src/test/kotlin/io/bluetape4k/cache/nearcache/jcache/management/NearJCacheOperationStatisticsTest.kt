package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

class NearJCacheOperationStatisticsTest {

    @Test
    fun `front hit는 logical hit 하나와 front hit 하나를 기록한다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.front.get("key") } returns "value"

        fixture.cache.get("key") shouldBeEqualTo "value"

        fixture.statistics.cacheHits shouldBeEqualTo 1L
        fixture.statistics.cacheMisses shouldBeEqualTo 0L
        fixture.statistics.getFrontHits() shouldBeEqualTo 1L
        fixture.statistics.getFrontMisses() shouldBeEqualTo 0L
        fixture.statistics.getBackHits() shouldBeEqualTo 0L
        fixture.statistics.getBackMisses() shouldBeEqualTo 0L
    }

    @Test
    fun `front miss와 back hit는 logical hit 하나와 두 tier 결과를 기록한다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.front.get("key") } returns null
        every { fixture.back.get("key") } returns "value"

        fixture.cache.get("key") shouldBeEqualTo "value"

        fixture.statistics.cacheHits shouldBeEqualTo 1L
        fixture.statistics.cacheMisses shouldBeEqualTo 0L
        fixture.statistics.getFrontHits() shouldBeEqualTo 0L
        fixture.statistics.getFrontMisses() shouldBeEqualTo 1L
        fixture.statistics.getBackHits() shouldBeEqualTo 1L
        fixture.statistics.getBackMisses() shouldBeEqualTo 0L
    }

    @Test
    fun `front와 back miss는 logical miss 하나를 기록한다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.front.get("missing") } returns null
        every { fixture.back.get("missing") } returns null

        fixture.cache.get("missing") shouldBeEqualTo null

        fixture.statistics.cacheHits shouldBeEqualTo 0L
        fixture.statistics.cacheMisses shouldBeEqualTo 1L
        fixture.statistics.getFrontMisses() shouldBeEqualTo 1L
        fixture.statistics.getBackMisses() shouldBeEqualTo 1L
    }

    @Test
    fun `mixed getAll은 key별 logical 결과와 tier 결과를 한 번 기록한다`() {
        val time = CountingTimeSource(100L, 3_100L)
        val fixture = statisticsEnabledFixture(time)
        val keys = setOf("front", "back", "missing")
        every { fixture.front.getAll(keys) } returns mapOf("front" to "front-value")
        every { fixture.back.getAll(setOf("back", "missing")) } returns mapOf("back" to "back-value")

        fixture.cache.getAll(keys) shouldBeEqualTo mutableMapOf(
            "front" to "front-value",
            "back" to "back-value",
        )

        fixture.statistics.cacheHits shouldBeEqualTo 2L
        fixture.statistics.cacheMisses shouldBeEqualTo 1L
        fixture.statistics.getFrontHits() shouldBeEqualTo 1L
        fixture.statistics.getFrontMisses() shouldBeEqualTo 2L
        fixture.statistics.getBackHits() shouldBeEqualTo 1L
        fixture.statistics.getBackMisses() shouldBeEqualTo 1L
        fixture.cache.statisticsRecorder.current().totalGetTimeNanos shouldBeEqualTo 3_000L
        time.invocations shouldBeEqualTo 2
    }

    @Test
    fun `empty getAll은 count와 clock을 사용하지 않는다`() {
        val time = CountingTimeSource()
        val fixture = statisticsEnabledFixture(time)

        fixture.cache.getAll(emptySet()) shouldBeEqualTo emptyMap()

        fixture.statistics.cacheGets shouldBeEqualTo 0L
        fixture.cache.statisticsRecorder.current().totalGetTimeNanos shouldBeEqualTo 0L
        time.invocations shouldBeEqualTo 0
    }

    @Test
    fun `front populate RuntimeException을 숨겨도 caller-visible logical hit를 기록한다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.front.get("key") } returns null
        every { fixture.back.get("key") } returns "value"
        every { fixture.front.put("key", "value") } throws IllegalStateException("front unavailable")

        fixture.cache.get("key") shouldBeEqualTo "value"

        fixture.statistics.cacheHits shouldBeEqualTo 1L
        fixture.statistics.getFrontMisses() shouldBeEqualTo 1L
        fixture.statistics.getBackHits() shouldBeEqualTo 1L
    }

    @Test
    fun `예외로 종료된 read는 성공 count와 시간을 기록하지 않는다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.front.get("key") } returns null
        every { fixture.back.get("key") } throws IllegalStateException("back unavailable")

        assertFailsWith<IllegalStateException> { fixture.cache.get("key") }

        fixture.statistics.cacheGets shouldBeEqualTo 0L
        fixture.cache.statisticsRecorder.current().totalGetTimeNanos shouldBeEqualTo 0L
    }

    private fun statisticsEnabledFixture(
        timeSource: NearJCacheTimeSource = CountingTimeSource(0L, 1_000L),
    ): StatisticsFixture {
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStoreByValue(false)
            .setStatisticsEnabled(true)
        val front = configuredCache(configuration)
        val back = configuredCache(configuration)
        val cache = NearJCache.withTimeSource(
            frontCache = front,
            backCache = back,
            config = NearJCacheConfig(frontCacheConfiguration = configuration, isSynchronous = true),
            timeSource = timeSource,
        )
        return StatisticsFixture(
            cache = cache,
            front = front,
            back = back,
            statistics = NearJCacheStatisticsMXBean.fromRecorder(cache.statisticsRecorder),
        )
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
        val front: JCache<String, String>,
        val back: JCache<String, String>,
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
}
