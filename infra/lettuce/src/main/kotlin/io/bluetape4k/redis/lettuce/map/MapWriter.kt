package io.bluetape4k.redis.lettuce.map

/**
 * 캐시 항목을 DB에 반영하는 인터페이스 (Write-Through / Write-Behind).
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface MapWriter<K: Any, V: Any> {
    /**
     * 여러 항목을 일괄 저장(upsert)한다.
     */
    fun write(map: Map<K, V>)

    /**
     * 여러 항목을 일괄 삭제한다.
     */
    fun delete(keys: Collection<K>)
}
