package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.lettuce.core.RedisClient
import javax.cache.configuration.Configuration

/**
 * Lettuce를 사용하는 [javax.cache.Cache]`<K, V>`를 제공하는 object입니다.
 *
 * ```kotlin
 * val cache = LettuceJCaching.getOrCreate<String, String>(redisClient, "my-cache")
 * cache.put("key", "value")
 * ```
 */
object LettuceJCaching: KLogging() {

    /**
     * 주어진 [RedisClient]로 [LettuceCacheManager]를 생성합니다.
     *
     * 외부에서 관리되는 [RedisClient]를 재사용하므로, 반환된 매니저가 닫혀도 클라이언트는 종료하지 않습니다.
     *
     * @param redisClient 연결된 Lettuce RedisClient
     */
    fun cacheManagerOf(redisClient: RedisClient): LettuceCacheManager =
        LettuceCacheManager(
            redisClient = redisClient,
            classLoader = Thread.currentThread().contextClassLoader,
            cacheProvider = LettuceCachingProvider(),
            properties = null,
            uri = null,
            closeResource = {},  // 외부 관리 client이므로 종료하지 않음
        )

    /**
     * [JCache]`<K, V>`를 생성하거나 기존 캐시를 가져옵니다.
     *
     * @param redisClient 연결된 Lettuce RedisClient
     * @param cacheName 캐시 이름
     * @param ttlSeconds TTL (초), null이면 만료 없음
     * @param codec 직렬화 codec (기본값: lz4Fory)
     */
    inline fun <reified K: Any, reified V: Any> getOrCreate(
        redisClient: RedisClient,
        cacheName: String,
        ttlSeconds: Long? = null,
        codec: LettuceBinaryCodec<*> = LettuceBinaryCodecs.lz4Fory<Any>(),
    ): JCache<K, V> {
        val manager = cacheManagerOf(redisClient)
        return manager.getCache(cacheName)
            ?: manager.createCache(
                cacheName,
                lettuceCacheConfigOf<K, V>(ttlSeconds = ttlSeconds, codec = codec)
            )
    }

    /**
     * 커스텀 [Configuration]을 이용해 [JCache]`<K, V>`를 생성하거나 가져옵니다.
     *
     * @param redisClient 연결된 Lettuce RedisClient
     * @param cacheName 캐시 이름
     * @param configuration JCache 설정
     */
    inline fun <reified K: Any, reified V: Any> getOrCreate(
        redisClient: RedisClient,
        cacheName: String,
        configuration: Configuration<K, V>,
    ): JCache<K, V> {
        val manager = cacheManagerOf(redisClient)
        return manager.getCache(cacheName)
            ?: manager.createCache(cacheName, configuration)
    }
}
