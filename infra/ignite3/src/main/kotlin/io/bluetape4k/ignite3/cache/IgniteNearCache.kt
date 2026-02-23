package io.bluetape4k.ignite3.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.table.KeyValueView

/**
 * Apache Ignite 3.x를 Back Cache로, Caffeine을 Front Cache로 사용하는 2-Tier NearCache입니다.
 *
 * Ignite 3.x 오픈소스는 클라이언트 측 Near Cache를 내장하지 않으므로,
 * Caffeine으로 로컬 캐시 레이어를 추가합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property backCache Ignite 3.x [KeyValueView] (원격 캐시)
 * @property frontCache Caffeine [Cache] (로컬 캐시)
 * @property config NearCache 설정
 */
class IgniteNearCache<K: Any, V: Any> private constructor(
    val backCache: KeyValueView<K, V>,
    val frontCache: Cache<K, V>,
    private val config: IgniteNearCacheConfig,
) {
    companion object: KLogging() {

        operator fun <K: Any, V: Any> invoke(
            client: IgniteClient,
            keyType: Class<K>,
            valueType: Class<V>,
            config: IgniteNearCacheConfig,
        ): IgniteNearCache<K, V> {
            val table = client.tables().table(config.tableName)
                ?: error("Ignite 3.x 테이블을 찾을 수 없습니다. tableName=${config.tableName}")
            val backCache = table.keyValueView(keyType, valueType)

            val caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(config.frontCacheMaxSize)
            if (!config.frontCacheTtl.isZero) caffeineBuilder.expireAfterWrite(config.frontCacheTtl)
            if (!config.frontCacheMaxIdleTime.isZero) caffeineBuilder.expireAfterAccess(config.frontCacheMaxIdleTime)
            val frontCache = caffeineBuilder.build<K, V>()

            return IgniteNearCache(backCache, frontCache, config)
        }

        inline operator fun <reified K: Any, reified V: Any> invoke(
            client: IgniteClient,
            config: IgniteNearCacheConfig,
        ): IgniteNearCache<K, V> = invoke(client, K::class.java, V::class.java, config)
    }

    /**
     * Front Cache(Caffeine) → Back Cache(Ignite 3.x) 순으로 조회합니다.
     */
    fun get(key: K): V? {
        frontCache.getIfPresent(key)?.let { return it }
        log.debug { "Front Cache 미스 - Ignite 3.x에서 조회. table=${config.tableName}, key=$key" }
        return backCache.get(null, key)?.also { frontCache.put(key, it) }
    }

    /**
     * Back Cache(Ignite 3.x)에 저장하고 Front Cache(Caffeine)를 갱신합니다.
     */
    fun put(key: K, value: V) {
        backCache.put(null, key, value)
        frontCache.put(key, value)
    }

    fun putAll(entries: Map<K, V>) {
        backCache.putAll(null, entries)
        frontCache.putAll(entries)
    }

    fun remove(key: K): Boolean {
        frontCache.invalidate(key)
        return backCache.remove(null, key)
    }

    fun removeAll(keys: Set<K>) {
        frontCache.invalidateAll(keys)
        backCache.removeAll(null, keys)
    }

    fun getAll(keys: Set<K>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        val missedKeys = mutableSetOf<K>()
        keys.forEach { key ->
            frontCache.getIfPresent(key)?.let { result[key] = it } ?: missedKeys.add(key)
        }
        if (missedKeys.isNotEmpty()) {
            log.debug { "Front Cache 미스 - Ignite 3.x 일괄 조회. missed=${missedKeys.size}개" }
            backCache.getAll(null, missedKeys).forEach { (k, v) ->
                result[k] = v
                frontCache.put(k, v)
            }
        }
        return result
    }

    fun containsKey(key: K): Boolean =
        frontCache.getIfPresent(key) != null || backCache.contains(null, key)

    /** Front Cache(Caffeine)만 초기화합니다. Back Cache(Ignite)는 변경하지 않습니다. */
    fun clearFrontCache() = frontCache.invalidateAll()
}
