package io.bluetape4k.ahocorasick.trie

/**
 * Aho-Corasick 알고리즘에서 키워드 매칭이 발생했을 때 호출되는 핸들러 인터페이스.
 *
 * 이 인터페이스를 구현하여 매칭된 키워드를 처리하는 사용자 정의 로직을 제공할 수 있습니다.
 *
 * ```
 * val handler = EmitHandler { emit ->
 *     println("Found: ${emit.keyword} at position ${emit.start}-${emit.end}")
 *     true // 계속 처리
 * }
 * trie.runParseText(text, handler)
 * ```
 *
 * @see StatefulEmitHandler
 * @see DefaultEmitHandler
 */
fun interface EmitHandler {
    /**
     * 키워드 매칭이 발생했을 때 호출됩니다.
     *
     * @param emit 매칭된 키워드 정보 (시작 위치, 끝 위치, 키워드)
     * @return true를 반환하면 계속 처리하고, false를 반환하면 중단합니다 (stopOnHit 설정 시)
     */
    fun emit(emit: Emit): Boolean
}

/**
 * 상태를 유지하는 EmitHandler 인터페이스.
 *
 * 매칭된 모든 Emit을 리스트에 저장하여 나중에 조회할 수 있습니다.
 *
 * @see AbstractStatefulEmitHandler
 * @see DefaultEmitHandler
 */
interface StatefulEmitHandler: EmitHandler {
    /**
     * 수집된 모든 Emit 리스트.
     */
    val emits: MutableList<Emit>
}

/**
 * StatefulEmitHandler의 추상 구현 클래스.
 *
 * 기본적으로 모든 Emit을 내장 리스트에 저장합니다.
 * 사용자는 [emit] 메서드를 오버라이드하여 커스텀 로직을 구현할 수 있습니다.
 *
 * ```
 * val handler = object : AbstractStatefulEmitHandler() {
 *     override fun emit(emit: Emit): Boolean {
 *         // 3글자 이상인 키워드만 수집
 *         if (emit.keyword?.length ?: 0 >= 3) {
 *             return addEmit(emit)
 *         }
 *         return false
 *     }
 * }
 * ```
 */
abstract class AbstractStatefulEmitHandler: StatefulEmitHandler {
    /**
     * 수집된 Emit 리스트.
     */
    override val emits: MutableList<Emit> = mutableListOf()

    /**
     * Emit을 리스트에 추가합니다.
     *
     * @param emit 추가할 Emit 객체
     * @return 항상 true를 반환
     */
    fun addEmit(emit: Emit): Boolean = emits.add(emit)
}

/**
 * 기본 StatefulEmitHandler 구현 클래스.
 *
 * 모든 Emit을 그대로 리스트에 저장합니다.
 */
class DefaultEmitHandler: AbstractStatefulEmitHandler() {
    /**
     * Emit을 리스트에 추가합니다.
     *
     * @param emit 추가할 Emit 객체
     * @return 항상 true를 반환
     */
    override fun emit(emit: Emit): Boolean = addEmit(emit)
}
