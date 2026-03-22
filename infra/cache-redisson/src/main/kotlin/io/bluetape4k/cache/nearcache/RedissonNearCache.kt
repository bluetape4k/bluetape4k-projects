package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import kotlinx.atomicfu.atomic
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.client.codec.Codec

/**
 * Redisson [RLocalCachedMap] 기반 Near Cache (2-tier cache) - 동기(Blocking) 구현.
 *
 * ## 아키텍처
 * Redisson `RLocalCachedMap`은 내장 2-tier 캐시를 제공합니다:
 * - **로컬 캐시**: JVM 내 in-memory 캐시 (front)
 * - **Redis 캐시**: Redisson이 관리하는 분산 캐시 (back)
 * - **Invalidation**: Redisson이 자동으로 client-side caching + pub/sub invalidation을 처리
 *
 * Lettuce RESP3 하이브리드가 불필요하며, Redisson 단일 연결로 동작합니다.
 *
 * @param V 값 타입 (키는 항상 String)
 * @param redisson Redisson 클라이언트
 * @param config Near Cache 설정
 * @param codec Redisson 직렬화 Codec
 */
class RedissonNearCache<V: Any>(
    private val redisson: RedissonClient,
    private val config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
    private val codec: Codec = RedissonCodecs.LZ4Fory,
): NearCacheOperations<V> {
    companion object: KLogging()

    override val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    override val isClosed: Boolean by closed

    private val backHitCount = atomic(0L)
    private val backMissCount = atomic(0L)

    private val localCachedMap: RLocalCachedMap<String, V> =
        redisson.getLocalCachedMap(
            buildLocalCachedMapOptions(config, codec)
        )

    /**
     * [key]에 해당하는 값을 조회합니다.
     *
     * `RLocalCachedMap`이 자동으로 로컬 캐시 → Redis 순서로 조회합니다.
     */
    override fun get(key: String): V? {
        val value = localCachedMap[key]
        if (value != null) backHitCount.incrementAndGet() else backMissCount.incrementAndGet()
        return value
    }

    /**
     * 여러 [keys]에 해당하는 값을 일괄 조회합니다.
     */
    override fun getAll(keys: Set<String>): Map<String, V> {
        @Suppress("UNCHECKED_CAST")
        return localCachedMap.getAll(keys) as Map<String, V>
    }

    /**
     * [key]가 캐시에 존재하는지 확인합니다.
     */
    override fun containsKey(key: String): Boolean = localCachedMap.containsKey(key)

    /**
     * [key]-[value] 쌍을 저장합니다.
     */
    override fun put(
        key: String,
        value: V,
    ) {
        localCachedMap[key] = value
    }

    /**
     * 여러 [entries]를 일괄 저장합니다.
     */
    override fun putAll(entries: Map<String, V>) {
        localCachedMap.putAll(entries)
    }

    /**
     * [key]가 없을 때만 [value]를 저장합니다.
     *
     * @return 기존에 존재하던 값. 저장에 성공하면 null.
     */
    override fun putIfAbsent(
        key: String,
        value: V,
    ): V? = localCachedMap.putIfAbsent(key, value)

    /**
     * [key]의 값을 [value]로 교체합니다.
     *
     * @return 키가 존재하여 교체에 성공하면 true.
     */
    override fun replace(
        key: String,
        value: V,
    ): Boolean = localCachedMap.replace(key, value) != null

    /**
     * [key]의 값이 [oldValue]와 일치할 때만 [newValue]로 교체합니다.
     *
     * @return 교체에 성공하면 true.
     */
    override fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean = localCachedMap.replace(key, oldValue, newValue)

    /**
     * [key]를 삭제합니다.
     */
    override fun remove(key: String) {
        localCachedMap.remove(key)
    }

    /**
     * 여러 [keys]를 일괄 삭제합니다.
     */
    override fun removeAll(keys: Set<String>) {
        keys.forEach { localCachedMap.remove(it) }
    }

    /**
     * [key]의 값을 반환하고 삭제합니다.
     *
     * @return 삭제된 값. 키가 없으면 null.
     */
    override fun getAndRemove(key: String): V? = localCachedMap.remove(key)

    /**
     * [key]의 현재 값을 반환하고 [value]로 교체합니다.
     *
     * @return 교체 전 값. 키가 없으면 null.
     */
    override fun getAndReplace(
        key: String,
        value: V,
    ): V? = localCachedMap.replace(key, value)

    /**
     * 로컬 캐시만 비웁니다. Redis 캐시는 유지됩니다.
     */
    override fun clearLocal() {
        localCachedMap.clearLocalCache()
    }

    /**
     * 로컬 캐시 + Redis 캐시 모두 비웁니다.
     */
    override fun clearAll() {
        localCachedMap.clear()
    }

    /**
     * 로컬 캐시 엔트리 수를 반환합니다.
     */
    override fun localCacheSize(): Long = localCachedMap.cachedKeySet().size.toLong()

    /**
     * Redis 캐시 엔트리 수를 반환합니다.
     */
    override fun backCacheSize(): Long = localCachedMap.size.toLong()

    /**
     * 캐시 통계 스냅샷을 반환합니다.
     *
     * **참고**: Redisson의 `RLocalCachedMap`이 로컬 캐시와 Redis 캐시를 내부적으로 통합 관리하므로,
     * `backHitCount`/`backMissCount`는 로컬+Redis 통합 조회 결과를 기준으로 카운트됩니다.
     * Redisson이 별도의 로컬/백엔드 통계를 노출하지 않으므로 `localHits`/`localMisses`는 0으로 보고됩니다.
     */
    override fun stats(): NearCacheStatistics =
        DefaultNearCacheStatistics(
            localHits = 0L,
            localMisses = 0L,
            localSize = localCacheSize(),
            localEvictions = 0L,
            backHits = backHitCount.value,
            backMisses = backMissCount.value
        )

    /**
     * 리소스를 정리합니다.
     */
    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { localCachedMap.destroy() }
            log.debug { "RedissonNearCache [${config.cacheName}] closed" }
        }
    }
}

/**
 * [RedissonNearCacheConfig]로 [LocalCachedMapOptions]를 생성합니다.
 */
internal fun <K, V> buildLocalCachedMapOptions(
    config: RedissonNearCacheConfig,
    codec: Codec = RedissonCodecs.LZ4Fory,
): LocalCachedMapOptions<K, V> {
    val opts = LocalCachedMapOptions
        .name<K, V>(config.cacheName)
        .codec(codec)
        .cacheSize(config.maxLocalSize)
        .evictionPolicy(config.evictionPolicy)
        .syncStrategy(config.syncStrategy)
        .reconnectionStrategy(config.reconnectionStrategy)
    config.timeToLive?.let { opts.timeToLive(it) }
    config.maxIdle?.let { opts.maxIdle(it) }
    return opts
}

/**
 * [RedissonClient]로 [NearCacheOperations]를 생성하는 팩토리 함수입니다.
 *
 * @param V 값 타입
 * @param redisson Redisson 클라이언트
 * @param config Near Cache 설정
 * @param codec Redisson 직렬화 Codec
 * @return [NearCacheOperations] 인스턴스
 */
fun <V: Any> redissonNearCacheOf(
    redisson: RedissonClient,
    config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
    codec: Codec = RedissonCodecs.LZ4Fory,
): NearCacheOperations<V> = RedissonNearCache(redisson, config, codec)
