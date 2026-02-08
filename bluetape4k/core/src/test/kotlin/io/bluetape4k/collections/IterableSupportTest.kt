package io.bluetape4k.collections

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test

class IterableSupportTest {

    companion object: KLogging()

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun `try mapping`() {
        val origin = fastList(10) { it + 1 }

        val result = origin.tryMap { it / it }
        result.all { it.isSuccess }.shouldBeTrue()

        val result2 = origin.tryMap { it / 0 }
        result2.all { it.isFailure }.shouldBeTrue()
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun `mapping 시 성공한 것만 반환`() {
        val origin = fastList(10) { it + 1 }

        val result = origin.mapIfSuccess { it / it }
        result shouldContainAll listOf(1)

        val result2 = origin.mapIfSuccess { it / 0 }
        result2.shouldBeEmpty()
    }

    @Test
    fun `list 를 chunk 하기 - size 남기기`() {
        val list = listOf(1, 2, 3, 4, 5)
        val chunks = list.chunked(3)
        chunks shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(4, 5))
    }

    @Test
    fun `list 를 chunk 하기 - size`() {
        val list = listOf(1, 2, 3, 4, 5, 6)
        val chunks = list.chunked(3)
        chunks shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(4, 5, 6))
    }

    @Test
    fun `sliding 하기`() {
        val list = listOf(1, 2, 3, 4)

        val sliding = list.sliding(3, false)
        sliding shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(2, 3, 4))

        val sliding2 = list.sliding(3, true)
        sliding2 shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(2, 3, 4), listOf(3, 4), listOf(4))
    }

    @Test
    fun `windowing 하기`() {
        val list = listOf(1, 2, 3, 4, 5)

        val windowed = list.windowed(3, 2, false)
        windowed shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(3, 4, 5))

        val windowed2 = list.windowed(3, 2, true)
        windowed2 shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(3, 4, 5), listOf(5))
    }

    @Test
    fun `iterator and iterable helpers`() {
        emptyIterator<Int>().hasNext().shouldBeFalse()
        emptyListIterator<Int>().hasNext().shouldBeFalse()

        val iterator = listOf(1, 2, 3).iterator()
        iterator.asIterable().toList() shouldBeEqualTo listOf(1, 2, 3)

        val iterator2 = listOf(4, 5).iterator()
        iterator2.toList() shouldBeEqualTo listOf(4, 5)

        val iterator3 = listOf(6, 7).iterator()
        iterator3.toMutableList() shouldBeEqualTo mutableListOf(6, 7)
    }

    @Test
    fun `size exists and same elements`() {
        val nonCollection: Iterable<Int> = Iterable { sequenceOf(1, 2, 3).iterator() }
        nonCollection.size() shouldBeEqualTo 3
        nonCollection.exists { it == 2 }.shouldBeTrue()

        listOf(1, 2, 3).isSameElements(listOf(1, 2, 3)).shouldBeTrue()
        listOf(1, 2, 3).isSameElements(listOf(1, 3, 2)).shouldBeFalse()

        val seqA: Iterable<Int> = Iterable { sequenceOf(1, 2).iterator() }
        val seqB: Iterable<Int> = Iterable { sequenceOf(1, 2).iterator() }
        seqA.isSameElements(seqB).shouldBeTrue()
    }

    @Test
    fun `as array conversions`() {
        listOf('a', 'b').asCharArray().contentEquals(charArrayOf('a', 'b')).shouldBeTrue()
        listOf(1, 2).asIntArray().contentEquals(intArrayOf(1, 2)).shouldBeTrue()
        listOf(1L, 2L).asLongArray().contentEquals(longArrayOf(1L, 2L)).shouldBeTrue()
        listOf(1.0F, 2.0F).asFloatArray().contentEquals(floatArrayOf(1.0F, 2.0F)).shouldBeTrue()
        listOf(1.0, 2.0).asDoubleArray().contentEquals(doubleArrayOf(1.0, 2.0)).shouldBeTrue()
        listOf("a", "b").asStringArray().contentEquals(arrayOf("a", "b")).shouldBeTrue()

        val mixed = listOf(1, "a").asArray<String>()
        mixed.size shouldBeEqualTo 2
        mixed[0] shouldBeEqualTo null
        mixed[1] shouldBeEqualTo "a"
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun `catching helpers`() {
        val results = listOf(1, 0).mapCatching { 1 / it }
        results[0].isSuccess.shouldBeTrue()
        results[1].isFailure.shouldBeTrue()

        var count = 0
        listOf(1, 0).tryForEach {
            count += 1 / it
        }
        count shouldBeEqualTo 1

        val forEachResults = listOf(1, 0).forEachCatching { 1 / it }
        forEachResults.size shouldBeEqualTo 2
        forEachResults[0].isSuccess.shouldBeTrue()
        forEachResults[1].isFailure.shouldBeTrue()
    }
}
