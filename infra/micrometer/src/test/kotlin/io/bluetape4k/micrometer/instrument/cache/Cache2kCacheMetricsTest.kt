package io.bluetape4k.micrometer.instrument.cache

import io.bluetape4k.cache.cache2k.getOrCreateCache2k
import io.bluetape4k.logging.KLogging
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.search.RequiredSearch
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.cache2k.Cache
import org.cache2k.core.api.InternalCache
import org.cache2k.core.api.InternalCacheInfo
import org.junit.jupiter.api.Test

class Cache2kCacheMetricsTest {

    companion object: KLogging()

    private val expectedTag = Tags.of("app", "test")
    private val cache = getOrCreateCache2k<String, String>("test")
    private val metrics = Cache2kCacheMetrics(cache, expectedTag)

    @Test
    fun `report expected general metrics`() {
        val registry = SimpleMeterRegistry()
        metrics.bindTo(registry)

        verifyCommonCacheMetrics(registry, metrics)

        cache.put("first", "42")
        cache.put("second", "43")
        cache.get("first") shouldBeEqualTo "42"
        cache.get("not-exists").shouldBeNull()

        val cacheSize = registry.fetch("cache.size").gauge()
        cacheSize.value().toLong() shouldBeEqualTo metrics.size()

        val hitCount = registry.fetch("cache.gets").tag("result", "hit").functionCounter()
        hitCount.count().toLong() shouldBeEqualTo metrics.hitCount()

        val missCount = registry.fetch("cache.gets").tag("result", "miss").functionCounter()
        missCount.count().toLong() shouldBeEqualTo metrics.missCount()

        val cachePuts = registry.fetch("cache.puts").functionCounter()
        cachePuts.count().toLong() shouldBeEqualTo metrics.putCount()

        val eviction = registry.fetch("cache.evictions").functionCounter()
        eviction.count().toLong() shouldBeEqualTo metrics.evictionCount()

        val loadTime = registry.fetch("cache.load.duration").timeGauge()
        loadTime.value() shouldBeEqualTo cache.info.loadMillis.toDouble()

        val clearedTimestamp = registry.fetch("cache.cleared.timestamp").gauge()
        clearedTimestamp.value().toLong() shouldBeEqualTo (cache.info.clearedTime?.toEpochMilli() ?: 0L)

        val cacheLoadSuccess = registry.fetch("cache.load").tag("result", "success").functionCounter()
        cacheLoadSuccess.count().toLong() shouldBeEqualTo cache.info.loadCount

        val cacheLoadFailure = registry.fetch("cache.load").tag("result", "failure").functionCounter()
        cacheLoadFailure.count().toLong() shouldBeEqualTo cache.info.loadExceptionCount
    }

    @Test
    fun `construct instance via monitor method`() {
        val registry = SimpleMeterRegistry()
        Cache2kCacheMetrics.monitor(registry, cache, expectedTag)

        registry.fetch("cache.load.duration").timeGauge()
    }

    @Test
    fun `return cache size`() {
        metrics.size() shouldBeEqualTo cache.info.size
        cache.put("size", "42")
        metrics.size() shouldBeEqualTo cache.info.size
    }

    @Test
    fun `return hit count`() {
        metrics.hitCount() shouldBeEqualTo cache.info.heapHitCount
        cache.put("hitCount", "123")
        cache.get("hitCount") shouldBeEqualTo "123"
        metrics.hitCount() shouldBeEqualTo cache.info.heapHitCount
    }

    @Test
    fun `return miss count`() {
        metrics.missCount() shouldBeEqualTo cache.info.missCount
        cache.get("not-exists").shouldBeNull()
        metrics.missCount() shouldBeEqualTo cache.info.missCount
    }

    @Test
    fun `return eviction count`() {
        metrics.evictionCount() shouldBeEqualTo cache.info.evictedCount
        cache.put("evition", "42")
        cache.remove("evition")
        metrics.evictionCount() shouldBeEqualTo cache.info.evictedCount
    }

    @Test
    fun `return put count`() {
        metrics.putCount() shouldBeEqualTo cache.info.putCount
        cache.put("put.count", "42")
        metrics.putCount() shouldBeEqualTo cache.info.putCount
    }

    private fun verifyCommonCacheMetrics(registry: MeterRegistry, metrics: Cache2kCacheMetrics) {
        registry.get("cache.puts").tags(expectedTag).functionCounter()
        registry.get("cache.gets").tags(expectedTag).functionCounter()

        metrics.size().run {
            registry.get("cache.size").tags(expectedTag).gauge()
        }
        metrics.missCount().run {
            registry.get("cache.gets").tags(expectedTag).tag("result", "miss").functionCounter()
        }
        metrics.evictionCount().run {
            registry.get("cache.evictions").tags(expectedTag).functionCounter()
        }
    }

    private fun MeterRegistry.fetch(name: String, tags: Iterable<Tag> = expectedTag): RequiredSearch =
        get(name).tags(tags)

    private val Cache<*, *>.info: InternalCacheInfo get() = (this as InternalCache).info
}
