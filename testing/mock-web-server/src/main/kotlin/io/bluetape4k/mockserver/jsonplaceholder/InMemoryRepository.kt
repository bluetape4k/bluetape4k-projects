package io.bluetape4k.mockserver.jsonplaceholder

import io.bluetape4k.logging.KLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 메모리 기반 CRUD 저장소.
 *
 * jsonplaceholder 데이터를 메모리에 저장하고 관리한다.
 * 스레드 안전한 [ConcurrentHashMap]과 [AtomicLong] 기반 ID 시퀀스를 사용한다.
 *
 * @param T 저장할 엔티티 타입
 * @param idExtractor 엔티티에서 ID를 추출하는 함수
 * @param withId 엔티티에 새 ID를 적용하는 함수
 */
class InMemoryRepository<T : Any>(
    private val idExtractor: (T) -> Long,
    private val withId: (T, Long) -> T,
) {
    companion object : KLogging()

    private val store = ConcurrentHashMap<Long, T>()
    private val seq = AtomicLong(0L)

    /**
     * 전체 데이터를 새 목록으로 교체한다.
     *
     * @param items 새로 적재할 엔티티 목록
     */
    fun loadAll(items: List<T>) {
        store.clear()
        items.forEach { store[idExtractor(it)] = it }
        seq.set(items.maxOfOrNull { idExtractor(it) } ?: 0L)
    }

    /**
     * 모든 엔티티를 반환한다.
     *
     * @return 저장된 모든 엔티티 목록
     */
    fun all(): List<T> = store.values.toList()

    /**
     * ID로 단일 엔티티를 조회한다.
     *
     * @param id 조회할 엔티티 ID
     * @return 해당 ID의 엔티티, 없으면 null
     */
    fun find(id: Long): T? = store[id]

    /**
     * 새 엔티티를 추가하고 ID가 할당된 엔티티를 반환한다.
     *
     * @param item 추가할 엔티티
     * @return ID가 할당된 저장된 엔티티
     */
    fun add(item: T): T {
        val id = seq.incrementAndGet()
        val saved = withId(item, id)
        store[id] = saved
        return saved
    }

    /**
     * 기존 엔티티를 업데이트한다. 존재하지 않으면 null 반환.
     *
     * @param id 업데이트할 엔티티 ID
     * @param item 새 엔티티 데이터
     * @return 업데이트된 엔티티, 해당 ID가 없으면 null
     */
    fun update(id: Long, item: T): T? =
        store[id]?.let { withId(item, id).also { updated -> store[id] = updated } }

    /**
     * 엔티티를 삭제한다. 삭제 성공 여부를 반환한다.
     *
     * @param id 삭제할 엔티티 ID
     * @return 삭제 성공이면 true, 해당 ID가 없으면 false
     */
    fun delete(id: Long): Boolean = store.remove(id) != null

    /**
     * 현재 저장된 엔티티 수를 반환한다.
     *
     * @return 저장된 엔티티 수
     */
    fun count(): Int = store.size
}
