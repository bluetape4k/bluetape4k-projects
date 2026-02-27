package io.bluetape4k.cache.nearcache.hazelcast

import io.bluetape4k.cache.jcache.jcacheManagerOf
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotNull
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.CacheException
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

/**
 * Hazelcast NearCache 전용 [CacheManager] 구현체입니다.
 */
class HazelcastNearCacheManager(
    private val classLoader: ClassLoader,
    private val cacheProvider: CachingProvider,
    private val properties: Properties?,
    private val uri: URI?,
): CacheManager {

    private val caches = ConcurrentHashMap<String, NearCache<*, *>>()
    private val closed = AtomicBoolean(false)
    private val lock = ReentrantLock()

    override fun getCachingProvider(): CachingProvider = cacheProvider

    override fun getURI(): URI? = uri

    override fun getClassLoader(): ClassLoader = classLoader

    override fun getProperties(): Properties? = properties

    private fun checkNotClosed() {
        check(!isClosed) { "HazelcastNearCacheManager is closed." }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any, C: Configuration<K, V>> createCache(
        cacheName: String,
        configuration: C,
    ): Cache<K, V> {
        checkNotClosed()

        val nearCacheConfig = configuration as? HazelcastNearCacheConfig<K, V>
        check(nearCacheConfig != null) { "configuration is not HazelcastNearCacheConfig type." }

        val backCacheManager = jcacheManagerOf("com.hazelcast.cache.HazelcastCachingProvider")
        val backCache = backCacheManager.getCache(cacheName, nearCacheConfig.keyType, nearCacheConfig.valueType)
            ?: backCacheManager.createCache(cacheName, nearCacheConfig.backCacheConfiguration)

        val nearCacheCfg = nearCacheConfig as? NearCacheConfig<K, V> ?: NearCacheConfig()
        val nearCache = NearCache(nearCacheCfg, backCache)

        val oldCache = caches.putIfAbsent(cacheName, nearCache)
        if (oldCache != null) {
            throw CacheException("Cache [$cacheName] already exists")
        }
        return nearCache
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(cacheName: String?, keyType: Class<K>?, valueType: Class<V>?): Cache<K, V>? {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")
        keyType.requireNotNull("keyType")
        valueType.requireNotNull("valueType")
        return caches[cacheName] as? NearCache<K, V>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(cacheName: String?): Cache<K, V>? {
        checkNotClosed()
        return getCache(cacheName, Any::class.java, Any::class.java) as? NearCache<K, V>
    }

    override fun getCacheNames(): MutableIterable<String> = caches.keys.toMutableSet()

    override fun destroyCache(cacheName: String?) {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        caches.remove(cacheName)?.let {
            it.clearAllCache()
            it.close()
        }
    }

    override fun enableManagement(cacheName: String, enabled: Boolean) {
        // TODO 향후 구현
    }

    override fun enableStatistics(cacheName: String?, enabled: Boolean) {
        // TODO 향후 구현
    }

    override fun close() {
        if (isClosed) return
        lock.withLock {
            if (!isClosed) {
                cacheProvider.close(uri, classLoader)
                caches.values.forEach { runCatching { it.close() } }
                closed.set(true)
            }
        }
    }

    override fun isClosed(): Boolean = closed.get()

    override fun <T: Any> unwrap(clazz: Class<T>): T? {
        if (clazz.isAssignableFrom(javaClass)) {
            return clazz.cast(this)
        }
        throw IllegalArgumentException("Can't cast to $clazz")
    }
}
