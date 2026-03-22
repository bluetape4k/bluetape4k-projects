package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.redis.lettuce.RedisCommandSupports
import io.bluetape4k.redis.lettuce.map.LettuceMap
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.atomicfu.atomic
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.CacheException
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.spi.CachingProvider
import kotlin.concurrent.withLock

/**
 * Lettuce 기반 [javax.cache.CacheManager] 구현체입니다.
 *
 * ## 동작/계약
 * - `createCache`는 [LettuceCacheConfig]를 받아 [LettuceJCache]를 생성합니다.
 * - 캐시 인스턴스는 내부 맵에 이름 기준으로 보관되며 중복 생성 시 [javax.cache.CacheException]이 발생합니다.
 * - 매니저가 닫히면 공개 API 대부분이 `IllegalStateException`을 발생시킵니다.
 */
class LettuceCacheManager(
    private val redisClient: RedisClient,
    private val classLoader: ClassLoader,
    val cacheProvider: CachingProvider,
    private val properties: Properties?,
    private val uri: URI?,
    private val closeResource: () -> Unit = {},
): CacheManager {

    companion object: KLogging() {
        /**
         * String key + ByteArray value 코덱
         */
        val STRING_BYTES_CODEC: RedisCodec<String, ByteArray> =
            RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
    }

    private val caches = ConcurrentHashMap<String, LettuceJCache<*, *>>()
    private val closed = atomic(false)
    private val lock = ReentrantLock()

    private val supportsHSetEx: Boolean by lazy {
        RedisCommandSupports.supportsHSetEx(redisClient)
    }

    private fun checkNotClosed() {
        check(!closed.value) { "LettuceCacheManager가 닫혀 있습니다." }
    }

    override fun getCachingProvider(): CachingProvider = cacheProvider

    override fun getURI(): URI? = uri

    override fun getClassLoader(): ClassLoader = classLoader

    override fun getProperties(): Properties? = properties

    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any, C: Configuration<K, V>> createCache(
        cacheName: String,
        configuration: C,
    ): Cache<K, V> {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")

        log.info { "Create LettuceCache. cacheName=$cacheName, configuration=$configuration" }

        if (caches.containsKey(cacheName)) {
            throw CacheException("Cache [$cacheName]이 이미 존재합니다.")
        }

        val lettuceCfg = configuration as? LettuceCacheConfig<K, V>

        val ttlSeconds = lettuceCfg?.ttlSeconds
        val keyCodec = lettuceCfg?.keyCodec ?: { k: K -> k.toString() }
        val keyDecoder = lettuceCfg?.keyDecoder
        val codec: LettuceBinaryCodec<*> = lettuceCfg?.codec ?: LettuceBinaryCodecs.lz4Fory<Any>()

        log.debug { "RedisClient 연결 생성. cacheName=$cacheName" }
        val connection = redisClient.connect(STRING_BYTES_CODEC)
        val map = LettuceMap<ByteArray>(connection, cacheName, supportsHSetEx = supportsHSetEx)

        val cache = LettuceJCache(
            map = map,
            keyCodec = keyCodec,
            keyDecoder = keyDecoder,
            codec = codec,
            ttlSeconds = ttlSeconds,
            cacheManager = this,
            configuration = configuration,
            closeResource = { connection.close() },
        )

        val oldCache = caches.putIfAbsent(cacheName, cache)
        if (oldCache != null) {
            connection.close()
            throw CacheException("Cache [$cacheName]이 이미 존재합니다.")
        }

        return cache
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(cacheName: String?, keyType: Class<K>?, valueType: Class<V>?): Cache<K, V>? {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")
        log.debug { "Get LettuceCache. cacheName=$cacheName, keyType=$keyType, valueType=$valueType" }
        return caches[cacheName] as? LettuceJCache<K, V>
    }

    @Suppress("UNCHECKED_CAST")
    override fun <K: Any, V: Any> getCache(cacheName: String?): Cache<K, V>? {
        checkNotClosed()
        return caches[cacheName] as? LettuceJCache<K, V>
    }

    override fun getCacheNames(): MutableIterable<String> = caches.keys.toMutableSet()

    override fun destroyCache(cacheName: String?) {
        checkNotClosed()
        cacheName.requireNotBlank("cacheName")
        log.debug { "Destroy LettuceCache. cacheName=$cacheName" }
        caches.remove(cacheName)?.let { cache ->
            log.info { "Destroy LettuceCache [$cacheName]" }
            runCatching { cache.clear() }
            runCatching { cache.close() }
        }
    }

    /**
     * 캐시 이름을 기준으로 내부 맵에서 제거합니다. [LettuceJCache.close] 내부에서 호출됩니다.
     */
    fun closeCache(cache: LettuceJCache<*, *>) {
        caches.remove(cache.name)
    }

    override fun enableManagement(cacheName: String, enabled: Boolean) {
        log.info { "enableManagement는 현재 지원하지 않습니다." }
    }

    override fun enableStatistics(cacheName: String?, enabled: Boolean) {
        log.info { "enableStatistics는 현재 지원하지 않습니다." }
    }

    override fun close() {
        if (closed.value) return
        lock.withLock {
            if (!closed.value) {
                // 재귀 진입 방지를 위해 closed를 먼저 true로 설정
                closed.value = true
                log.info { "Close LettuceCacheManager." }
                if (uri != null) {
                    runCatching { cacheProvider.close(uri, classLoader) }
                }
                caches.values.forEach { cache ->
                    runCatching { cache.close() }
                }
                runCatching { closeResource() }
            }
        }
    }

    override fun isClosed(): Boolean = closed.value

    override fun <T: Any> unwrap(clazz: Class<T>): T {
        if (clazz.isAssignableFrom(javaClass)) return clazz.cast(this)
        throw IllegalArgumentException("Can't unwrap to $clazz")
    }
}
