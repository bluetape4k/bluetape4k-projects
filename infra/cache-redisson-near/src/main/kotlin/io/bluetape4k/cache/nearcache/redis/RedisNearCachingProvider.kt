package io.bluetape4k.cache.nearcache.redis

import com.fasterxml.jackson.core.JsonProcessingException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.redisson.Redisson
import org.redisson.config.Config
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.cache.CacheException
import javax.cache.CacheManager
import javax.cache.configuration.OptionalFeature
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

/**
 * [NearCache]를 제공하는 JCache [CachingProvider]의 구현체입니다.
 */
class RedisNearCachingProvider: CachingProvider {

    companion object: KLogging() {
        private const val DEFAULT_URI_PATH = "jsr107-default-config"
        private var defaultUri: URI = URI(DEFAULT_URI_PATH)

        private const val DEFAULT_REDISSON_JCACHE_CONFIG_YAML = "/redisson-jcache.yaml"
        private const val DEFAULT_REDISSON_JCACHE_CONFIG_JSON = "/redisson-jcache.json"
    }

    private val managers = ConcurrentHashMap<ClassLoader, MutableMap<URI, RedisNearCacheManager>>()
    private val lock = ReentrantLock()

    /**
     * Requests a [CacheManager] configured according to the implementation
     * specific [URI] be made available that uses the provided
     * [ClassLoader] for loading underlying classes.
     *
     *
     * Multiple calls to this method with the same [URI] and
     * [ClassLoader] must return the same [CacheManager] instance,
     * except if a previously returned [CacheManager] has been closed.
     *
     *
     * Properties are used in construction of a [CacheManager] and do not form
     * part of the identity of the CacheManager. i.e. if a second call is made to
     * with the same [URI] and [ClassLoader] but different properties,
     * the [CacheManager] created in the first call is returned.
     *
     *
     * Properties names follow the same scheme as package names.
     * The prefixes `java` and `javax` are reserved.
     * Properties are passed through and can be retrieved via
     * [CacheManager.getProperties].
     * Properties within the package scope of a caching implementation may be used for
     * additional configuration.
     *
     * @param uri         an implementation specific URI for the
     * [CacheManager] (null means use
     * [.getDefaultURI])
     * @param classLoader the [ClassLoader]  to use for the
     * [CacheManager] (null means use
     * [.getDefaultClassLoader])
     * @param properties  the [Properties] for the [CachingProvider]
     * to create the [CacheManager] (null means no
     * implementation specific Properties are required)
     * @throws CacheException    when a [CacheManager] for the
     * specified arguments could not be produced
     * @throws SecurityException when the operation could not be performed
     * due to the current security settings
     */
    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?, properties: Properties?): CacheManager? {
        val cacheUri = uri ?: defaultUri
        val cacheClassLoader = classLoader ?: defaultClassLoader

        log.debug { "Get NearCacheManager ..." }

        val uri2manager = managers.computeIfAbsent(cacheClassLoader) { ConcurrentHashMap() }
        var manager = uri2manager[cacheUri]
        if (manager != null) {
            log.info { "Get NearCacheManager. manager=$manager" }
            return manager
        }

        val redisson = loadConfig(cacheUri)?.let { Redisson.create(it) as Redisson }

        log.debug { "Create NearCacheManager. redisson=$redisson, classLoader=$cacheClassLoader, properties=$properties, uri=$uri" }
        manager = RedisNearCacheManager(redisson, cacheClassLoader, this, properties, cacheUri)
        uri2manager.putIfAbsent(cacheUri, manager)?.let { oldManager ->
            redisson?.run { shutdown() }
            manager = oldManager
        }
        return manager
    }

    private fun loadConfig(uri: URI): Config? {
        log.debug { "Load config for redisson jcache. uri=$uri" }
        var config: Config? = null
        try {
            val yamlUrl = when (DEFAULT_URI_PATH) {
                uri.path -> javaClass.getResource(DEFAULT_REDISSON_JCACHE_CONFIG_YAML)
                else -> uri.toURL()
            }
            yamlUrl?.let { config = Config.fromYAML(it) }
                ?: throw FileNotFoundException("/redisson-jcache.yaml")
        } catch (e: JsonProcessingException) {
            throw CacheException(e)
        } catch (e: IOException) {
            try {
                val jsonUrl = when (DEFAULT_URI_PATH) {
                    uri.path -> javaClass.getResource(DEFAULT_REDISSON_JCACHE_CONFIG_JSON)
                    else -> uri.toURL()
                }
                if (jsonUrl != null) {
                    config = Config.fromYAML(jsonUrl)
                }
            } catch (ex: IOException) {
                // skip
            }
        }
        return config
    }

    /**
     * Requests a [CacheManager] configured according to the implementation
     * specific [URI] that uses the provided [ClassLoader] for loading
     * underlying classes.
     *
     *
     * Multiple calls to this method with the same [URI] and
     * [ClassLoader] must return the same [CacheManager] instance,
     * except if a previously returned [CacheManager] has been closed.
     *
     * @param uri         an implementation specific [URI] for the
     * [CacheManager] (null means
     * use [.getDefaultURI])
     * @param classLoader the [ClassLoader]  to use for the
     * [CacheManager] (null means
     * use [.getDefaultClassLoader])
     * @throws CacheException    when a [CacheManager] for the
     * specified arguments could not be produced
     * @throws SecurityException when the operation could not be performed
     * due to the current security settings
     */
    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?): CacheManager? {
        return getCacheManager(uri, classLoader, defaultProperties)
    }

    /**
     * Requests a [CacheManager] configured according to the
     * [.getDefaultURI] and [.getDefaultProperties] be made
     * available that using the [.getDefaultClassLoader] for loading
     * underlying classes.
     *
     *
     * Multiple calls to this method must return the same [CacheManager]
     * instance, except if a previously returned [CacheManager] has been
     * closed.
     *
     * @throws SecurityException when the operation could not be performed
     * due to the current security settings
     */
    override fun getCacheManager(): CacheManager? {
        return getCacheManager(defaultURI, defaultClassLoader)
    }

    /**
     * Obtains the default [ClassLoader] that will be used by the
     * [CachingProvider].
     *
     * @return the default [ClassLoader] used by the [CachingProvider]
     */
    override fun getDefaultClassLoader(): ClassLoader = javaClass.classLoader

    /**
     * Obtains the default [URI] for the [CachingProvider].
     *
     *
     * Use this method to obtain a suitable [URI] for the
     * [CachingProvider].
     *
     * @return the default [URI] for the [CachingProvider]
     */
    override fun getDefaultURI(): URI = defaultUri

    /**
     * Obtains the default [Properties] for the [CachingProvider].
     *
     *
     * Use this method to obtain suitable [Properties] for the
     * [CachingProvider].
     *
     * @return the default [Properties] for the [CachingProvider]
     */
    override fun getDefaultProperties(): Properties = Properties()

    /**
     * Closes all of the [CacheManager] instances and associated resources
     * created and maintained by the [CachingProvider] across all
     * [ClassLoader]s.
     *
     *
     * After closing the [CachingProvider] will still be operational.  It
     * may still be used for acquiring [CacheManager] instances, though
     * those will now be new.
     *
     * @throws SecurityException when the operation could not be performed
     * due to the current security settings
     */
    override fun close() {
        lock.withLock {
            managers.keys.forEach {
                close(it)
            }
        }
    }

    /**
     * Closes all [CacheManager] instances and associated resources created
     * by the [CachingProvider] using the specified [ClassLoader].
     *
     *
     * After closing the [CachingProvider] will still be operational.  It
     * may still be used for acquiring [CacheManager] instances, though
     * those will now be new for the specified [ClassLoader] .
     *
     * @param classLoader the [ClassLoader]  to release
     * @throws SecurityException when the operation could not be performed
     * due to the current security settings
     */
    override fun close(classLoader: ClassLoader) {
        log.info { "Close RedisNearCachingProvider. classLoader=$classLoader" }

        managers.remove(classLoader)?.let { uri2manager ->
            uri2manager.values.forEach { manager ->
                runCatching { manager.close() }
            }
        }
    }

    /**
     * Closes all [CacheManager] instances and associated resources created
     * by the [CachingProvider] for the specified [URI] and
     * [ClassLoader].
     *
     * @param uri         the [URI] to release
     * @param classLoader the [ClassLoader]  to release
     * @throws SecurityException when the operation could not be performed
     * due to the current security settings
     */
    override fun close(uri: URI, classLoader: ClassLoader) {
        log.info { "Close RedisNearCachingProvider. uri=$uri, classLoader=$classLoader" }

        managers[classLoader]?.let { uri2manager ->
            uri2manager.remove(uri)?.let { manager ->
                runCatching { manager.close() }
            }
            if (uri2manager.isEmpty()) {
                managers.remove(classLoader)
            }
        }
    }

    /**
     * Determines whether an optional feature is supported by the
     * [CachingProvider].
     *
     * @param optionalFeature the feature to check for
     * @return true if the feature is supported
     */
    override fun isSupported(optionalFeature: OptionalFeature?): Boolean {
        return false
    }
}
