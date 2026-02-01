package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.ValueObject
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.logging.KLogging
import java.util.*

/**
 * Aho-Corasick 알고리즘의 상태를 나타내는 데이터 클래스.
 *
 * @property depth 상태의 깊이
 */
class State(val depth: Int = 0): ValueObject {

    companion object: KLogging()

    private val rootState: State? get() = if (depth == 0) this else null
    private val success = unifiedMapOf<Char, State>()
    private val emits = TreeSet<String>()

    var failure: State? = null

    fun nextState(ch: Char, ignoreRootState: Boolean = false): State? {
        var nextState = this.success[ch]

        val canUseRootState = !ignoreRootState && nextState == null && rootState != null
        if (canUseRootState) {
            nextState = rootState
        }
        return nextState
    }

    fun nextStateIgnoreRootState(ch: Char): State? = nextState(ch, true)

    fun addState(keyword: String): State {
        var state = this
        keyword.forEach { state = state.addState(it) }
        return state
    }

    fun addState(ch: Char): State {
        var nextState = nextStateIgnoreRootState(ch)
        if (nextState == null) {
            nextState = State(this.depth + 1)
            success[ch] = nextState
        }
        return nextState
    }

    fun addStates(vararg chars: Char): State {
        var state = this
        chars.forEach {
            state = state.addState(it)
        }
        return state
    }

    fun addEmit(keyword: String) {
        this.emits.add(keyword)
    }

    fun addEmits(emits: Collection<String>) {
        this.emits.addAll(emits)
    }

    fun addEmits(vararg emits: String) {
        this.emits.addAll(emits)
    }

    fun emit(): Collection<String> = this.emits

    fun getStates(): Collection<State> = this.success.values

    fun getTransitions(): Collection<Char> = this.success.keys

    override fun toString(): String = "State(emits=$emits, failure=$failure)"
}
