package io.bluetape4k.cache.jcache

import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import org.slf4j.Logger
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.Caching
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.EternalExpiryPolicy
import javax.cache.expiry.ExpiryPolicy
import javax.cache.integration.CacheLoader
import javax.cache.integration.CacheWriter
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

private val log: Logger by lazy { KotlinLogging.logger {} }

@PublishedApi
internal val jcacheLock = ReentrantLock()

/**
 * JCache Configuration을 빌드합니다.
 *
 * @param K     key type
 * @param V     value type
 * @param builder Jcache configuration setup block
 * @return [CompleteConfiguration] instance
 */
inline fun <reified K, reified V> jcacheConfiguration(
    @BuilderInference builder: MutableConfiguration<K, V>.() -> Unit,
): MutableConfiguration<K, V> {
    return MutableConfiguration<K, V>()
        .apply { setTypes(K::class.java, V::class.java) }
        .apply(builder)
}

/**
 * JCache Configuration 을 빌드합니다.
 *
 * @param K     key type
 * @param V     value type
 * @param cacheLoaderFactory cache loader factory (default: null)
 * @param cacheWriterFactory cache writer factory (default: null)
 * @param isReadThrough read through flag (default: false)
 * @param isWriteThrough write through flag (default: false)
 * @param isStoreByValue store by value flag (default: false)
 * @param isStatisticsEnabled statistics enabled flag (default: false)
 * @param isManagementEnabled management enabled flag (default: false)
 * @param expiryPolicyFactory expiry policy factory
 * @return [CompleteConfiguration] instance
 */
inline fun <reified K, reified V> jcacheConfigurationOf(
    cacheLoaderFactory: Factory<CacheLoader<K, V>>? = null,
    cacheWriterFactory: Factory<CacheWriter<in K, in V>>? = null,
    isReadThrough: Boolean = false,
    isWriteThrough: Boolean = false,
    isStoreByValue: Boolean = false,
    isStatisticsEnabled: Boolean = false,
    isManagementEnabled: Boolean = false,
    noinline expiryPolicyFactory: (() -> Factory<out ExpiryPolicy>)? = { EternalExpiryPolicy.factoryOf() },
): CompleteConfiguration<K, V> = jcacheConfiguration {
    setTypes(K::class.java, V::class.java)
    cacheLoaderFactory?.let { this.setCacheLoaderFactory(it) }
    cacheWriterFactory?.let { this.setCacheWriterFactory(it) }
    this.isReadThrough = isReadThrough
    this.isWriteThrough = isWriteThrough
    this.isStoreByValue = isStoreByValue
    this.isStatisticsEnabled = isStatisticsEnabled
    this.isManagementEnabled = isManagementEnabled
    expiryPolicyFactory?.let { setExpiryPolicyFactory(it.invoke()) }
}

/**
 * JCache에 대한 기본 설정
 */
inline fun <reified K, reified V> getDefaultJCacheConfiguration(): CompleteConfiguration<K, V> =
    jcacheConfiguration {
        setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
    }

/**
 * 원하는 수형의 [CachingProvider] 를 로드합니다.
 *
 * ```
 * val provider = jcachingProvider<CaffeineCachingProvider>()
 * val provider = jcachingProvider<EhcacheCachingProvider>()
 * val provider = jcachingProvider<RedissonCachingProvider>()
 * ```
 *
 * @param P CachingProvider type
 * @return CachingProvider instance
 */
inline fun <reified P: CachingProvider> jcachingProvider(): CachingProvider =
    Caching.getCachingProvider(P::class.qualifiedName)

/**
 * 지정한 [qualifiedName]에 해당하는 [CachingProvider]를 로드합니다.
 *
 * @param qualifiedName CachingProvider qualified name
 * @return [CachingProvider] instance
 */
fun jcachingProviderOf(qualifiedName: String): CachingProvider {
    qualifiedName.requireNotBlank("qualifiedName")
    return Caching.getCachingProvider(qualifiedName)
}

/**
 * 지정한 [CachingProvider]의 [CacheManager]를 가져옵니다.
 *
 * ```
 * val cacheManager = jcacheManager<CaffeineCachingProvider>()
 * val cacheManager = jcacheManager<EhcacheCachingProvider>()
 * val cacheManager = jcacheManager<RedissonCachingProvider>()
 * ```
 *
 * @return [CacheManager] instance
 */
inline fun <reified P: CachingProvider> jcacheManager(): CacheManager {
    return jcacheLock.withLock {
        jcachingProvider<P>().cacheManager
    }
}

/**
 * 지정한 [qualifiedName]에 해당하는 [CachingProvider]의 [CacheManager]를 가져옵니다.
 *
 * ```
 * val cacheManager = jcacheManager("com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider")
 * val cacheManager = jcacheManager("org.cache2k.jcache.provider.Cache2kCachingProvider")
 * val cacheManager = jcacheManager("org.ehcache.jsr107.EhcacheCachingProvider")
 * val cacheManager = jcacheManager("org.redisson.jcache.JCacheProvider")
 * ```
 *
 * @param qualifiedName CachingProvider qualified name
 * @return [CacheManager] instance
 */
fun jcacheManagerOf(qualifiedName: String): CacheManager {
    return jcacheLock.withLock {
        jcachingProviderOf(qualifiedName).cacheManager
    }
}

