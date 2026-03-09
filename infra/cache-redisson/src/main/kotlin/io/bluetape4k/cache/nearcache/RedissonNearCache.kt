package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.jcacheManager
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.jcache.JCachingProvider
import org.redisson.jcache.configuration.RedissonConfiguration
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Redisson back cache를 사용하는 [NearCache] 팩토리입니다.
 *
 * ## 동작/계약
 * - front cache는 [NearCacheConfig] 설정을 사용하며 기본 구현은 Caffeine입니다.
 * - back cache는 Redisson JCache 매니저에서 조회하거나 없으면 생성합니다.
 * - `backCacheName`이 blank면 `IllegalArgumentException`이 발생합니다.
 *
 * ```kotlin
 * val near = RedissonNearCache<String, String>("users", redisson)
 * // near.getName() == "users"
 * ```
 */
object RedissonNearCache {

    /**
     * 기본 Codec: [RedissonCodecs.LZ4Fory]
     */
    val defaultNearCacheCodec = RedissonCodecs.LZ4Fory

    /**
     * 기존 Back Cache 인스턴스로 [NearCache]를 생성합니다.
     *
     * @param backCache Redisson 기반 Back Cache
     * @param nearCacheConfig Near Cache 설정
     */
    operator fun <K: Any, V: Any> invoke(
        backCache: JCache<K, V>,
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> = NearCache(nearCacheConfig, backCache)

    /**
     * RedissonClient로 Back Cache를 조회/생성한 뒤 [NearCache]를 생성합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param redisson Redisson 클라이언트
     * @param backCacheConfiguration Back Cache 설정
     * @param nearCacheConfig Near Cache 설정
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        redisson: RedissonClient,
        backCacheConfiguration: Configuration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")

        val manager = jcacheManager<JCachingProvider>()
        val redissonCfg = RedissonConfiguration.fromInstance(redisson, backCacheConfiguration)
        val backCache = manager.getCache(backCacheName, K::class.java, V::class.java)
            ?: manager.createCache(backCacheName, redissonCfg)
        return NearCache(nearCacheConfig, backCache)
    }

    /**
     * Redisson Config로 Back Cache를 조회/생성한 뒤 [NearCache]를 생성합니다.
     *
     * @param backCacheName Back Cache 이름
     * @param redissonConfig Redisson 설정
     * @param backCacheConfiguration Back Cache 설정
     * @param nearCacheConfig Near Cache 설정
     */
    inline operator fun <reified K: Any, reified V: Any> invoke(
        backCacheName: String,
        redissonConfig: Config,
        backCacheConfiguration: CompleteConfiguration<K, V> = MutableConfiguration<K, V>().apply {
            setTypes(K::class.java, V::class.java)
        },
        nearCacheConfig: NearCacheConfig<K, V> = NearCacheConfig(),
    ): NearCache<K, V> {
        backCacheName.requireNotBlank("backCacheName")

        val manager = jcacheManager<JCachingProvider>()
        val redissonCfg = RedissonConfiguration.fromConfig(redissonConfig, backCacheConfiguration)
        val backCache = manager.getCache(backCacheName, K::class.java, V::class.java)
            ?: manager.createCache(backCacheName, redissonCfg)
        return NearCache(nearCacheConfig, backCache)
    }
}
