package io.bluetape4k.cache.nearcache

/**
 * NearCache 공통 인터페이스 (Blocking).
 *
 * Caffeine 로컬 캐시(front)와 분산 캐시(back)의 2-tier 캐시를 통일된 API로 제공합니다.
 * Lettuce(Redis), Hazelcast, Redisson, JCache 등 다양한 백엔드를 동일한 인터페이스로 사용할 수 있습니다.
 *
 * 키는 [String]으로 고정됩니다. 모든 분산 캐시 백엔드에서 키는 결국 String으로 직렬화되므로,
 * 제네릭 키는 복잡성만 추가합니다.
 *
 * ```kotlin
 * // Lettuce NearCache 예시 (LettuceNearCache는 NearCacheOperations를 구현)
 * val cache: NearCacheOperations<String> = lettuceNearCacheOf(redisClient, codec, config)
 * cache.put("hello", "world")
 * val value = cache.get("hello")
 * // value == "world"
 * cache.remove("hello")
 * cache.close()
 * ```
 *
 * @param V 캐시 값 타입
 * @see SuspendNearCacheOperations suspend 버전
 * @see ResilientNearCacheDecorator retry + failure strategy Decorator
 */
interface NearCacheOperations<V: Any>: AutoCloseable {
    /** 캐시 이름 */
    val cacheName: String

    /** 캐시 종료 여부 */
    val isClosed: Boolean

    // -- Read --

    /**
     * [key]에 해당하는 값을 조회합니다.
     *
     * 로컬 캐시에 있으면 즉시 반환하고, 없으면 백엔드에서 조회하여 로컬에 채웁니다.
     *
     * @return 캐시된 값. 없으면 null.
     */
    fun get(key: String): V?

    /**
     * 여러 [keys]에 해당하는 값을 일괄 조회합니다.
     *
     * 로컬 히트된 키는 바로 반환하고, 미스된 키만 백엔드에서 조회합니다.
     */
    fun getAll(keys: Set<String>): Map<String, V>

    /**
     * [key]가 캐시에 존재하는지 확인합니다.
     * 로컬과 백엔드 모두 확인합니다.
     */
    fun containsKey(key: String): Boolean

    // -- Write --

    /**
     * [key]-[value] 쌍을 저장합니다.
     * 로컬과 백엔드 모두에 write-through로 저장합니다.
     */
    fun put(key: String, value: V)

    /**
     * 여러 [entries]를 일괄 저장합니다.
     */
    fun putAll(entries: Map<String, V>)

    /**
     * [key]가 없을 때만 [value]를 저장합니다.
     *
     * @return 기존에 존재하던 값. 키가 없어서 저장에 성공하면 null.
     */
    fun putIfAbsent(key: String, value: V): V?

    /**
     * [key]의 값을 [value]로 교체합니다.
     *
     * @return 키가 존재하여 교체에 성공하면 true.
     */
    fun replace(key: String, value: V): Boolean

    /**
     * [key]의 값이 [oldValue]와 일치할 때만 [newValue]로 교체합니다.
     *
     * @return 교체에 성공하면 true.
     */
    fun replace(key: String, oldValue: V, newValue: V): Boolean

    // -- Delete --

    /**
     * [key]를 삭제합니다.
     */
    fun remove(key: String)

    /**
     * 여러 [keys]를 일괄 삭제합니다.
     */
    fun removeAll(keys: Set<String>)

    /**
     * [key]의 값을 반환하고 삭제합니다.
     *
     * @return 삭제된 값. 키가 없으면 null.
     */
    fun getAndRemove(key: String): V?

    /**
     * [key]의 현재 값을 반환하고 [value]로 교체합니다.
     *
     * @return 교체 전 값. 키가 없으면 null (교체하지 않음).
     */
    fun getAndReplace(key: String, value: V): V?

    // -- Cache Management --

    /**
     * 로컬 캐시만 비웁니다.
     * 백엔드 캐시는 유지됩니다.
     */
    fun clearLocal()

    /**
     * 로컬 + 백엔드 캐시 모두 비웁니다.
     */
    fun clearAll()

    /**
     * 로컬 캐시 엔트리 수를 반환합니다.
     */
    fun localCacheSize(): Long

    /**
     * 백엔드 캐시 엔트리 수를 반환합니다.
     */
    fun backCacheSize(): Long

    // -- Statistics --

    /**
     * 캐시 통계를 반환합니다.
     *
     * 로컬 캐시의 hit/miss/eviction과 백엔드 캐시의 hit/miss를 포함합니다.
     */
    fun stats(): NearCacheStatistics
}
