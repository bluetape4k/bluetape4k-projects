package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class ObjectSupportTest {

    companion object: KLogging()

    @Test
    fun `요소 중 가장 빈번히 나오는 값을 찾습니다`() {
        modeOrNull(1, 2, 2, 3, 3, 3, 4, 5) shouldBeEqualTo 3
        modeOrNull(1, 2, 2, 3, 3, 4, 5).shouldBeNull()
        modeOrNull(1, 2, 3, 4, 5).shouldBeNull()
    }

    @Test
    fun `Iterable의 요소 중 가장 빈번히 나오는 값을 찾습니다`() {

        listOf(1, 2, 2, 3, 3, 3, 4, 5).modeOrNull() shouldBeEqualTo 3
        listOf(1, 2, 2, 3, 3, 4, 5).modeOrNull().shouldBeNull()
        listOf(1, 2, 3, 4, 5).modeOrNull().shouldBeNull()
    }

    @Test
    fun `Sequence의 요소 중 가장 빈번히 나오는 값을 찾습니다`() {
        sequenceOf(1, 2, 2, 3, 3, 3, 4, 5).modeOrNull() shouldBeEqualTo 3
        sequenceOf(1, 2, 2, 3, 3, 4, 5).modeOrNull().shouldBeNull()
        sequenceOf(1, 2, 3, 4, 5).modeOrNull().shouldBeNull()
    }

    @Test
    fun `컬렉션의 중간 값을 찾습니다`() {
        listOf(1, 2, 3, 4, 5).median() shouldBeEqualTo 3
        listOf(1, 1, 1).median() shouldBeEqualTo 1

        // 중간 값이 2개일 때는 작은 값을 반환합니다.
        listOf(1, 2, 3, 4).median() shouldBeEqualTo 2
        listOf(1, 2, 3, 4, 5, 6).median() shouldBeEqualTo 3
    }

    @Test
    fun `컬렉션의 중간값을 Custom Comparator로 찾습니다`() {
        // reverse order
        val comparator = Comparator<Int> { o1, o2 -> o2 - o1 }

        listOf(1, 2, 3, 4, 5).median(comparator) shouldBeEqualTo 3
        listOf(1, 1, 1).median(comparator) shouldBeEqualTo 1

        // 중간 값이 2개일 때는 작은 값을 반환합니다.
        listOf(1, 2, 3, 4).median(comparator) shouldBeEqualTo 3
        listOf(1, 2, 3, 4, 5, 6).median(comparator) shouldBeEqualTo 4
    }
}
