package io.bluetape4k.ahocorasick.trie

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class StateTest {

    companion object: KLogging()

    @Test
    fun `ctor sequence of characters`() {
        val rootState = State()
        rootState.addStates('a', 'b', 'c')

        val nextState1 = rootState.nextState('a')

        nextState1.shouldNotBeNull()
        nextState1.depth shouldBeEqualTo 1

        val nextState2 = nextState1.nextState('b')
        nextState2.shouldNotBeNull()
        nextState2.depth shouldBeEqualTo 2

        val nextState3 = nextState2.nextState('c')
        nextState3.shouldNotBeNull()
        nextState3.depth shouldBeEqualTo 3
    }

    @Test
    fun `addEmit to State`() {
        val rootState = State()
        rootState.addEmit("ghi")
        rootState.addEmit("def")
        rootState.addEmit("abc")

        rootState.emit().toFastList() shouldBeEqualTo listOf("abc", "def", "ghi")
    }

    @Test
    fun `addEmits to State`() {
        val rootState = State()
        rootState.addEmits("ghi", "def", "abc")

        rootState.emit().toFastList() shouldBeEqualTo listOf("abc", "def", "ghi")
    }
}
