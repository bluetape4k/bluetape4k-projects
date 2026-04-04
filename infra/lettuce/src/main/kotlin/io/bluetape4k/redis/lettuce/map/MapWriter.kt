package io.bluetape4k.redis.lettuce.map

/**
 * 캐시 항목을 DB에 반영하는 인터페이스 (Write-Through / Write-Behind).
 *
 * ```kotlin
 * val writer = object : MapWriter<String, Int> {
 *     override fun write(map: Map<String, Int>) { db.upsertAll(map) }
 *     override fun delete(keys: Collection<String>) { db.deleteAll(keys) }
 * }
 * val config = LettuceCacheConfig(writeMode = WriteMode.WRITE_THROUGH)
 * val map = LettuceLoadedMap(connection, config, loader, writer)
 * map["hello"] = 5   // Redis에 저장 + DB에 write-through
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface MapWriter<K: Any, V: Any> {
    /**
     * 여러 항목을 일괄 저장(upsert)한다.
     *
     * ```kotlin
     * writer.write(mapOf("key1" to 1, "key2" to 2))
     * ```
     */
    fun write(map: Map<K, V>)

    /**
     * 여러 항목을 일괄 삭제한다.
     *
     * ```kotlin
     * writer.delete(listOf("key1", "key2"))
     * ```
     */
    fun delete(keys: Collection<K>)
}
