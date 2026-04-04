package io.bluetape4k.cache.nearcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.future.await
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.Codec

/**
 * Redisson [RLocalCachedMap] 기반 Near Cache (2-tier cache) - Coroutine(Suspend) 구현.
 *
 * ## 아키텍처
 * Redisson `RLocalCachedMap`은 내장 2-tier 캐시를 제공합니다:
 * - **로컬 캐시**: JVM 내 in-memory 캐시 (front)
 * - **Redis 캐시**: Redisson이 관리하는 분산 캐시 (back)
 * - **Invalidation**: Redisson이 자동으로 client-side caching + pub/sub invalidation을 처리
 *
 * 모든 연산은 `RLocalCachedMap`의 `*Async()` 메서드에 `.await()`를 적용하여 suspend function으로 제공합니다.
 *
 * @param V 값 타입 (키는 항상 String)
 * @param redisson Redisson 클라이언트
 * @param config Near Cache 설정
 * @param codec Redisson 직렬화 Codec
 */
class RedissonSuspendNearCache<V : Any>(
    private val redisson: RedissonClient,
    private val config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
    private val codec: Codec = RedissonCodecs.LZ4Fory,
) : SuspendNearCacheOperations<V> {
    companion object : KLogging()

    override val cacheName: String get() = config.cacheName

    private val closed = atomic(false)
    override val isClosed: Boolean by closed

    private val backHitCount = atomic(0L)
    private val backMissCount = atomic(0L)

    private val localCachedMap: RLocalCachedMap<String, V> =
        redisson.getLocalCachedMap(buildLocalCachedMapOptions(config, codec))

    /**
     * [key]에 해당하는 값을 조회합니다.
     *
     * `RLocalCachedMap`이 자동으로 로컬 캐시 → Redis 순서로 조회합니다.
     */
    override suspend fun get(key: String): V? {
        val value = localCachedMap.getAsync(key).await()
        if (value != null) backHitCount.incrementAndGet() else backMissCount.incrementAndGet()
        return value
    }

    /**
     * 여러 [keys]에 해당하는 값을 일괄 조회합니다.
     */
    override suspend fun getAll(keys: Set<String>): Map<String, V> {
        @Suppress("UNCHECKED_CAST")
        return localCachedMap.getAllAsync(keys).await() as Map<String, V>
    }

    /**
     * [key]가 캐시에 존재하는지 확인합니다.
     */
    override suspend fun containsKey(key: String): Boolean = localCachedMap.containsKeyAsync(key).await() == true

    /**
     * [key]-[value] 쌍을 저장합니다.
     */
    override suspend fun put(
        key: String,
        value: V,
    ) {
        localCachedMap.putAsync(key, value).await()
    }

    /**
     * 여러 [entries]를 일괄 저장합니다.
     */
    override suspend fun putAll(entries: Map<String, V>) {
        localCachedMap.putAllAsync(entries).await()
    }

    /**
     * [key]가 없을 때만 [value]를 저장합니다.
     *
     * @return 기존에 존재하던 값. 저장에 성공하면 null.
     */
    override suspend fun putIfAbsent(
        key: String,
        value: V,
    ): V? = localCachedMap.putIfAbsentAsync(key, value).await()

    /**
     * [key]의 값을 [value]로 교체합니다.
     *
     * @return 키가 존재하여 교체에 성공하면 true.
     */
    override suspend fun replace(
        key: String,
        value: V,
    ): Boolean = localCachedMap.replaceAsync(key, value).await() != null

    /**
     * [key]의 값이 [oldValue]와 일치할 때만 [newValue]로 교체합니다.
     *
     * @return 교체에 성공하면 true.
     */
    override suspend fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean = localCachedMap.replaceAsync(key, oldValue, newValue).await() == true

    /**
     * [key]를 삭제합니다.
     */
    override suspend fun remove(key: String) {
        localCachedMap.removeAsync(key).await()
    }

    /**
     * 여러 [keys]를 일괄 삭제합니다.
     *
     * 모든 삭제 요청을 먼저 비동기로 시작한 뒤 일괄 완료를 기다립니다.
     */
    override suspend fun removeAll(keys: Set<String>) {
        val futures = keys.map { localCachedMap.removeAsync(it) }
        futures.forEach { it.await() }
    }

    /**
     * [key]의 값을 반환하고 삭제합니다.
     *
     * @return 삭제된 값. 키가 없으면 null.
     */
    override suspend fun getAndRemove(key: String): V? = localCachedMap.removeAsync(key).await()

    /**
     * [key]의 현재 값을 반환하고 [value]로 교체합니다.
     *
     * @return 교체 전 값. 키가 없으면 null.
     */
    override suspend fun getAndReplace(
        key: String,
        value: V,
    ): V? = localCachedMap.replaceAsync(key, value).await()

    /**
     * 로컬 캐시만 비웁니다. Redis 캐시는 유지됩니다.
     * 로컬 메모리 접근이므로 suspend가 아닙니다.
     */
    override fun clearLocal() {
        localCachedMap.clearLocalCache()
    }

    /**
     * 로컬 캐시 + Redis 캐시 모두 비웁니다.
     */
    override suspend fun clearAll() {
        localCachedMap.clearAsync().await()
    }

    /**
     * 로컬 캐시 엔트리 수를 반환합니다.
     * 로컬 메모리 접근이므로 suspend가 아닙니다.
     */
    override fun localCacheSize(): Long = localCachedMap.cachedKeySet().size.toLong()

    /**
     * Redis 캐시 엔트리 수를 반환합니다.
     */
    override suspend fun backCacheSize(): Long =
        localCachedMap.sizeAsync().await()?.toLong() ?: localCachedMap.size.toLong()

    /**
     * 캐시 통계 스냅샷을 반환합니다.
     * 로컬 카운터 기반이므로 suspend가 아닙니다.
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
    override suspend fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { localCachedMap.destroy() }
            log.debug { "RedissonSuspendNearCache [${config.cacheName}] closed" }
        }
    }
}

/**
 * [RedissonClient]로 [SuspendNearCacheOperations]를 생성하는 팩토리 함수입니다.
 *
 * ```kotlin
 * val cache = redissonSuspendNearCacheOf<String>(redisson, RedissonNearCacheConfig(cacheName = "data"))
 * cache.put("key", "value")
 * val value = cache.get("key")
 * // value == "value"
 * ```
 *
 * @param V 값 타입
 * @param redisson Redisson 클라이언트
 * @param config Near Cache 설정
 * @param codec Redisson 직렬화 Codec
 * @return [SuspendNearCacheOperations] 인스턴스
 */
fun <V : Any> redissonSuspendNearCacheOf(
    redisson: RedissonClient,
    config: RedissonNearCacheConfig = RedissonNearCacheConfig(),
    codec: Codec = RedissonCodecs.LZ4Fory,
): SuspendNearCacheOperations<V> = RedissonSuspendNearCache(redisson, config, codec)
