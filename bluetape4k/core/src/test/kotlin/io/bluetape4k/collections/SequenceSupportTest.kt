package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        assertThrows<IllegalArgumentException> { charSequenceOf('a', 'z', 0) }
        assertThrows<IllegalArgumentException> { byteSequenceOf(0, 10, 0) }
        assertThrows<IllegalArgumentException> { intSequenceOf(1, 10, 0) }
        assertThrows<IllegalArgumentException> { longSequenceOf(1L, 10L, 0L) }
        assertThrows<IllegalArgumentException> { floatSequenceOf(1.0F, 10.0F, 0.0F) }
        assertThrows<IllegalArgumentException> { doubleSequenceOf(1.0, 10.0, 0.0) }
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
        sequenceOf(1, 2).asIntArray().contentEquals(intArrayOf(1, 2)).shouldBeTrue()
        sequenceOf(1L, 2L).asLongArray().contentEquals(longArrayOf(1L, 2L)).shouldBeTrue()
        sequenceOf(1.0F, 2.0F).asFloatArray().contentEquals(floatArrayOf(1.0F, 2.0F)).shouldBeTrue()
        sequenceOf(1.0, 2.0).asDoubleArray().contentEquals(doubleArrayOf(1.0, 2.0)).shouldBeTrue()
        sequenceOf("a", "b").asStringArray().contentEquals(arrayOf("a", "b")).shouldBeTrue()

        val mixed = sequenceOf(1, "a").asArray<String>()
        mixed.size shouldBeEqualTo 2
        mixed[0] shouldBeEqualTo null
        mixed[1] shouldBeEqualTo "a"
    }
}
