package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats

/**
 * Lettuce 기반 Near Cache (2-tier cache)의 Coroutine(Suspend) 공통 인터페이스.
 *
 * [LettuceSuspendNearCache]와 [ResilientLettuceSuspendNearCache]가 이 인터페이스를 구현하여
 * 다형적으로 사용할 수 있도록 한다.
 *
 * @param V 값 타입 (키는 항상 String)
 */
interface LettuceSuspendNearCacheOperations<V: Any>: AutoCloseable {
    /** 캐시 이름 (Redis key prefix로 사용) */
    val cacheName: String

    /** 캐시가 닫혔는지 여부 */
    val isClosed: Boolean

    // ---- Read ----

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → Redis GET → front populate → return
     */
    suspend fun get(key: String): V?

    /**
     * 여러 키에 대한 값을 한 번에 조회한다 (multi-get).
     */
    suspend fun getAll(keys: Set<String>): Map<String, V>

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or Redis).
     */
    suspend fun containsKey(key: String): Boolean

    // ---- Write ----

    /**
     * key-value를 저장한다.
     */
    suspend fun put(
        key: String,
        value: V,
    )

    /**
     * 여러 key-value를 한 번에 저장한다.
     */
    suspend fun putAll(map: Map<out String, V>)

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    suspend fun putIfAbsent(
        key: String,
        value: V,
    ): V?

    /**
     * 기존 값을 새 값으로 교체한다.
     * @return 교체 성공 여부
     */
    suspend fun replace(
        key: String,
        value: V,
    ): Boolean

    /**
     * 기존 값이 [oldValue]와 같을 때만 [newValue]로 교체한다.
     */
    suspend fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean

    // ---- Delete ----

    /**
     * 키를 제거한다 (front + Redis).
     */
    suspend fun remove(key: String)

    /**
     * 여러 키를 한 번에 제거한다.
     */
    suspend fun removeAll(keys: Set<String>)

    /**
     * 조회 후 제거한다.
     */
    suspend fun getAndRemove(key: String): V?

    /**
     * 조회 후 교체한다.
     */
    suspend fun getAndReplace(
        key: String,
        value: V,
    ): V?

    // ---- Management ----

    /**
     * 로컬 캐시만 비운다 (Redis 유지).
     */
    fun clearLocal()

    /**
     * 로컬 캐시 + Redis를 모두 비운다.
     */
    suspend fun clearAll()

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localCacheSize(): Long

    /**
     * Redis에서 이 cacheName에 속한 key의 개수를 반환한다.
     */
    suspend fun backCacheSize(): Long

    /**
     * 로컬 캐시(Caffeine) 통계. recordStats가 true일 때만 유효한 값을 반환한다.
     */
    fun localStats(): CacheStats?
}
