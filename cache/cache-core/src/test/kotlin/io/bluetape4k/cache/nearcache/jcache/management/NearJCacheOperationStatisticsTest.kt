package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.nearcache.jcache.BackCacheWriteCompletion
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
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

    @Test
    fun `getAll back RuntimeException은 identity를 보존하고 front와 통계를 변경하지 않는다`() {
        val fixture = statisticsEnabledFixture()
        val keys = setOf("back", "missing")
        val failure = IllegalStateException("provider unavailable")
        every { fixture.front.getAll(keys) } returns emptyMap()
        every { fixture.back.getAll(keys) } throws failure

        val thrown = assertFailsWith<IllegalStateException> { fixture.cache.getAll(keys) }

        thrown shouldBeSameInstanceAs failure
        verify(exactly = 0) { fixture.front.putAll(any()) }
        fixture.statistics.cacheGets shouldBeEqualTo 0L
        fixture.statistics.cacheHits shouldBeEqualTo 0L
        fixture.statistics.cacheMisses shouldBeEqualTo 0L
        fixture.statistics.getFrontHits() shouldBeEqualTo 0L
        fixture.statistics.getFrontMisses() shouldBeEqualTo 0L
        fixture.statistics.getBackHits() shouldBeEqualTo 0L
        fixture.statistics.getBackMisses() shouldBeEqualTo 0L
    }

    @Test
    fun `getAll back CancellationException은 identity를 보존하고 front와 통계를 변경하지 않는다`() {
        val fixture = statisticsEnabledFixture()
        val keys = setOf("back", "missing")
        val failure = CancellationException("caller cancelled")
        every { fixture.front.getAll(keys) } returns emptyMap()
        every { fixture.back.getAll(keys) } throws failure

        val thrown = assertFailsWith<CancellationException> { fixture.cache.getAll(keys) }

        thrown shouldBeSameInstanceAs failure
        verify(exactly = 0) { fixture.front.putAll(any()) }
        fixture.statistics.cacheGets shouldBeEqualTo 0L
        fixture.statistics.cacheHits shouldBeEqualTo 0L
        fixture.statistics.cacheMisses shouldBeEqualTo 0L
        fixture.statistics.getFrontHits() shouldBeEqualTo 0L
        fixture.statistics.getFrontMisses() shouldBeEqualTo 0L
        fixture.statistics.getBackHits() shouldBeEqualTo 0L
        fixture.statistics.getBackMisses() shouldBeEqualTo 0L
    }

    @Test
    fun `put과 non-empty putAll은 정상 반환한 entry 수만큼 기록한다`() {
        val fixture = statisticsEnabledFixture()

        fixture.cache.put("put", "value")
        fixture.cache.putAll(mapOf("one" to "1", "two" to "2"))

        fixture.statistics.cachePuts shouldBeEqualTo 3L
    }

    @Test
    fun `empty putAll은 count와 clock을 사용하지 않는다`() {
        val time = CountingTimeSource()
        val fixture = statisticsEnabledFixture(time)

        fixture.cache.putAll(emptyMap())

        fixture.statistics.cachePuts shouldBeEqualTo 0L
        time.invocations shouldBeEqualTo 0
    }

    @Test
    fun `putIfAbsent는 실제 삽입 성공만 put으로 기록한다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.back.putIfAbsent("key", "value") } returnsMany listOf(true, false)

        fixture.cache.putIfAbsent("key", "value").shouldBeTrue()
        fixture.cache.putIfAbsent("key", "value") shouldBeEqualTo false

        fixture.statistics.cachePuts shouldBeEqualTo 1L
    }

    @Test
    fun `replace overload는 실제 교체 성공만 put으로 기록한다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.back.replace("key", "value") } returnsMany listOf(true, false)
        every { fixture.back.replace("key", "old", "new") } returnsMany listOf(true, false)

        fixture.cache.replace("key", "value").shouldBeTrue()
        fixture.cache.replace("key", "value") shouldBeEqualTo false
        fixture.cache.replace("key", "old", "new").shouldBeTrue()
        fixture.cache.replace("key", "old", "new") shouldBeEqualTo false

        fixture.statistics.cachePuts shouldBeEqualTo 2L
    }

    @Test
    fun `remove overload는 실제 삭제 성공만 removal로 기록한다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.back.remove("key") } returnsMany listOf(true, false)
        every { fixture.back.remove("key", "old") } returnsMany listOf(true, false)

        fixture.cache.remove("key").shouldBeTrue()
        fixture.cache.remove("key") shouldBeEqualTo false
        fixture.cache.remove("key", "old").shouldBeTrue()
        fixture.cache.remove("key", "old") shouldBeEqualTo false

        fixture.statistics.cacheRemovals shouldBeEqualTo 2L
    }

    @Test
    fun `getAndPut은 이전 값 hit와 put을 같은 elapsed로 각각 기록한다`() {
        val fixture = statisticsEnabledFixture(CountingTimeSource(10L, 1_010L))
        every { fixture.back.getAndPut("key", "new") } returns "old"

        fixture.cache.getAndPut("key", "new") shouldBeEqualTo "old"

        fixture.statistics.cacheHits shouldBeEqualTo 1L
        fixture.statistics.cachePuts shouldBeEqualTo 1L
        fixture.statistics.averageGetTime shouldBeEqualTo 1.0F
        fixture.statistics.averagePutTime shouldBeEqualTo 1.0F
    }

    @Test
    fun `getAndReplace는 이전 값이 있을 때만 put을 기록한다`() {
        val hit = statisticsEnabledFixture()
        every { hit.back.getAndReplace("key", "new") } returns "old"
        hit.cache.getAndReplace("key", "new") shouldBeEqualTo "old"
        hit.statistics.cacheHits shouldBeEqualTo 1L
        hit.statistics.cachePuts shouldBeEqualTo 1L

        val miss = statisticsEnabledFixture()
        every { miss.back.getAndReplace("key", "new") } returns null
        miss.cache.getAndReplace("key", "new") shouldBeEqualTo null
        miss.statistics.cacheMisses shouldBeEqualTo 1L
        miss.statistics.cachePuts shouldBeEqualTo 0L
    }

    @Test
    fun `getAndRemove는 이전 값이 있을 때만 removal을 기록한다`() {
        val hit = statisticsEnabledFixture()
        every { hit.back.getAndRemove("key") } returns "old"
        hit.cache.getAndRemove("key") shouldBeEqualTo "old"
        hit.statistics.cacheHits shouldBeEqualTo 1L
        hit.statistics.cacheRemovals shouldBeEqualTo 1L

        val miss = statisticsEnabledFixture()
        every { miss.back.getAndRemove("key") } returns null
        miss.cache.getAndRemove("key") shouldBeEqualTo null
        miss.statistics.cacheMisses shouldBeEqualTo 1L
        miss.statistics.cacheRemovals shouldBeEqualTo 0L
    }

    @Test
    fun `예외로 종료된 putAll은 partial count와 시간을 추정하지 않는다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.front.putAll(any()) } throws IllegalStateException("front unavailable")

        assertFailsWith<IllegalStateException> {
            fixture.cache.putAll(mapOf("one" to "1", "two" to "2"))
        }

        fixture.statistics.cachePuts shouldBeEqualTo 0L
        fixture.cache.statisticsRecorder.current().totalPutTimeNanos shouldBeEqualTo 0L
    }

    @Test
    fun `동기 back failure로 종료된 put은 caller-visible 성공을 기록하지 않는다`() {
        val fixture = statisticsEnabledFixture()
        every { fixture.back.put("key", "value") } throws IllegalStateException("back unavailable")

        assertFailsWith<IllegalStateException> { fixture.cache.put("key", "value") }

        fixture.statistics.cachePuts shouldBeEqualTo 0L
        fixture.cache.statisticsRecorder.current().totalPutTimeNanos shouldBeEqualTo 0L
    }

    @Test
    fun `removeAll과 cache clear는 removal을 추정하거나 기존 통계를 초기화하지 않는다`() {
        val fixture = statisticsEnabledFixture()
        fixture.cache.put("key", "value")

        fixture.cache.removeAll(setOf("key"))
        fixture.cache.removeAll()
        fixture.cache.clear()

        fixture.statistics.cachePuts shouldBeEqualTo 1L
        fixture.statistics.cacheRemovals shouldBeEqualTo 0L
    }

    @Test
    fun `statistics clear는 cache data operation을 실행하지 않고 generation만 교체한다`() {
        val fixture = statisticsEnabledFixture()
        fixture.cache.put("key", "value")

        fixture.statistics.clear()

        fixture.statistics.cachePuts shouldBeEqualTo 0L
        fixture.cache.statisticsRecorder.current().cacheGets shouldBeEqualTo 0L
        verify(exactly = 0) { fixture.front.clear() }
        verify(exactly = 0) { fixture.back.clear() }
    }

    @Test
    fun `async caller 성공은 이후 exceptional completion에도 put count를 유지한다`() {
        val fixture = statisticsEnabledFixture(synchronous = false, retryCount = 0)
        val observed = CompletableFuture<BackCacheWriteCompletion>()
        val observation = fixture.cache.addBackCacheWriteListener(observed::complete)
        every { fixture.back.put("key", "value") } throws IllegalStateException("back unavailable")

        try {
            fixture.cache.put("key", "value")
            fixture.statistics.cachePuts shouldBeEqualTo 1L

            val write = observed.get(5, TimeUnit.SECONDS)
            write.operation shouldBeEqualTo "put"
            (write.operationId > 0L).shouldBeTrue()
            assertFailsWith<ExecutionException> {
                write.completion.toCompletableFuture().get(5, TimeUnit.SECONDS)
            }
            fixture.statistics.cachePuts shouldBeEqualTo 1L
        } finally {
            observation.close()
        }
    }

    private fun statisticsEnabledFixture(
        timeSource: NearJCacheTimeSource = CountingTimeSource(0L, 1_000L),
        synchronous: Boolean = true,
        retryCount: Int = NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT,
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
            config = NearJCacheConfig(
                frontCacheConfiguration = configuration,
                isSynchronous = synchronous,
                syncRemoteRetryCount = retryCount,
            ),
            timeSource = timeSource,
            clearAuthority = NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
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
