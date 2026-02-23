package io.bluetape4k.hazelcast.cache

import com.hazelcast.map.IMap
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug

/**
 * Hazelcast [IMap]을 활용한 NearCache 래퍼입니다.
 *
 * Hazelcast 클라이언트에 [HazelcastNearCacheConfig]를 적용하면
 * 클라이언트가 자동으로 로컬 Near Cache를 관리합니다.
 * 이 클래스는 [IMap]에 편의 메서드를 추가합니다.
 *
 * **주의**: Near Cache는 반드시 **클라이언트 모드**에서 사용해야 합니다.
 * 임베디드 모드에서는 Near Cache가 동작하지 않습니다 (Enterprise 기능).
 *
 * ```kotlin
 * val client = hazelcastClient("localhost:5701") {
 *     addNearCacheConfig(HazelcastNearCacheConfig("my-map").toNearCacheConfig())
 * }
 * val nearCache = HazelcastNearCache(client.getMap("my-map"))
 * nearCache["key"] = value
 * val v = nearCache["key"]
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property map Near Cache가 활성화된 Hazelcast [IMap]
 */
class HazelcastNearCache<K: Any, V: Any>(
    val map: IMap<K, V>,
) {
    companion object: KLogging()

    /** 캐시 이름 */
    val name: String get() = map.name

    /** 캐시에 저장된 항목 수 */
    val size: Int get() = map.size

    /**
     * 키에 해당하는 값을 조회합니다. Near Cache → 원격 캐시 순으로 조회합니다.
     *
     * @param key 캐시 키
     * @return 캐시 값 또는 null
     */
    operator fun get(key: K): V? {
        log.debug { "캐시 조회. map=${map.name}, key=$key" }
        return map[key]
    }

    /**
     * 키에 해당하는 값을 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     */
    operator fun set(key: K, value: V) {
        log.debug { "캐시 저장. map=${map.name}, key=$key" }
        map.set(key, value)
    }

    /**
     * 키에 해당하는 값을 저장하고 이전 값을 반환합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     * @return 이전 값 또는 null
     */
    fun put(key: K, value: V): V? = map.put(key, value)

    /**
     * 여러 항목을 한 번에 저장합니다.
     *
     * @param entries 저장할 키-값 쌍
     */
    fun putAll(entries: Map<K, V>) {
        log.debug { "캐시 일괄 저장. map=${map.name}, size=${entries.size}" }
        map.putAll(entries)
    }

    /**
     * 키가 없을 때만 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     * @return 이전 값 또는 null
     */
    fun putIfAbsent(key: K, value: V): V? = map.putIfAbsent(key, value)

    /**
     * 키에 해당하는 항목을 삭제합니다.
     *
     * @param key 삭제할 캐시 키
     * @return 삭제된 값 또는 null
     */
    fun remove(key: K): V? {
        log.debug { "캐시 삭제. map=${map.name}, key=$key" }
        return map.remove(key)
    }

    /**
     * 여러 키에 해당하는 항목을 일괄 삭제합니다.
     *
     * @param keys 삭제할 캐시 키 목록
     */
    fun removeAll(keys: Set<K>) {
        log.debug { "캐시 일괄 삭제. map=${map.name}, keys=${keys.size}개" }
        keys.forEach { map.delete(it) }
    }

    /**
     * 키가 캐시에 존재하는지 확인합니다.
     *
     * @param key 확인할 캐시 키
     * @return 존재 여부
     */
    fun containsKey(key: K): Boolean = map.containsKey(key)

    /**
     * 여러 키에 해당하는 값을 일괄 조회합니다.
     *
     * @param keys 조회할 캐시 키 집합
     * @return 키-값 맵 (존재하지 않는 키는 포함되지 않음)
     */
    fun getAll(keys: Set<K>): Map<K, V> {
        log.debug { "캐시 일괄 조회. map=${map.name}, keys=${keys.size}개" }
        return map.getAll(keys)
    }

    /**
     * 캐시를 비웁니다.
     */
    fun clear() {
        log.debug { "캐시 초기화. map=${map.name}" }
        map.clear()
    }

    /**
     * IMap 리소스를 해제합니다.
     */
    fun destroy() {
        log.debug { "캐시 소멸. map=${map.name}" }
        map.destroy()
    }
}
