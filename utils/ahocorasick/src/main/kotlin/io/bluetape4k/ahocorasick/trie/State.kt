package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.ValueObject
import io.bluetape4k.logging.KLogging
import java.util.*

/**
 * Aho-Corasick 알고리즘의 상태를 나타내는 데이터 클래스.
 *
 * ## 동작/계약
 * - [depth]는 루트로부터 상태 깊이를 나타냅니다.
 * - 전이(success)/실패(failure)/emit 집합을 내부 mutable 상태로 유지합니다.
 * - 상태 전이 탐색은 [nextState], [addState]로 수행됩니다.
 *
 * ```kotlin
 * val root = State()
 * val next = root.addState('a')
 * // next.depth == 1
 * ```
 *
 * @property depth 상태의 깊이
 */
class State(val depth: Int = 0): ValueObject {

    companion object: KLogging()

    private val rootState: State? get() = if (depth == 0) this else null
    private val success = mutableMapOf<Char, State>()
    private val emits = TreeSet<String>()

    var failure: State? = null

    /** 문자 전이를 조회합니다. 루트에서 미스 시 루트 자신을 반환할 수 있습니다. */
    fun nextState(ch: Char, ignoreRootState: Boolean = false): State? {
        var nextState = this.success[ch]

        val canUseRootState = !ignoreRootState && nextState == null && rootState != null
        if (canUseRootState) {
            nextState = rootState
        }
        return nextState
    }

    /** 루트 fallback 없이 문자 전이를 조회합니다. */
    fun nextStateIgnoreRootState(ch: Char): State? = nextState(ch, true)

    /** 문자열의 각 문자에 대해 상태를 추가/탐색하고 마지막 상태를 반환합니다. */
    fun addState(keyword: String): State {
        var state = this
        keyword.forEach { state = state.addState(it) }
        return state
    }

    /** 단일 문자 전이를 추가하거나 기존 상태를 반환합니다. */
    fun addState(ch: Char): State {
        var nextState = nextStateIgnoreRootState(ch)
        if (nextState == null) {
            nextState = State(this.depth + 1)
            success[ch] = nextState
        }
        return nextState
    }

    /** 가변 인자 문자 전이를 순서대로 추가합니다. */
    fun addStates(vararg chars: Char): State {
        var state = this
        chars.forEach {
            state = state.addState(it)
        }
        return state
    }

    /** 현재 상태에 키워드 emit을 추가합니다. */
    fun addEmit(keyword: String) {
        this.emits.add(keyword)
    }

    /** 컬렉션 emit을 추가합니다. */
    fun addEmits(emits: Collection<String>) {
        this.emits.addAll(emits)
    }

    /** 가변 인자 emit을 추가합니다. */
    fun addEmits(vararg emits: String) {
        this.emits.addAll(emits)
    }

    /** 현재 상태의 emit 집합을 반환합니다. */
    fun emit(): Collection<String> = this.emits

    /** 자식 상태 컬렉션을 반환합니다. */
    fun getStates(): Collection<State> = this.success.values

    /** 가능한 전이 문자 컬렉션을 반환합니다. */
    fun getTransitions(): Collection<Char> = this.success.keys

    override fun toString(): String = "State(emits=$emits, failure=$failure)"
}
