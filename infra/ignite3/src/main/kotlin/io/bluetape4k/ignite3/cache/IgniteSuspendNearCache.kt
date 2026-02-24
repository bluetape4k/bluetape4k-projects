package io.bluetape4k.ignite3.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.future.await
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.table.KeyValueView

/**
 * Caffeine을 Front Cache로, Apache Ignite 3.x [KeyValueView]를 Back Cache로 사용하는 2-Tier Suspend NearCache입니다.
 *
 * Ignite 3.x 오픈소스는 클라이언트 측 Near Cache를 내장하지 않으므로,
 * Caffeine으로 로컬 캐시 레이어를 추가합니다.
 * `KeyValueView`의 네이티브 CompletableFuture API(`getAsync`, `putAsync`, `removeAsync` 등)를
 * 코루틴에서 non-blocking으로 활용합니다.
 *
 * ```kotlin
 * val nearCache = IgniteSuspendNearCache<Long, String>(
 *     client = igniteClient,
 *     keyType = Long::class.javaObjectType,
 *     valueType = String::class.java,
 *     config = IgniteNearCacheConfig(tableName = "MY_TABLE"),
 * )
 * nearCache.put(1L, "value")
 * val cached = nearCache.get(1L)
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property backCache Ignite 3.x [KeyValueView] (원격 캐시)
 * @property frontCache Caffeine [Cache] (로컬 캐시)
 * @property config NearCache 설정
 */
class IgniteSuspendNearCache<K: Any, V: Any> private constructor(
    val backCache: KeyValueView<K, V>,
    val frontCache: Cache<K, V>,
    private val config: IgniteNearCacheConfig,
) {
    companion object: KLogging() {

        /**
         * [IgniteSuspendNearCache] 인스턴스를 생성합니다.
         *
         * @param client Ignite 3.x 클라이언트
         * @param keyType 캐시 키 클래스 타입
         * @param valueType 캐시 값 클래스 타입
         * @param config Near Cache 설정
         * @return [IgniteSuspendNearCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            client: IgniteClient,
            keyType: Class<K>,
            valueType: Class<V>,
            config: IgniteNearCacheConfig,
        ): IgniteSuspendNearCache<K, V> {
            val table = client.tables().table(config.tableName)
                ?: error("Ignite 3.x 테이블을 찾을 수 없습니다. tableName=${config.tableName}")
            val backCache = table.keyValueView(keyType, valueType)

            val caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(config.frontCacheMaxSize)
            if (!config.frontCacheTtl.isZero) caffeineBuilder.expireAfterWrite(config.frontCacheTtl)
            if (!config.frontCacheMaxIdleTime.isZero) caffeineBuilder.expireAfterAccess(config.frontCacheMaxIdleTime)
            val frontCache = caffeineBuilder.build<K, V>()

            return IgniteSuspendNearCache(backCache, frontCache, config)
        }

        /**
         * reified 타입 파라미터를 사용하는 [IgniteSuspendNearCache] 인스턴스 생성 함수입니다.
         *
         * @param client Ignite 3.x 클라이언트
         * @param config Near Cache 설정
         * @return [IgniteSuspendNearCache] 인스턴스
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            client: IgniteClient,
            config: IgniteNearCacheConfig,
        ): IgniteSuspendNearCache<K, V> = invoke(client, K::class.java, V::class.java, config)
    }

    /**
     * Front Cache(Caffeine) → Back Cache(Ignite 3.x) 순으로 조회합니다.
     *
     * @param key 캐시 키
     * @return 캐시 값 또는 null
     */
    suspend fun get(key: K): V? {
        frontCache.getIfPresent(key)?.let { return it }
        log.debug { "Front Cache 미스 - Ignite 3.x에서 조회. table=${config.tableName}, key=$key" }
        val value = backCache.getAsync(null, key).await()
        if (value != null) {
            frontCache.put(key, value)
        }
        return value
    }

    /**
     * Front Cache(Caffeine)와 Back Cache(Ignite 3.x) 모두에 값을 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     */
    suspend fun put(key: K, value: V) {
        frontCache.put(key, value)
        log.debug { "캐시 저장. table=${config.tableName}, key=$key" }
        backCache.putAsync(null, key, value).await()
    }

    /**
     * 여러 항목을 한 번에 Front Cache와 Back Cache에 저장합니다.
     *
     * @param entries 저장할 키-값 쌍
     */
    suspend fun putAll(entries: Map<K, V>) {
        log.debug { "캐시 일괄 저장. table=${config.tableName}, size=${entries.size}" }
        entries.forEach { (k, v) -> frontCache.put(k, v) }
        backCache.putAllAsync(null, entries).await()
    }

    /**
     * 키에 해당하는 항목을 Front Cache와 Back Cache에서 삭제합니다.
     *
     * @param key 삭제할 캐시 키
     * @return 삭제 성공 여부
     */
    suspend fun remove(key: K): Boolean {
        frontCache.invalidate(key)
        log.debug { "캐시 삭제. table=${config.tableName}, key=$key" }
        return backCache.removeAsync(null, key).await()
    }

    /**
     * 키가 Front Cache 또는 Back Cache에 존재하는지 확인합니다.
     *
     * @param key 확인할 캐시 키
     * @return 존재 여부
     */
    suspend fun containsKey(key: K): Boolean {
        if (frontCache.getIfPresent(key) != null) return true
        return backCache.getAsync(null, key).await() != null
    }

    /**
     * Front Cache 히트 항목과 Back Cache에서 조회한 나머지 항목을 병합하여 반환합니다.
     *
     * @param keys 조회할 캐시 키 집합
     * @return 키-값 맵 (존재하지 않는 키는 포함되지 않음)
     */
    suspend fun getAll(keys: Set<K>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        val missedKeys = mutableSetOf<K>()
        keys.forEach { key ->
            frontCache.getIfPresent(key)?.let { result[key] = it } ?: missedKeys.add(key)
        }
        if (missedKeys.isNotEmpty()) {
            log.debug { "Front Cache 미스 - Ignite 3.x 일괄 조회. missed=${missedKeys.size}개" }
            backCache.getAllAsync(null, missedKeys).await().forEach { (k, v) ->
                result[k] = v
                frontCache.put(k, v)
            }
        }
        return result
    }

    /**
     * Front Cache(Caffeine)만 초기화합니다. Back Cache(Ignite)는 변경하지 않습니다.
     */
    fun clearFrontCache() {
        log.debug { "Front Cache(Caffeine) 초기화. table=${config.tableName}" }
        frontCache.invalidateAll()
    }
}
