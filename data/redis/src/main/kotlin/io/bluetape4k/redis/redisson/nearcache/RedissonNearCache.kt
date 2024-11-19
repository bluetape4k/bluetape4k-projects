package io.bluetape4k.redis.redisson.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.bluetape4k.redis.redisson.options.name
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMapCache
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Redisson의 [RLocalCachedMap] 을 이용하여 NearCache를 구현합니다.
 *
 * @see [RLocalCachedMap]
 *
 * 참고:
 * - [Redisson Local Cache](https://github.com/redisson/redisson/wiki/7.-distributed-collections#local-cache)
 */
class RedissonNearCache<K: Any, V: Any> private constructor(
    internal val frontCache: RLocalCachedMap<K, V>,
    internal val backCache: RMapCache<K, V>,
): RLocalCachedMap<K, V> by frontCache {

    companion object: KLogging() {
        @JvmStatic
        val DefaultCodec = RedissonCodecs.Default

        @JvmStatic
        val DefaultLocalCacheMapOptions: LocalCachedMapOptions<String, Any> by lazy {
            LocalCachedMapOptions.name<String, Any>("default")
                .cacheSize(100_000)
                .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
                .timeToLive(60.seconds.toJavaDuration())
                .maxIdle(120.seconds.toJavaDuration())
                .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.LOAD)
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.UPDATE)
        }

        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            redisson: RedissonClient,
            options: LocalCachedMapOptions<K, V>,
        ): RedissonNearCache<K, V> {
            // RLocalCachedMap 은 Reference Object가 저장된다
            val frontCache = redisson.getLocalCachedMap(options)
            val backCache = redisson.getMapCache<K, V>(options.name)

            //            frontCache.expire(options.timeToLiveInMillis.milliseconds.toJavaDuration())
            //            backCache.expire(options.timeToLiveInMillis.milliseconds.toJavaDuration())

            return RedissonNearCache(frontCache, backCache)
        }
    }

    override fun destroy() {
        frontCache.destroy()
        backCache.destroy()
    }
}
