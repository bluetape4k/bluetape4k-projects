package io.bluetape4k.collections

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class VarargSupportTest {

    @Test
    fun `toVarargArray converts collection`() {
        val list: Collection<Any> = listOf("a", "b")
        val array = list.toVarargArray()
        array.size shouldBeEqualTo 2
        array[0] shouldBeEqualTo "a"
        array[1] shouldBeEqualTo "b"
    }
}
