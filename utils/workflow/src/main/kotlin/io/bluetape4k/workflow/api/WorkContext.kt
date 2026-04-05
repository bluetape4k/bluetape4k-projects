package io.bluetape4k.workflow.api

import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction

/**
 * 워크플로 실행 컨텍스트입니다.
 *
 * 내부적으로 [ConcurrentHashMap]을 사용해 스레드 안전성을 보장합니다.
 * ParallelFlow에서 여러 Work가 동시에 접근하더라도 안전합니다.
 *
 * ### 병렬 사용 시 주의사항
 * - 개별 `get`/`set` 연산은 스레드 안전합니다.
 * - **read-modify-write** 패턴(예: 값을 읽고 -> 변환하고 -> 다시 저장)은
 *   race condition이 발생할 수 있으므로, [compute] 메서드를 사용하세요.
 * - 병렬 플로우에서는 각 Work가 **서로 다른 키**를 사용하는 것을 권장합니다.
 *
 * ```kotlin
 * val ctx = WorkContext()
 * ctx["orderId"] = 42L
 * val orderId: Long? = ctx.get("orderId")
 *
 * // read-modify-write 안전 패턴
 * ctx.compute("counter") { _, old -> ((old as? Int) ?: 0) + 1 }
 * ```
 */
class WorkContext(
    private val store: ConcurrentHashMap<String, Any> = ConcurrentHashMap(),
) {
    /**
     * 키로 값을 조회합니다.
     *
     * @param key 조회할 키
     * @return 저장된 값 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T: Any> get(key: String): T? = store[key] as? T

    /**
     * 키-값을 저장합니다.
     *
     * @param key 저장할 키
     * @param value 저장할 값
     */
    operator fun set(key: String, value: Any) {
        store[key] = value
    }

    /**
     * 키를 제거합니다.
     *
     * @param key 제거할 키
     * @return 제거된 값 또는 null
     */
    fun remove(key: String): Any? = store.remove(key)

    /**
     * 키 존재 여부를 반환합니다.
     *
     * @param key 확인할 키
     * @return 키가 존재하면 true
     */
    fun contains(key: String): Boolean = store.containsKey(key)

    /**
     * 원자적 read-modify-write 연산을 수행합니다.
     *
     * [ConcurrentHashMap.compute]에 위임하여 race condition 없이 값을 갱신합니다.
     * 병렬 플로우에서 동일 키를 갱신해야 할 때 반드시 이 메서드를 사용하세요.
     *
     * @param key 대상 키
     * @param remapper (키, 기존값?) -> 새 값 (null 반환 시 키 제거)
     * @return 갱신된 값 또는 null
     */
    fun compute(key: String, remapper: BiFunction<String, Any?, Any?>): Any? =
        store.compute(key, remapper)

    /**
     * 현재 컨텍스트의 불변 스냅샷 복사본을 반환합니다.
     *
     * @return 현재 저장소의 불변 맵 복사본
     */
    fun snapshot(): Map<String, Any> = store.toMap()

    /**
     * 다른 컨텍스트의 값을 현재 컨텍스트에 병합합니다.
     *
     * [other]의 키가 현재 컨텍스트의 키와 충돌하면 [other]의 값이 우선합니다.
     *
     * @param other 병합할 컨텍스트
     */
    fun merge(other: WorkContext) {
        store.putAll(other.store)
    }

    override fun toString(): String = "WorkContext(${store.keys})"
}

/**
 * [WorkContext]를 생성합니다.
 *
 * ```kotlin
 * val ctx = workContext("orderId" to 42L, "userId" to "alice")
 * ```
 *
 * @param pairs 초기 키-값 쌍
 * @return 생성된 [WorkContext]
 */
fun workContext(vararg pairs: Pair<String, Any>): WorkContext =
    WorkContext(ConcurrentHashMap(pairs.toMap()))
