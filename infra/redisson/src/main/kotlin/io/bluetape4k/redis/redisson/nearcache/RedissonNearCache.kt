package io.bluetape4k.redis.redisson.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import io.bluetape4k.redis.redisson.options.codec
import io.bluetape4k.redis.redisson.options.name
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Redisson의 [RLocalCachedMap]을 이용하여 2-Tier NearCache를 구현합니다.
 *
 * ## 구조
 * - **frontCache** ([RLocalCachedMap]): JVM 프로세스 내 로컬 캐시. 빠른 읽기 속도를 제공합니다.
 * - **backCache** ([RMap]): Redis 원격 캐시. 여러 노드 간 데이터를 공유합니다.
 *
 * ## 동작 방식
 * - 읽기: frontCache(로컬)에서 우선 조회하고, 없으면 backCache(Redis)에서 가져옵니다.
 * - 쓰기: frontCache와 backCache 모두에 동기화됩니다.
 * - 무효화: [LocalCachedMapOptions.SyncStrategy]에 따라 다른 노드의 로컬 캐시를 무효화합니다.
 *
 * ## 주의사항
 * - [destroy]를 호출하면 로컬 캐시 인스턴스만 종료되며, Redis의 원격 데이터는 유지됩니다.
 * - 여러 노드에서 같은 캐시 이름을 공유할 경우, 동기화 전략([LocalCachedMapOptions.SyncStrategy])을 적절히 설정해야 합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @see RLocalCachedMap
 * @see LocalCachedMapOptions
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
            val cacheName = options.name.requireNotBlank("name")
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
