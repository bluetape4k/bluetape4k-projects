package io.bluetape4k.cache.nearcache

/**
 * NearCache 공통 인터페이스 (Coroutine Suspend).
 *
 * [NearCacheOperations]의 suspend 버전입니다.
 * Caffeine 로컬 캐시(front)와 분산 캐시(back)의 2-tier 캐시를 코루틴 기반으로 제공합니다.
 *
 * [AutoCloseable]을 구현하지 않습니다. `AutoCloseable.close()`는 non-suspend이므로
 * `suspend fun close()`와 시그니처가 충돌합니다. 대신 [close]를 직접 선언합니다.
 *
 * @param V 캐시 값 타입 (키는 String 고정)
 * @see NearCacheOperations blocking 버전
 * @see ResilientSuspendNearCacheDecorator retry + failure strategy Decorator
 */
interface SuspendNearCacheOperations<V : Any> {
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
    suspend fun get(key: String): V?

    /**
     * 여러 [keys]에 해당하는 값을 일괄 조회합니다.
     */
    suspend fun getAll(keys: Set<String>): Map<String, V>

    /**
     * [key]가 캐시에 존재하는지 확인합니다.
     */
    suspend fun containsKey(key: String): Boolean

    // -- Write --

    /**
     * [key]-[value] 쌍을 저장합니다.
     */
    suspend fun put(
        key: String,
        value: V,
    )

    /**
     * 여러 [entries]를 일괄 저장합니다.
     */
    suspend fun putAll(entries: Map<String, V>)

    /**
     * [key]가 없을 때만 [value]를 저장합니다.
     *
     * @return 기존에 존재하던 값. 키가 없어서 저장에 성공하면 null.
     */
    suspend fun putIfAbsent(
        key: String,
        value: V,
    ): V?

    /**
     * [key]의 값을 [value]로 교체합니다.
     *
     * @return 키가 존재하여 교체에 성공하면 true.
     */
    suspend fun replace(
        key: String,
        value: V,
    ): Boolean

    /**
     * [key]의 값이 [oldValue]와 일치할 때만 [newValue]로 교체합니다.
     *
     * @return 교체에 성공하면 true.
     */
    suspend fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean

    // -- Delete --

    /**
     * [key]를 삭제합니다.
     */
    suspend fun remove(key: String)

    /**
     * 여러 [keys]를 일괄 삭제합니다.
     */
    suspend fun removeAll(keys: Set<String>)

    /**
     * [key]의 값을 반환하고 삭제합니다.
     *
     * @return 삭제된 값. 키가 없으면 null.
     */
    suspend fun getAndRemove(key: String): V?

    /**
     * [key]의 현재 값을 반환하고 [value]로 교체합니다.
     *
     * @return 교체 전 값. 키가 없으면 null (교체하지 않음).
     */
    suspend fun getAndReplace(
        key: String,
        value: V,
    ): V?

    // -- Cache Management --

    /**
     * 로컬 캐시만 비웁니다. 백엔드 캐시는 유지됩니다.
     * 로컬 메모리 접근이므로 suspend가 아닙니다.
     */
    fun clearLocal()

    /**
     * 로컬 + 백엔드 캐시 모두 비웁니다.
     */
    suspend fun clearAll()

    /**
     * 로컬 캐시 엔트리 수를 반환합니다.
     * 로컬 메모리 접근이므로 suspend가 아닙니다.
     */
    fun localCacheSize(): Long

    /**
     * 백엔드 캐시 엔트리 수를 반환합니다.
     */
    suspend fun backCacheSize(): Long

    // -- Statistics --

    /**
     * 캐시 통계를 반환합니다.
     * 로컬 카운터 기반이므로 suspend가 아닙니다.
     */
    fun stats(): NearCacheStatistics

    // -- Lifecycle --

    /**
     * 캐시 리소스를 정리합니다.
     */
    suspend fun close()
}
