package io.bluetape4k.redis.lettuce.map

/**
 * [MapLoader]의 코루틴 버전. suspend 함수로 DB에서 값을 로드한다.
 *
 * Read-through 캐시 패턴에서 캐시 미스 시 DB에서 값을 로드하는 인터페이스.
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface SuspendedMapLoader<K: Any, V: Any> {
    /**
     * 주어진 [key]에 해당하는 값을 DB에서 로드한다.
     * 값이 없으면 null을 반환한다.
     */
    suspend fun load(key: K): V?

    /**
     * DB에 존재하는 모든 키를 반환한다.
     */
    suspend fun loadAllKeys(): List<K>
}
