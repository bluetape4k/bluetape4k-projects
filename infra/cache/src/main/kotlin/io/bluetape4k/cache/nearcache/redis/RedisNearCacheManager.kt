package io.bluetape4k.cache.nearcache.redis

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.nearcache.NearCache
import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.cache.nearcache.management.EmptyNearCacheStatisticsMXBean
import io.bluetape4k.cache.nearcache.management.NearCacheManagementMXBean
import io.bluetape4k.cache.nearcache.management.NearCacheStatisticsMXBean
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotNull
import org.redisson.Redisson
import org.redisson.jcache.configuration.JCacheConfiguration
import org.redisson.jcache.configuration.RedissonConfiguration
import java.lang.management.ManagementFactory
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.CacheException
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.management.CacheStatisticsMXBean
import javax.cache.spi.CachingProvider
import javax.management.MBeanServer
import javax.management.ObjectName
import kotlin.concurrent.withLock

/**
 * [NearCache]를 위한 JCache 용 [CacheManager] 구현체입니다.
 *
 * @property redisson       [Redisson] instance
 * @property classLoader    [ClassLoader] instance
 * @property cacheProvider  [CachingProvider] 구현체인 [RedisNearCachingProvider] 인스턴스
 * @property properties     [NearCache]용 속성 정보
 * @property uri            환경설정 정보를 가진 리소스의 경로
 */
