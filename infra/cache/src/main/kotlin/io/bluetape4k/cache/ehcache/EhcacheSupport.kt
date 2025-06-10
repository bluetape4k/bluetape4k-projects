package io.bluetape4k.cache.ehcache

import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue
import org.ehcache.Cache
import org.ehcache.CacheManager
import org.ehcache.config.CacheConfiguration
import org.ehcache.config.builders.CacheConfigurationBuilder
import org.ehcache.config.builders.CacheManagerBuilder
import org.ehcache.config.builders.ConfigurationBuilder
import org.ehcache.config.builders.ResourcePoolsBuilder
import org.ehcache.config.units.EntryUnit
import org.ehcache.config.units.MemoryUnit

/**
 * Default Ehcache [CacheManager]
 */
val DefaultEhCacheCacheManager: CacheManager by lazy {
    ehcacheManager {
        this.withDefaultClassLoader()
    }
}

/**
 * Ehcache [CacheManager] 를 생성합니다.
 *
 * 참고: [Ehcache Code Samples](https://www.ehcache.org/documentation/2.8/code-samples.html)
 *
 * ```
 * val cacheManager = ehcacheManager { }
 * val cache = cacheManager.getOrCreateCache<String, String>("default") {
 *    this.heap(10_000, EntryUnit.ENTRIES)
 *    this.offheap(32, MemoryUnit.MB)
 *    this.disk(100, MemoryUnit.MB)
 *    this.withDefaultResilienceStrategy()
 * }
 * cache.put("key", "value")
 * val value = cache.get("key") // value is "value"
 * ```
 *
 * @param initializer [ConfigurationBuilder] 초기화 람다
 */
inline fun ehcacheManager(
    @BuilderInference initializer: ConfigurationBuilder.() -> Unit,
): CacheManager {
    val configuration = ConfigurationBuilder.newConfigurationBuilder()
        .withDefaultClassLoader()
        .apply(initializer)
        .build()

    return CacheManagerBuilder.newCacheManager(configuration).apply {
        init()
        ShutdownQueue.register(this)
    }
}

/**
 * [CacheManager] 에서 [Cache]`<K, V>` 를 생성하거나 가져옵니다.
 *
 * ```
 * val cache = cacheManager.getOrCreateCache<String, String>("default") {
 *   this.heap(10_000, EntryUnit.ENTRIES)
 *   this.offheap(32, MemoryUnit.MB)
 *   this.disk(100, MemoryUnit.MB)
 *   this.withDefaultResilienceStrategy()
 * }
 *
 * cache.put("key", "value")
 * val value = cache.get("key") // value is "value"
 * ```
 *
 * @param cacheName 캐시 이름
 * @param builder [ResourcePoolsBuilder] 초기화 람다
 */
inline fun <reified K: Any, reified V: Any> CacheManager.getOrCreateCache(
    cacheName: String,
    @BuilderInference builder: ResourcePoolsBuilder.() -> Unit = { this.heap(10_000L, EntryUnit.ENTRIES) },
): Cache<K, V> {
    cacheName.requireNotBlank("cacheName")

    return getCache(cacheName, K::class.java, V::class.java)
        ?: run {
            val resourcePools = ResourcePoolsBuilder
                .newResourcePoolsBuilder()
                .offheap(32L, MemoryUnit.MB)
                .apply(builder)
                .build()

            val cacheConfiguration: CacheConfiguration<K, V> = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(K::class.java, V::class.java, resourcePools)
                .withDefaultResilienceStrategy()
                .withDispatcherConcurrency(4)
                .withDefaultDiskStoreThreadPool()
                .build()

            createCache(cacheName, cacheConfiguration)
        }
}