/**
 * [cacheName]에 해당하는 cache를 가져옵니다.
 *
 * ```
 * val cache = cacheManager.getCache("my-cache", String::class.java, Any::class.java)
 * ```
 *
 * @param cacheName  cache name
 * @return cache instance
 */
inline fun <reified K, reified V> CacheManager.get(cacheName: String): Cache<K, V>? {
    cacheName.requireNotBlank("cacheName")
    return runCatching { getCache(cacheName, K::class.java, V::class.java) }.getOrNull()
}

/**
 * [cacheName]에 해당하는 [Cache]를 생성합니다. 기존에 존재하면, 예외를 발생시킵니다.
 *
 * ```
 * val cache = cacheManager.create("my-cache", String::class.java, Any::class.java)
 * ```
 *
 * @param cacheName cache name
 * @param configuration jcache configuration
 * @return jcache instance
 */
inline fun <reified K, reified V> CacheManager.create(
    cacheName: String,
    configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
): Cache<K, V> {
    cacheName.requireNotBlank("cacheName")
    return createCache(cacheName, configuration)
}

/**
 * [cacheName]을 가지는 Cache를 가져옵니다. 없다면 새로 생성해서 반환합니다.
 *
 * ```
 * val cache = cacheManager.getOrCreate<String, Any>("my-cache")
 * ```
 *
 * @param cacheName cache name
 * @param configuration  jcache configuration
 * @return jcache instance
 */
inline fun <reified K, reified V> CacheManager.getOrCreate(
    cacheName: String,
    configuration: Configuration<K, V> = getDefaultJCacheConfiguration(),
): Cache<K, V> {
    cacheName.requireNotBlank("cacheName")
    return jcacheLock.withLock {
        get(cacheName) ?: create(cacheName, configuration)
    }
}

/**
 * 캐시에서 값을 가져옵니다. 캐시에 값이 없다면, [valueSupplier]를 통해 값을 얻어, 캐싱한 후 반환합니다.
 *
 * ```
 * val cache = cacheManager.getOrCreate<String, Any>("my-cache")
 * val value = cache.getOrPut("key") { "value" }
 * ```
 *
 * @param key 캐시 키
 * @param valueSupplier 값 제공자
 * @return 캐시된 값
 */
inline fun <K, V> JCache<K, V>.getOrPut(key: K, valueSupplier: () -> V): V {
    if (!containsKey(key)) {
        put(key, valueSupplier())
    }
    return get(key)
}


/**
 * Cache 조회 시, `front cache` <- `back cache` 순서로 조회되도록 read through를 수행합니다.
 *
 * ```
 * val cache = cacheManager.getOrCreate<String, Any>("my-cache")
 * val cacheLoader = cache.cacheLoader()
 * cacheLoader.load("key")  // return value
 * cacheLoader.loadAll(setOf("key1", "key2")) // return mapOf("key1" to value1, "key2" to value2)
 * ```
 *
 * @param K key type
 * @param V value type
 * @return CacheLoader instance
 */
fun <K, V> JCache<K, V>.cacheLoader(): CacheLoader<K, V> {
    return object: CacheLoader<K, V> {
        override fun load(key: K): V? {
            log.debug { "Read through load cache entry. key=$key" }
            return get(key)
        }

        override fun loadAll(keys: MutableIterable<K>): Map<K, V> {
            log.debug { "Read through loadAll cache entries. keys=${keys.joinToString()}" }
            return getAll(keys.toUnifiedSet())
        }
    }
}

/**
 * Cache 저장 시, `front cache` -> `back cache` 순서로 적용되도록 write through를 수행합니다.
 */
@Suppress("UNCHECKED_CAST")
fun <K, V> JCache<K, V>.cacheWriter(): CacheWriter<K, V> {
    return object: CacheWriter<K, V> {
        override fun write(entry: Cache.Entry<out K, out V>) {
            log.debug { "Write through write cache entry. entry=$entry at $this" }
            put(entry.key, entry.value)
        }

        override fun writeAll(entries: MutableCollection<Cache.Entry<out K, out V>>) {
            log.debug { "Write through writeAll cache entries. entries=${entries.joinToString()} at $this" }
            putAll(entries.associate { it.key to it.value })
        }

        override fun delete(key: Any?) {
            log.debug { "Write through delete cache entry. key=$key  at $this" }
            val removeKey = key as? K
            remove(removeKey)
        }

        override fun deleteAll(keys: MutableCollection<*>) {
            log.debug { "Write through deleteAll cache entries. keys=$keys at $this" }
            removeAll(keys.mapNotNull { it as? K }.toUnifiedSet())
        }
    }
}

/**
 * Cache의 Configuration 정보를 가져온다
 *
 * @param K  key type
 * @param V  value type
 * @param C  cache configuration type (eg. CompleteConfiguration<K, V>)
 * @return cache configuration
 *
 * ```
 * val config = cache.getConfiguration<Any, Any, CompleteConfiguration<Any, Any>>()
 * ```
 */
@Suppress("UNCHECKED_CAST")
fun <K, V, C: Configuration<K, V>> JCache<K, V>.getConfiguration(): C {
    return getConfiguration(Configuration::class.java as Class<C>)
}
