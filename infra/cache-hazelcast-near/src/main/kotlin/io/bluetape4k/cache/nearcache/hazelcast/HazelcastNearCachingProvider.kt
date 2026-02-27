package io.bluetape4k.cache.nearcache.hazelcast

import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.cache.CacheManager
import javax.cache.configuration.OptionalFeature
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

/**
 * Hazelcast NearCache를 제공하는 JCache [CachingProvider] 구현체입니다.
 */
class HazelcastNearCachingProvider: CachingProvider {

    companion object {
        private const val DEFAULT_URI_PATH = "jsr107-default-config"
        private val defaultUri: URI = URI(DEFAULT_URI_PATH)
    }

    private val managers = ConcurrentHashMap<ClassLoader, MutableMap<URI, HazelcastNearCacheManager>>()
    private val lock = ReentrantLock()

    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?, properties: Properties?): CacheManager {
        val cacheUri = uri ?: defaultUri
        val cacheClassLoader = classLoader ?: defaultClassLoader

        val uri2manager = managers.computeIfAbsent(cacheClassLoader) { ConcurrentHashMap() }
        return uri2manager.computeIfAbsent(cacheUri) {
            HazelcastNearCacheManager(cacheClassLoader, this, properties, cacheUri)
        }
    }

    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?): CacheManager =
        getCacheManager(uri, classLoader, defaultProperties)

    override fun getCacheManager(): CacheManager =
        getCacheManager(defaultURI, defaultClassLoader)

    override fun getDefaultClassLoader(): ClassLoader = javaClass.classLoader

    override fun getDefaultURI(): URI = defaultUri

    override fun getDefaultProperties(): Properties = Properties()

    override fun close() {
        lock.withLock {
            managers.keys.forEach { close(it) }
        }
    }

    override fun close(classLoader: ClassLoader) {
        managers.remove(classLoader)?.values?.forEach { manager ->
            runCatching { manager.close() }
        }
    }

    override fun close(uri: URI, classLoader: ClassLoader) {
        managers[classLoader]?.let { uri2manager ->
            uri2manager.remove(uri)?.let { manager ->
                runCatching { manager.close() }
            }
            if (uri2manager.isEmpty()) {
                managers.remove(classLoader)
            }
        }
    }

    override fun isSupported(optionalFeature: OptionalFeature?): Boolean = false
}
