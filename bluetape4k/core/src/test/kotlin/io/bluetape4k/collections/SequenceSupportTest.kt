package io.bluetape4k.collections

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class SequenceSupportTest {

    companion object: KLogging()

    @Test
    fun `build char sequence`() {
        val sequence = charSequenceOf('a', 'z', 2)
        val array = sequence.toCharArray()

        array.size shouldBeEqualTo sequence.count()
        sequence.forEachIndexed { index, value ->
            array[index] shouldBeEqualTo value
        }
    }

    @Test
    fun `build byte sequence`() {
        val sequence = byteSequenceOf(1, 100, 2)
        val array = sequence.toByteArray()

        array.size shouldBeEqualTo sequence.count()
        sequence.forEachIndexed { index, value ->
            array[index] shouldBeEqualTo value
        }
    }

    @Test
    fun `build int sequence`() {
        val sequence = intSequenceOf(1, 100, 2)
        val array = sequence.toIntArray()

        array.size shouldBeEqualTo sequence.count()
        sequence.forEachIndexed { index, value ->
            array[index] shouldBeEqualTo value
        }
    }

    @Test
    fun `build long sequence`() {
        val sequence = longSequenceOf(1L, 100L, 2L)
        val array = sequence.toLongArray()

        array.size shouldBeEqualTo sequence.count()
        sequence.forEachIndexed { index, value ->
            array[index] shouldBeEqualTo value
        }
    }

    @Test
    fun `build float sequence`() {
        val sequence = floatSequenceOf(1.0F, 10.0F, 0.5F)
        val array = sequence.toFloatArray()

        array.size shouldBeEqualTo sequence.count()
        sequence.forEachIndexed { index, value ->
            array[index] shouldBeEqualTo value
        }
    }

    @Test
    fun `build double sequence`() {
        val sequence = doubleSequenceOf(1.0, 10.0, 0.5)
        val array = sequence.toDoubleArray()

        array.size shouldBeEqualTo sequence.count()
        sequence.forEachIndexed { index, value ->
            array[index] shouldBeEqualTo value
        }
    }

    @Test
    fun `sliding 하기`() {
        val list = listOf(1, 2, 3, 4)

        val sliding = list.asSequence().sliding(3, false)
        sliding.toList() shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(2, 3, 4))

        val sliding2 = list.asSequence().sliding(3, true)
        sliding2.toList() shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(2, 3, 4), listOf(3, 4), listOf(4))
    }

    @Test
    fun `sequenceOf step must be positive`() {
        assertFailsWith<IllegalArgumentException> { charSequenceOf('a', 'z', 0) }
        assertFailsWith<IllegalArgumentException> { byteSequenceOf(0, 10, 0) }
        assertFailsWith<IllegalArgumentException> { intSequenceOf(1, 10, 0) }
        assertFailsWith<IllegalArgumentException> { longSequenceOf(1L, 10L, 0L) }
        assertFailsWith<IllegalArgumentException> { floatSequenceOf(1.0F, 10.0F, 0.0F) }
        assertFailsWith<IllegalArgumentException> { doubleSequenceOf(1.0, 10.0, 0.0) }
    }

    @Test
    fun `repeat on single-use sequence repeats with cache`() {
        val singleUse = listOf(1, 2, 3).iterator().asSequence()
        singleUse.repeat().take(10).toList() shouldBeEqualTo listOf(1, 2, 3, 1, 2, 3, 1, 2, 3, 1)
    }

    @Test
    fun `repeat on reusable sequence repeats`() {
        sequenceOf(1, 2).repeat().take(5).toList() shouldBeEqualTo listOf(1, 2, 1, 2, 1)
    }

    @Test
    fun `sequence array conversions`() {
        sequenceOf('a', 'b').asCharArray().contentEquals(charArrayOf('a', 'b')).shouldBeTrue()
        sequenceOf(1, 2).map { it.toShort() }.toShortArray().contentEquals(shortArrayOf(1, 2)).shouldBeTrue()
        sequenceOf<Any?>(1, null).asByteArray(fallback = 7).contentEquals(byteArrayOf(1, 7)).shouldBeTrue()
        sequenceOf(1, 2).asIntArray().contentEquals(intArrayOf(1, 2)).shouldBeTrue()
        sequenceOf(1L, 2L).asLongArray().contentEquals(longArrayOf(1L, 2L)).shouldBeTrue()
        sequenceOf(1.0F, 2.0F).asFloatArray().contentEquals(floatArrayOf(1.0F, 2.0F)).shouldBeTrue()
        sequenceOf(1.0, 2.0).asDoubleArray().contentEquals(doubleArrayOf(1.0, 2.0)).shouldBeTrue()
        sequenceOf("a", "b").asStringArray().contentEquals(arrayOf("a", "b")).shouldBeTrue()

        emptySequence<Short>().toShortArray().contentEquals(shortArrayOf()).shouldBeTrue()
        emptySequence<Any?>().asByteArray().contentEquals(byteArrayOf()).shouldBeTrue()

        val mixed = sequenceOf(1, "a").asArray<String>()
        mixed.size shouldBeEqualTo 2
        mixed[0] shouldBeEqualTo null
        mixed[1] shouldBeEqualTo "a"
    }

    @Test
    fun `sequence catching helpers preserve successful and failed elements`() {
        val mapped = sequenceOf(1, 0).mapCatching { 10 / it }.toList()
        mapped[0].getOrThrow() shouldBeEqualTo 10
        mapped[1].isFailure.shouldBeTrue()

        sequenceOf(1, 0).mapIfSuccess { 10 / it }.toList() shouldBeEqualTo listOf(10)

        var sum = 0
        sequenceOf(1, 0).tryForEach { sum += 10 / it }
        sum shouldBeEqualTo 10

        val actions = sequenceOf(1, 0).forEachCatching { 10 / it }.toList()
        actions[0].isSuccess.shouldBeTrue()
        actions[1].isFailure.shouldBeTrue()

        emptySequence<Int>().mapCatching { it }.toList() shouldBeEqualTo emptyList()
        emptySequence<Int>().mapIfSuccess { it }.toList() shouldBeEqualTo emptyList()
        emptySequence<Int>().forEachCatching { }.toList() shouldBeEqualTo emptyList()

        var emptyCount = 0
        emptySequence<Int>().tryForEach { emptyCount += 1 }
        emptyCount shouldBeEqualTo 0
    }

    @Test
    fun `sequence sliding transform and empty input`() {
        sequenceOf(1, 2, 3, 4).sliding(3, partialWindows = true) { it.sum() }
            .toList() shouldBeEqualTo listOf(6, 9, 7, 4)
        emptySequence<Int>().sliding(3).toList() shouldBeEqualTo emptyList()
    }
}
