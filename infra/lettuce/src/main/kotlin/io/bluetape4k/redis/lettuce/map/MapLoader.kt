package io.bluetape4k.redis.lettuce.map

/**
 * 캐시 미스 시 DB에서 값을 로드하는 인터페이스 (Read-Through).
 *
 * ```kotlin
 * val loader = object : MapLoader<String, Int> {
 *     override fun load(key: String): Int? = db.findByKey(key)
 *     override fun loadAllKeys(): Iterable<String> = db.findAllKeys()
 * }
 * val config = LettuceCacheConfig(writeMode = WriteMode.WRITE_THROUGH)
 * val map = LettuceLoadedMap(connection, config, loader, writer)
 * val value = map["hello"]   // DB에서 로드
 * // value == db.findByKey("hello")
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface MapLoader<K: Any, V: Any> {
    /**
     * 주어진 [key]에 해당하는 값을 DB에서 로드한다.
     * 값이 없으면 null을 반환한다.
     */
    fun load(key: K): V?

    /**
     * DB에 존재하는 모든 키를 반환한다.
     */
    fun loadAllKeys(): Iterable<K>
}
