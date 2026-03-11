package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import javax.cache.CacheManager
import javax.cache.configuration.OptionalFeature
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

/**
 * Lettuce Redis 기반 JCache [javax.cache.spi.CachingProvider] 구현체입니다.
 *
 * ## 동작/계약
 * - `(ClassLoader, URI)` 조합으로 [LettuceCacheManager]를 생성/재사용합니다.
 * - `close` 계열 호출은 등록된 매니저를 닫고 내부 맵에서 제거합니다.
 * - optional feature는 지원하지 않아 항상 `false`를 반환합니다.
 *
 * ## SPI 등록
 * `META-INF/services/javax.cache.spi.CachingProvider`에 이 클래스가 등록됩니다.
 *
 * ## Redis URI 설정
 * URI를 통해 Redis 연결 정보를 지정할 수 있습니다.
 * 기본값은 `redis://localhost:6379`입니다.
 */
class LettuceCachingProvider: CachingProvider {

    companion object: KLogging() {
        private const val DEFAULT_URI_PATH = "lettuce-jcache-default"
        private val defaultUri: URI = URI(DEFAULT_URI_PATH)
        private const val DEFAULT_REDIS_URI = "redis://localhost:6379"
    }

    private val managers = ConcurrentHashMap<ClassLoader, MutableMap<URI, LettuceCacheManager>>()
    private val lock = ReentrantLock()

    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?, properties: Properties?): CacheManager {
        val cacheUri = uri ?: defaultUri
        val cacheClassLoader = classLoader ?: defaultClassLoader

        log.debug { "Get LettuceCacheManager. uri=$cacheUri, classLoader=$cacheClassLoader" }

        val uri2manager = managers.computeIfAbsent(cacheClassLoader) { ConcurrentHashMap() }
        uri2manager[cacheUri]?.let { return it }

        val redisUri = if (cacheUri == defaultUri) {
            DEFAULT_REDIS_URI
        } else {
            cacheUri.toString()
        }

        log.debug { "Create RedisClient. redisUri=$redisUri" }
        val redisClient = RedisClient.create(RedisURI.create(redisUri))

        val manager = LettuceCacheManager(
            redisClient = redisClient,
            classLoader = cacheClassLoader,
            cacheProvider = this,
            properties = properties,
            uri = cacheUri,
            closeResource = { redisClient.shutdown() },
        )

        uri2manager.putIfAbsent(cacheUri, manager)?.let { existingManager ->
            redisClient.shutdown()
            return existingManager
        }

        log.info { "Created LettuceCacheManager. uri=$cacheUri" }
        return manager
    }

    override fun getCacheManager(uri: URI?, classLoader: ClassLoader?): CacheManager {
        return getCacheManager(uri, classLoader, defaultProperties)
    }

    override fun getCacheManager(): CacheManager {
        return getCacheManager(defaultURI, defaultClassLoader)
    }

    override fun getDefaultClassLoader(): ClassLoader = javaClass.classLoader

    override fun getDefaultURI(): URI = defaultUri

    override fun getDefaultProperties(): Properties = Properties()

    override fun close() {
        lock.withLock {
            managers.keys.toList().forEach { close(it) }
        }
    }

    override fun close(classLoader: ClassLoader) {
        log.info { "Close LettuceCachingProvider. classLoader=$classLoader" }
        managers.remove(classLoader)?.values?.forEach { manager ->
            runCatching { manager.close() }
        }
    }

    override fun close(uri: URI, classLoader: ClassLoader) {
        log.info { "Close LettuceCachingProvider. uri=$uri, classLoader=$classLoader" }
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
