package io.bluetape4k.collections.eclipse

import io.bluetape4k.collections.AbstractCollectionTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class FastListSupportTest: AbstractCollectionTest() {

    companion object: KLogging()

    val expectedList = fastList(10) { it + 1 }

    @Test
    fun `빈 FastList 생성`() {
        val emptyList = emptyFastList<String>()

        emptyList.isEmpty.shouldBeTrue()
        emptyList.size shouldBeEqualTo 0
    }

    @Test
    fun `초기 size가 음수일때는 예외 발생`() {
        assertFailsWith<IllegalArgumentException> {
            fastList(-1) { it.toString() }
        }
    }

    @Test
    fun `초기화를 이용하여 FastList 생성하기`() {
        val list = fastList(10) { it }
        list.size shouldBeEqualTo 10
        list shouldBeEqualTo fastListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    }

    @Test
    fun `Collection 들을 FastList로 변환`() {
        (1..10).toFastList() shouldBeEqualTo expectedList
        (1..10).asSequence().toFastList() shouldBeEqualTo expectedList
        (1..10).iterator().toFastList() shouldBeEqualTo expectedList
        (1..10).toList().toTypedArray().toFastList() shouldBeEqualTo expectedList
    }
}
