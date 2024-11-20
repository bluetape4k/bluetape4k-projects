package io.bluetape4k.micrometer.instrument.cache

import io.bluetape4k.logging.KLogging
import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.TimeGauge
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder
import org.cache2k.Cache
import org.cache2k.core.api.InternalCache
import org.cache2k.core.api.InternalCacheInfo
import java.util.concurrent.TimeUnit

class Cache2kCacheMetrics(
    private val cache: org.cache2k.Cache<*, *>,
    private val tags: Iterable<Tag> = emptyList(),
): CacheMeterBinder<Any>(cache, cache.name, tags) {

    companion object: KLogging() {
        @JvmStatic
        fun <K, V> monitor(registry: MeterRegistry, cache: Cache<K, V>, vararg tags: Tag): Cache<K, V> {
            return monitor(registry, cache, Tags.of(*tags))
        }

        @JvmStatic
        fun <K, V> monitor(registry: MeterRegistry, cache: Cache<K, V>, tags: Iterable<Tag>): Cache<K, V> {
            Cache2kCacheMetrics(cache, tags).bindTo(registry)
            return cache
        }
    }

    private val Cache<*, *>.info: InternalCacheInfo
        get() = (this as InternalCache).info

    public override fun size(): Long = cache.info.size

    public override fun hitCount(): Long = cache.info.heapHitCount

    public override fun missCount(): Long = cache.info.missCount

    public override fun putCount(): Long = cache.info.putCount

    public override fun evictionCount(): Long = cache.info.evictedCount

    override fun bindImplementationSpecificMetrics(registry: MeterRegistry) {
        TimeGauge
            .builder("cache.load.duration", cache, TimeUnit.MILLISECONDS) {
                it.info.loadMillis.toDouble()
            }
            .tags(tagsWithCacheName)
            .description("새로운 캐시 엔트리를 로딩하는데 걸리는 시간")
            .register(registry)

        Gauge
            .builder("cache.cleared.timestamp", cache) {
                it.info.clearedTime.toEpochMilli().toDouble()
            }
            .tags(tagsWithCacheName)
            .description("캐시 클리어 시각")
            .register(registry)

        FunctionCounter
            .builder("cache.load", cache) {
                it.info.loadCount.toDouble()
            }
            .tags(tagsWithCacheName)
            .tags("result", "success")
            .description("로딩된 캐시 엔트리 갯수")
            .register(registry)

        FunctionCounter
            .builder("cache.load", cache) {
                it.info.loadExceptionCount.toDouble()
            }
            .tags(tagsWithCacheName)
            .tags("result", "failure")
            .description("캐시 엔트리 로딩 시 예외가 발생한 수")
            .register(registry)

        FunctionCounter
            .builder("cache.expired.count", cache) {
                it.info.expiredCount.toDouble()
            }
            .tags(tagsWithCacheName)
            .description("무효화된 캐시 엔트리 갯수")
            .register(registry)
    }
}
