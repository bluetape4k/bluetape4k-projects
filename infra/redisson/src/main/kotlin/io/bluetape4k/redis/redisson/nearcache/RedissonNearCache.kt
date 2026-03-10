package io.bluetape4k.redis.redisson.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.redis.redisson.options.codec
import io.bluetape4k.redis.redisson.options.name
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
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
    internal val backCache: RMap<K, V>,
): RLocalCachedMap<K, V> by frontCache {

    companion object: KLogging() {
        /** Near cache 생성 시 기본으로 사용할 Codec 입니다. */
        @JvmStatic
        val DefaultCodec = RedissonCodecs.LZ4Fory

        /** Near cache 에 사용할 기본 로컬 캐시 옵션입니다. */
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

        /**
         * [LocalCachedMapOptions] 기반으로 [RedissonNearCache]를 생성합니다.
         *
         * 로컬 캐시와 원격 백엔드 캐시가 동일한 이름/코덱을 사용하도록 맞춰,
         * destroy 시 로컬 near cache 인스턴스만 정리해도 원격 데이터는 보존됩니다.
         */
        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            redisson: RedissonClient,
            options: LocalCachedMapOptions<K, V>,
        ): RedissonNearCache<K, V> {
            // RLocalCachedMap 은 Reference Object가 저장된다
            val frontCache = redisson.getLocalCachedMap(options)
            val cacheName = requireNotNull(options.name) {
                "LocalCachedMapOptions.name must not be null when creating RedissonNearCache."
            }
            val codec = options.codec ?: DefaultCodec
            val backCache = redisson.getMap<K, V>(cacheName, codec)

            //            frontCache.expire(options.timeToLiveInMillis.milliseconds.toJavaDuration())
            //            backCache.expire(options.timeToLiveInMillis.milliseconds.toJavaDuration())

            return RedissonNearCache(frontCache, backCache)
        }
    }

    /**
     * 로컬 near cache 인스턴스만 종료합니다.
     *
     * 원격 백엔드 캐시는 공유 리소스이므로 여기서 파괴하지 않습니다.
     */
    override fun destroy() {
        frontCache.destroy()
    }
}
