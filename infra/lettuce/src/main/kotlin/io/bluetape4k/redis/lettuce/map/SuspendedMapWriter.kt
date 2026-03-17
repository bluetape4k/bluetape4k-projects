package io.bluetape4k.redis.lettuce.map

/**
 * [MapWriter]의 코루틴 버전. suspend 함수로 DB에 값을 쓰거나 삭제한다.
 *
 * Write-through / Write-behind 캐시 패턴에서 DB에 반영하는 인터페이스.
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface SuspendedMapWriter<K: Any, V: Any> {
    /**
     * 여러 항목을 일괄 저장(upsert)한다.
     */
    suspend fun write(map: Map<K, V>)

    /**
     * 여러 항목을 일괄 삭제한다.
     */
    suspend fun delete(keys: Collection<K>)
}
