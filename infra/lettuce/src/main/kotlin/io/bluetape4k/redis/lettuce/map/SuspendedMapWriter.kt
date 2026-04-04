package io.bluetape4k.redis.lettuce.map

/**
 * [MapWriter]의 코루틴 버전. suspend 함수로 DB에 값을 쓰거나 삭제한다.
 *
 * Write-through / Write-behind 캐시 패턴에서 DB에 반영하는 인터페이스.
 *
 * ```kotlin
 * val writer = object : SuspendedMapWriter<String, Int> {
 *     override suspend fun write(map: Map<String, Int>) { db.upsertAll(map) }
 *     override suspend fun delete(keys: Collection<String>) { db.deleteAll(keys) }
 * }
 * val map = LettuceSuspendedLoadedMap(connection, config, loader, writer)
 * map.set("hello", 5)   // Redis에 저장 + DB에 write-through
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface SuspendedMapWriter<K: Any, V: Any> {
    /**
     * 여러 항목을 일괄 저장(upsert)한다.
     *
     * ```kotlin
     * writer.write(mapOf("key1" to 1, "key2" to 2))
     * ```
     */
    suspend fun write(map: Map<K, V>)

    /**
     * 여러 항목을 일괄 삭제한다.
     *
     * ```kotlin
     * writer.delete(listOf("key1", "key2"))
     * ```
     */
    suspend fun delete(keys: Collection<K>)
}