class RedisNearCacheManager(
    private val redisson: Redisson?,
    private val classLoader: ClassLoader,
    val cacheProvider: CachingProvider,
    private val properties: Properties?,
    private val uri: URI?,
): CacheManager {

    companion object: KLogging() {
        private val EMPTY_INSTANCE: EmptyNearCacheStatisticsMXBean = EmptyNearCacheStatisticsMXBean()
        private val mBeanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer()
    }

    private val caches = ConcurrentHashMap<String, NearCache<*, *>>()
    private val statBeans = ConcurrentHashMap<NearCache<*, *>, NearCacheStatisticsMXBean>()
    private val managementBeans = ConcurrentHashMap<NearCache<*, *>, NearCacheManagementMXBean>()

    private val closed = AtomicBoolean(false)
    private val lock = ReentrantLock()

//    @Volatile
//    private var closed: Boolean = false

    /**
     * Get the [CachingProvider] that created and is responsible for
     * the [CacheManager].
     *
     * @return the CachingProvider or `null` if the [CacheManager]
     * was created without using a [CachingProvider]
     */
    override fun getCachingProvider(): CachingProvider = cacheProvider

    /**
     * Get the URI of the [CacheManager].
     *
     * @return the URI of the [CacheManager]
     */
    override fun getURI(): URI? = uri

    /**
     * Get the [ClassLoader] used by the [CacheManager].
     *
     * @return  the [ClassLoader] used by the [CacheManager]
     */
    override fun getClassLoader(): ClassLoader = classLoader

    /**
     * Get the [Properties] that were used to create this
     * [CacheManager].
     *
     *
     * Implementations are not required to re-configure the
     * [CacheManager] should modifications to the returned
     * [Properties] be made.
     *
     * @return the Properties used to create the [CacheManager]
     */
    override fun getProperties(): Properties? = properties

    private fun checkNotClosed() {
        if (isClosed) {
            error("NearCacheManager is closed.")
        }
    }

    /**
     * Creates a named [Cache] at runtime.
     *
     *
     * If a [Cache] with the specified name is known to the [ ], a CacheException is thrown.
     *
     *
     * If a [Cache] with the specified name is unknown the [ ], one is created according to the provided [Configuration]
     * after which it becomes managed by the [CacheManager].
     *
     *
     * Prior to a [Cache] being created, the provided [Configuration]s is
     * validated within the context of the [CacheManager] properties and
     * implementation.
     *
     *
     * Implementers should be aware that the [Configuration] may be used to
     * configure other [Cache]s.
     *
     *
     * There's no requirement on the part of a developer to call this method for
     * each [Cache] an application may use.  Implementations may support
     * the use of declarative mechanisms to pre-configure [Cache]s, thus
     * removing the requirement to configure them in an application.  In such
     * circumstances a developer may simply call either the
     * [.getCache] or [.getCache]
     * methods to acquire a previously established or pre-configured [Cache].
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param <C> the type of the Configuration
     * @param cacheName     the name of the [Cache]. Names should not use
     * forward slashes(/) or colons(:), or start with
     * java. or javax. These prefixes are reserved.
     * @param configuration a [RedissonConfiguration] or [RedisNearCacheConfig] for the [Cache]
     * @throws IllegalStateException         if the [CacheManager]
     * [.isClosed]
     * @throws CacheException                if there was an error configuring the
     * [Cache], which includes trying
     * to create a cache that already exists.
     * @throws IllegalArgumentException      if the configuration is invalid
     * @throws UnsupportedOperationException if the configuration specifies
     * an unsupported feature
     * @throws NullPointerException          if the cache configuration or name
     * is null
     * @throws SecurityException             when the operation could not be performed
     * due to the current security settings
    </C></V></K> */
    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any, C: Configuration<K, V>> createCache(
        cacheName: String,
        configuration: C,
    ): Cache<K, V> {
        checkNotClosed()
        //        cacheName.requireNotBlank("cacheName")
        //        configuration.requireNotNull("configuration")

        log.info { "Create RedisNearCache. cacheName=$cacheName, configuration=$configuration " }

        when (configuration) {
            is RedisNearCacheConfig<*, *> -> {

            }
        }
        val nearCacheConfig = configuration as? RedisNearCacheConfig<K, V>
        check(nearCacheConfig != null) { "configuration is not RedisNearCacheConfig type." }

        val redissonConfiguration = nearCacheConfig.redissonConfig
        var cacheRedisson = redisson
        redissonConfiguration?.let {
            cacheRedisson = if (it.config != null) {
                Redisson.create(it.config) as Redisson
            } else {
                it.redisson as? Redisson
            }
        }

        check(cacheRedisson != null)
        check(redissonConfiguration != null)

        log.debug { "Create backCache using Redis" }
        val backCache =
            JCaching.Redisson.getOrCreateCache(cacheName, cacheRedisson, redissonConfiguration.jcacheConfig)

        log.debug { "Create new NearCache. cacheName=$cacheName" }
        val nearCacheCfg = nearCacheConfig as? NearCacheConfig<K, V> ?: NearCacheConfig()
        val nearCache = NearCache(nearCacheCfg, backCache)

        val oldCache = caches.putIfAbsent(cacheName, nearCache)
        if (oldCache != null) {
            throw CacheException("Cache [$cacheName] already exists")
        }

        val cfg = JCacheConfiguration(redissonConfiguration)
        if (cfg.isStatisticsEnabled) {
            enableStatistics(cacheName, true)
        }
        if (cfg.isManagementEnabled) {
            enableManagement(cacheName, true)
        }

        return nearCache
    }

    /**
     * Looks up a managed [Cache] given its name.
     *
     *
     * Use this method to check runtime key and value types.
     *
     *
     * Use [.getCache] where this check is not required.
     *
     *
     * Implementations must ensure that the key and value types are the same as
     * those configured for the [Cache] prior to returning from this method.
     *
     *
     * Implementations may further perform type checking on mutative cache operations
     * and throw a [ClassCastException] if these checks fail.
     *
     *
     * Implementations that support declarative mechanisms for pre-configuring
     * [Cache]s may return a pre-configured [Cache] instead of
     * `null`.
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param cacheName the name of the managed [Cache] to acquire
     * @param keyType   the expected [Class] of the key
     * @param valueType the expected [Class] of the value
     * @return the Cache or null if it does exist or can't be pre-configured
     * @throws IllegalStateException    if the [CacheManager]
     * is [.isClosed]
     * @throws ClassCastException       if the specified key and/or value types are
     * incompatible with the configured cache.
     * @throws NullPointerException     if either keyType or classType is null.
     * @throws SecurityException        when the operation could not be performed
     * due to the current security settings
    </V></K> */
    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(cacheName: String?, keyType: Class<K>?, valueType: Class<V>?): Cache<K, V>? {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")
        keyType.requireNotNull("keyType")
        valueType.requireNotNull("valueType")

        log.debug { "Get NearCache. cache name=$cacheName, keyType=$keyType, valueType=$valueType" }

        val nearCache: NearCache<K, V> = caches[cacheName] as? NearCache<K, V> ?: return null

        // TODO: 향후 지원 예정
        //        val clazz = CompleteConfiguration::class.java as Class<CompleteConfiguration<K, V>>
        //        if (!keyType.isAssignableFrom(nearCache.getConfiguration(clazz).keyType)) {
        //            throw ClassCastException("Wrong type of key for $cacheName")
        //        }
        //        if (!valueType.isAssignableFrom(nearCache.getConfiguration(clazz).valueType)) {
        //            throw ClassCastException("Wrong type of value for $cacheName")
        //        }
        return nearCache
    }

    /**
     * Looks up a managed [Cache] given its name.
     *
     *
     * This method may only be used to acquire [Cache]s that were
     * configured without runtime key and value types, or were configured
     * to use Object.class key and value types.
     *
     *
     * Use the [.getCache] method to acquire
     * [Cache]s with a check that the supplied key and value type parameters
     * match the runtime types.
     *
     *
     * Implementations that support declarative mechanisms for pre-configuring
     * [Cache]s may return a pre-configured [Cache] instead of
     * `null`.
     *
     * @param <K> the type of key
     * @param <V> the type of value
     * @param cacheName the name of the cache to look for
     * @return the Cache or null if it does exist or can't be pre-configured
     * @throws IllegalStateException    if the CacheManager is [.isClosed]
     * @throws SecurityException        when the operation could not be performed
     * due to the current security settings
     * @see .getCache
    </V></K> */
    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(cacheName: String?): Cache<K, V>? {
        checkNotClosed()
        return getCache(cacheName, Any::class.java, Any::class.java) as? NearCache<K, V>
    }

    /**
     * Obtains an [Iterable] over the names of [Cache]s managed by the
     * [CacheManager].
     *
     *
     * [java.util.Iterator]s returned by the [Iterable] are immutable.
     * If the [Cache]s managed by the [CacheManager] change,
     * the [Iterable] and associated [java.util.Iterator]s are not
     * affected.
     *
     *
     * [java.util.Iterator]s returned by the [Iterable] may not provide
     * all of the [Cache]s managed by the [CacheManager].  For example:
     * Internally defined or platform specific [Cache]s that may be accessible
     * by a call to [.getCache] or [.getCache] may not be present in an iteration.
     *
     * @return an [Iterable] over the names of managed [Cache]s.
     * @throws IllegalStateException if the [CacheManager]
     * is [.isClosed]
     * @throws SecurityException     when the operation could not be performed
     * due to the current security settings
     */
    override fun getCacheNames(): MutableIterable<String> = caches.keys.toMutableSet()

    /**
     * Destroys a specifically named and managed [Cache].  Once destroyed
     * a new [Cache] of the same name but with a different [ ] may be configured.
     *
     *
     * This is equivalent to the following sequence of method calls:
     *
     *  1. [Cache.clear]
     *  1. [Cache.close]
     *
     * followed by allowing the name of the [Cache] to be used for other
     * [Cache] configurations.
     *
     *
     * From the time this method is called, the specified [Cache] is not
     * available for operational use. An attempt to call an operational method on
     * the [Cache] will throw an [IllegalStateException].
     *
     * @param cacheName the cache to destroy
     * @throws IllegalStateException if the [CacheManager]
     * [.isClosed]
     * @throws NullPointerException  if cacheName is null
     * @throws SecurityException     when the operation could not be performed
     * due to the current security settings
     */
    override fun destroyCache(cacheName: String?) {
        log.debug { "Destroy NearCache... cacheName=$cacheName" }
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        caches[cacheName]?.let { cache ->
            log.info { "Destroy NearCache [$cacheName]" }
            cache.clearAllCache()
            cache.close()
        }
    }

    fun closeCache(cache: NearCache<*, *>) {
        caches.remove(cache.name)
        unregisterStatisticsBean(cache)
        unregisterManagementBean(cache)
    }

    /**
     * Controls whether management is enabled. If enabled the [CacheMXBean]
     * for each cache is registered in the platform MBean server. The platform
     * MBeanServer is obtained using
     * [ManagementFactory.getPlatformMBeanServer].
     *
     *
     * Management information includes the name and configuration information for
     * the cache.
     *
     *
     * Each cache's management object must be registered with an ObjectName that
     * is unique and has the following type and attributes:
     *
     *
     * Type:
     * `javax.cache:type=CacheConfiguration`
     *
     *
     * Required Attributes:
     *
     *  * CacheManager the URI of the CacheManager
     *  * Cache the name of the Cache
     *
     *
     * @param cacheName the name of the cache to register
     * @param enabled   true to enable management, false to disable.
     * @throws IllegalStateException if the [CacheManager] or
     * [Cache] [.isClosed]
     * @throws SecurityException     when the operation could not be performed
     * due to the current security settings
     */
    override fun enableManagement(cacheName: String, enabled: Boolean) {
        log.info { "현재는 지원하지 않습니다." }
    }

    private fun queryName(baseName: String, cache: NearCache<*, *>): ObjectName {
        val name = getName(baseName, cache)
        return ObjectName(name)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun unregisterManagementBean(cache: NearCache<*, *>) {
        log.info { "현재는 지원하지 않습니다." }
    }

    fun getStatBean(cache: NearCache<*, *>): CacheStatisticsMXBean {
        return statBeans[cache] ?: EMPTY_INSTANCE
    }

    private fun getName(name: String, cache: NearCache<*, *>): String {
        return "javax.cache:type=NearCache$name" +
                ",CacheManager=${cache.cacheManager.uri.toString().replace(",|:|=|\n", ".")}" +
                ",Cache=${cache.name.replace(",|:|=|\n", ".")}"
    }

    /**
     * Enables or disables statistics gathering for a managed [Cache] at
     * runtime.
     *
     *
     * Each cache's statistics object must be registered with an ObjectName that
     * is unique and has the following type and attributes:
     *
     *
     * Type:
     * `javax.cache:type=CacheStatistics`
     *
     *
     * Required Attributes:
     *
     *  * CacheManager the URI of the CacheManager
     *  * Cache the name of the Cache
     *
     *
     * @param cacheName the name of the cache to register
     * @param enabled   true to enable statistics, false to disable.
     * @throws IllegalStateException if the [CacheManager] or
     * [Cache] [.isClosed]
     * @throws NullPointerException  if cacheName is null
     * @throws SecurityException when the operation could not be performed
     * due to the current security settings
     */
    override fun enableStatistics(cacheName: String?, enabled: Boolean) {
        log.info { "현재는 지원하지 않습니다." }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun unregisterStatisticsBean(cache: NearCache<*, *>) {
        log.info { "현재는 지원하지 않습니다." }
    }

    /**
     * Closes the [CacheManager].
     *
     *
     * For each [Cache] managed by the [CacheManager], the
     * [Cache.close] method will be invoked, in no guaranteed order.
     *
     *
     * If a [Cache.close] call throws an exception, the exception will be
     * ignored.
     *
     *
     * After executing this method, the [.isClosed] method will return
     * `true`.
     *
     *
     * All attempts to close a previously closed [CacheManager] will be
     * ignored.
     *
     * Closing a CacheManager does not necessarily destroy the contents of the
     * Caches in the CacheManager.
     *
     *
     * It simply signals that the CacheManager is no longer required by the application
     * and that future uses of a specific CacheManager instance should not be permitted.
     *
     *
     * Depending on the implementation and Cache topology,
     * (e.g. a storage-backed or distributed cache), the contents of closed Caches
     * previously referenced by the CacheManager, may still be available and accessible
     * by other applications.
     *
     * @throws SecurityException when the operation could not be performed due to the
     * current security settings
     */
    override fun close() {
        if (isClosed) {
            return
        }
        lock.withLock {
            if (!isClosed) {
                log.info { "Close NearCacheManager." }

                cacheProvider.close(uri, classLoader)

                caches.values.forEach { cache ->
                    runCatching { cache.close() }
                }
                log.debug { "Shutdown redisson instance. redisson=$redisson" }
                redisson?.shutdown()
                closed.set(true)
            }
        }
    }

    /**
     * Determines whether the [CacheManager] instance has been closed. A
     * [CacheManager] is considered closed if;
     *
     *  1. the [.close] method has been called
     *  1. the associated [.getCachingProvider] has been closed, or
     *  1. the [CacheManager] has been closed using the associated
     * [.getCachingProvider]
     *
     *
     *
     * This method generally cannot be called to determine whether the
     * [CacheManager] is valid or invalid. A typical client can determine
     * that a [CacheManager] is invalid by catching any exceptions that
     * might be thrown when an operation is attempted.
     *
     * @return true if this [CacheManager] instance is closed; false if it
     * is still open
     */
    override fun isClosed(): Boolean = closed.get()

    /**
     * Provides a standard mechanism to access the underlying concrete caching
     * implementation to provide access to further, proprietary features.
     *
     *
     * If the provider's implementation does not support the specified class,
     * the [IllegalArgumentException] is thrown.
     *
     * @param <T> the type of the underlying [CacheManager]
     * @param clazz the proprietary class or interface of the underlying concrete
     * [CacheManager]. It is this type that is returned.
     * @return an instance of the underlying concrete [CacheManager]
     * @throws IllegalArgumentException if the caching provider doesn't support the
     * specified class.
     * @throws SecurityException        when the operation could not be performed
     * due to the current security settings
    </T> */
    override fun <T: Any> unwrap(clazz: Class<T>): T? {
        if (clazz.isAssignableFrom(javaClass)) {
            return clazz.cast(this)
        }
        throw IllegalArgumentException("Can't cast to $clazz")
    }
}
