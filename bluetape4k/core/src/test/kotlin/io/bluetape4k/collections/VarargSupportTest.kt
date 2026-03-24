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

    class TestList<ID: Any>: ArrayList<ID>() {
        /**
         * 이 것 처럼 기존 Generic Type Class 가 reified 를 지원하지 않을 때에는 `toTypedArray()`를 사용하지 못합니다.
         */
        fun reverseArray(): Array<ID> =
            this.reverse().let { toVarargArray() }
    }

    @Test
    fun `toVarargArray converts generic collection`() {
        val list = TestList<String>().apply { add("a"); add("b") }

        val reverseArray = list.reverseArray()
        reverseArray.size shouldBeEqualTo 2
        reverseArray[0] shouldBeEqualTo "b"
        reverseArray[1] shouldBeEqualTo "a"
    }
}
