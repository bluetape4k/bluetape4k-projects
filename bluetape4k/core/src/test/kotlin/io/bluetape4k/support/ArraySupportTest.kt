package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArraySupportTest {

    companion object: KLogging()

    @Test
    fun `길이가 0 인 array`() {
        emptyByteArray.count() shouldBeEqualTo 0
        emptyByteArray.isNullOrEmpty().shouldBeTrue()
        emptyByteArray.isNotEmpty().shouldBeFalse()
    }

    @Test
    fun `set all to array`() {
        val array = IntArray(10)
        array.setAll { it }
        array.indices.all { idx -> array[idx] == idx }.shouldBeTrue()
    }

    @Test
    fun `setAll ByteArray`() {
        val array = ByteArray(3)
        array.setAll { idx -> idx.toByte() }
        array shouldBeEqualTo byteArrayOf(0, 1, 2)
    }

    @Test
    fun `setAll ShortArray`() {
        val array = ShortArray(3)
        array.setAll { idx -> (idx * 2).toShort() }
        array shouldBeEqualTo shortArrayOf(0, 2, 4)
    }

    @Test
    fun `setAll IntArray`() {
        val array = IntArray(3)
        array.setAll { idx -> idx * idx }
        array shouldBeEqualTo intArrayOf(0, 1, 4)
    }

    @Test
    fun `setAll LongArray`() {
        val array = LongArray(3)
        array.setAll { idx -> idx.toLong() + 10L }
        array shouldBeEqualTo longArrayOf(10L, 11L, 12L)
    }

    @Test
    fun `setAll FloatArray`() {
        val array = FloatArray(3)
        array.setAll { idx -> idx.toFloat() / 2f }
        array shouldBeEqualTo floatArrayOf(0f, 0.5f, 1.0f)
    }

    @Test
    fun `setAll DoubleArray`() {
        val array = DoubleArray(3)
        array.setAll { idx -> idx.toDouble() * 1.5 }
        array shouldBeEqualTo doubleArrayOf(0.0, 1.5, 3.0)
    }

    @Test
    fun `setAll CharArray`() {
        val array = charArrayOf('a', 'a', 'a')
        array.setAll { idx -> ('a'.code + idx).toChar() }
        array shouldBeEqualTo charArrayOf('a', 'b', 'c')
    }

    @Test
    fun `setAll BooleanArray`() {
        val array = booleanArrayOf(false, false, false, false)
        array.setAll { idx -> idx % 2 == 0 }
        array shouldBeEqualTo booleanArrayOf(true, false, true, false)
    }

    @Test
    fun `setAll empty array is no-op`() {
        val intArray = intArrayOf()
        val result = intArray.setAll { it + 1 }
        result.size shouldBeEqualTo 0
    }

    /**
     * Maps each element of this array using [transform], capturing exceptions
     * as [Result.failure] instead of throwing.
     *
     * The transformation is applied sequentially and preserves the original order.
     * If an exception occurs for an element, processing continues for remaining elements.
     *
     * @return a list of [Result], one for each element in this array
     */
    @Test
    fun `mapCatching all success`() {
        val source = intArrayOf(1, 2, 3)
        val result = source.mapCatching { it * 2 }

        result shouldBeEqualTo listOf(
            Result.success(2),
            Result.success(4),
            Result.success(6)
        )
    }

    @Test
    fun `mapCatching with exception`() {
        val source = intArrayOf(1, 0, 2)
        val result = source.mapCatching { 10 / it }

        result[0].getOrNull() shouldBeEqualTo 10
        result[1].isFailure shouldBeEqualTo true
        result[2].getOrNull() shouldBeEqualTo 5
    }

    @Test
    fun `mapCatching keeps order`() {
        val source = intArrayOf(5, 4, 3)
        val result = source.mapCatching { it }

        result.map { it.getOrNull() } shouldBeEqualTo listOf(5, 4, 3)
    }

    @Test
    fun `mapCatching empty array`() {
        val source = intArrayOf()
        val result = source.mapCatching { it * 2 }

        result.size shouldBeEqualTo 0
    }

    @Test
    fun `forEachCatching all success`() {
        val source = intArrayOf(1, 2, 3)
        val acc = mutableListOf<Int>()

        val result = source.forEachCatching { acc += it * 2 }

        acc shouldBeEqualTo listOf(2, 4, 6)
        result.all { it.isSuccess }.shouldBeTrue()
    }

    @Test
    fun `forEachCatching with exception continues`() {
        val source = intArrayOf(1, 0, 2)
        val acc = mutableListOf<Int>()

        val result = source.forEachCatching { acc += 10 / it }

        acc shouldBeEqualTo listOf(10, 5)
        result[1].isFailure.shouldBeTrue()
        result[1].exceptionOrNull() shouldBeInstanceOf ArithmeticException::class
    }

    @Test
    fun `forEachCatching keeps order`() {
        val source = intArrayOf(3, 2, 1)
        val acc = mutableListOf<Int>()

        source.forEachCatching { acc += it }

        acc shouldBeEqualTo listOf(3, 2, 1)
    }

    @Test
    fun `forEachCatching empty array`() {
        val source = intArrayOf()
        val acc = mutableListOf<Int>()

        val result = source.forEachCatching { acc += it }

        acc.size shouldBeEqualTo 0
        result.all { it.isSuccess }.shouldBeTrue()
    }

    @Test
    fun `remove first element`() {
        assertFailsWith<IllegalStateException> {
            emptyArray<String>().removeFirst()
        }

        val array = arrayOf("one", "two", "three")
        val array2 = array.removeFirst()
        array2 shouldContainSame arrayOf("two", "three")
    }

    @Test
    fun `remove last element`() {
        assertFailsWith<IllegalStateException> {
            emptyArray<String>().removeLast()
        }

        val array = arrayOf("one", "two", "three")
        val array2 = array.removeLast()
        array2 shouldBeEqualTo arrayOf("one", "two")
    }

    @Test
    fun `set first element`() {
        val array = arrayOf("one", "two", "three")
        array.setFirst("1")
        array shouldContainSame arrayOf("1", "two", "three")

        assertFailsWith<IllegalStateException> {
            val emptyArray = emptyArray<Int>()
            emptyArray.setFirst(1)
        }
    }

    @Test
    fun `set last element`() {
        val array = arrayOf("one", "two", "three")
        array.setLast("3")
        array shouldBeEqualTo arrayOf("one", "two", "3")

        assertFailsWith<IllegalStateException> {
            val emptyArray = emptyArray<Int>()
            emptyArray.setLast(3)
        }
    }

    @Test
    fun `leadingZeros ByteArray`() {
        byteArrayOf().leadingZeros() shouldBeEqualTo 0
        byteArrayOf(0, 0, 1, 0).leadingZeros() shouldBeEqualTo 2
        byteArrayOf(1, 0, 0).leadingZeros() shouldBeEqualTo 0
        byteArrayOf(0, 0, 0).leadingZeros() shouldBeEqualTo 3
    }

    @Test
    fun `leadingZeros ShortArray`() {
        shortArrayOf().leadingZeros() shouldBeEqualTo 0
        shortArrayOf(0, 0, 1, 0).leadingZeros() shouldBeEqualTo 2
        shortArrayOf(1, 0, 0).leadingZeros() shouldBeEqualTo 0
        shortArrayOf(0, 0, 0).leadingZeros() shouldBeEqualTo 3
    }

    @Test
    fun `leadingZeros IntArray`() {
        intArrayOf().leadingZeros() shouldBeEqualTo 0
        intArrayOf(0, 0, 1, 0).leadingZeros() shouldBeEqualTo 2
        intArrayOf(1, 0, 0).leadingZeros() shouldBeEqualTo 0
        intArrayOf(0, 0, 0).leadingZeros() shouldBeEqualTo 3
    }

    @Test
    fun `leadingZeros LongArray`() {
        longArrayOf().leadingZeros() shouldBeEqualTo 0
        longArrayOf(0L, 0L, 1L, 0L).leadingZeros() shouldBeEqualTo 2
        longArrayOf(1L, 0L, 0L).leadingZeros() shouldBeEqualTo 0
        longArrayOf(0L, 0L, 0L).leadingZeros() shouldBeEqualTo 3
    }

    @Test
    fun `padTo array`() {
        val array = arrayOf(1, 2, 3)

        val paddedArray = array.padTo(5, 0)
        paddedArray shouldBeEqualTo arrayOf(1, 2, 3, 0, 0)
    }

    @Test
    fun `padTo does not shrink or drop data`() {
        val array = arrayOf(1, 2, 3)

        assertTrue { array.padTo(3, 0) === array }
        assertTrue { array.padTo(2, 0) === array }
    }

    @Test
    fun `padTo IntArray`() {
        val array = intArrayOf(1, 2, 3)

        val paddedArray = array.padTo(5, 0)
        paddedArray shouldBeEqualTo intArrayOf(1, 2, 3, 0, 0)
    }

    @Test
    fun `padTo IntArray does not shrink or drop data`() {
        val array = intArrayOf(1, 2, 3)

        assertTrue { array.padTo(3, 0) === array }
        assertTrue { array.padTo(2, 0) === array }
    }

    @Test
    fun `padTo ByteArray`() {
        val array = byteArrayOf(1, 2, 3)

        val paddedArray = array.padTo(5, 0)
        paddedArray shouldBeEqualTo byteArrayOf(1, 2, 3, 0, 0)
    }

    @Test
    fun `padTo ByteArray does not shrink or drop data`() {
        val array = byteArrayOf(1, 2, 3)

        assertTrue { array.padTo(3, 0) === array }
        assertTrue { array.padTo(2, 0) === array }
    }

    @Test
    fun `padTo LongArray`() {
        val array = longArrayOf(1L, 2L, 3L)

        val paddedArray = array.padTo(5, 0L)
        paddedArray shouldBeEqualTo longArrayOf(1L, 2L, 3L, 0L, 0L)
    }

    @Test
    fun `padTo LongArray does not shrink or drop data`() {
        val array = longArrayOf(1L, 2L, 3L)

        assertTrue { array.padTo(3, 0L) === array }
        assertTrue { array.padTo(2, 0L) === array }
    }

    @Test
    fun `padTo FloatArray`() {
        val array = floatArrayOf(1.0f, 2.0f, 3.0f)

        val paddedArray = array.padTo(5, 0.0f)
        paddedArray shouldBeEqualTo floatArrayOf(1.0f, 2.0f, 3.0f, 0.0f, 0.0f)
    }

    @Test
    fun `padTo FloatArray does not shrink or drop data`() {
        val array = floatArrayOf(1.0f, 2.0f, 3.0f)

        assertTrue { array.padTo(3, 0.0f) === array }
        assertTrue { array.padTo(2, 0.0f) === array }
    }

    @Test
    fun `padTo DoubleArray`() {
        val array = doubleArrayOf(1.0, 2.0, 3.0)

        val paddedArray = array.padTo(5, 0.0)
        paddedArray shouldBeEqualTo doubleArrayOf(1.0, 2.0, 3.0, 0.0, 0.0)
    }

    @Test
    fun `padTo DoubleArray does not shrink or drop data`() {
        val array = doubleArrayOf(1.0, 2.0, 3.0)

        assertTrue { array.padTo(3, 0.0) === array }
        assertTrue { array.padTo(2, 0.0) === array }
    }

    @Test
    fun `padTo CharArray`() {
        val array = charArrayOf('a', 'b', 'c')

        val paddedArray = array.padTo(5, 'x')
        paddedArray shouldBeEqualTo charArrayOf('a', 'b', 'c', 'x', 'x')
    }

    @Test
    fun `padTo CharArray does not shrink or drop data`() {
        val array = charArrayOf('a', 'b', 'c')

        assertTrue { array.padTo(3, 'x') === array }
        assertTrue { array.padTo(2, 'x') === array }
    }

    @Test
    fun `padTo ShortArray`() {
        val array = shortArrayOf(1, 2, 3)

        val paddedArray = array.padTo(5, 0)
        paddedArray shouldBeEqualTo shortArrayOf(1, 2, 3, 0, 0)
    }

    @Test
    fun `padTo ShortArray does not shrink or drop data`() {
        val array = shortArrayOf(1, 2, 3)

        assertTrue { array.padTo(3, 0) === array }
        assertTrue { array.padTo(2, 0) === array }
    }
}
