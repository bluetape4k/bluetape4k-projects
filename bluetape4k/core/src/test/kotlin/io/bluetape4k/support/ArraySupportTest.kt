package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import kotlin.byteArrayOf
import kotlin.longArrayOf
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
    fun `setAll - 기본 타입 배열에서 모든 요소를 변환한다`() {
        IntArray(10).also { it.setAll { i -> i } }.let { arr ->
            arr.indices.all { idx -> arr[idx] == idx }.shouldBeTrue()
        }
        ByteArray(3).also { it.setAll { idx -> idx.toByte() } } shouldBeEqualTo byteArrayOf(0, 1, 2)
        ShortArray(3).also { it.setAll { idx -> (idx * 2).toShort() } } shouldBeEqualTo shortArrayOf(0, 2, 4)
        IntArray(3).also { it.setAll { idx -> idx * idx } } shouldBeEqualTo intArrayOf(0, 1, 4)
        LongArray(3).also { it.setAll { idx -> idx.toLong() + 10L } } shouldBeEqualTo longArrayOf(10L, 11L, 12L)
        FloatArray(3).also { it.setAll { idx -> idx.toFloat() / 2f } } shouldBeEqualTo floatArrayOf(0f, 0.5f, 1.0f)
        DoubleArray(3).also { it.setAll { idx -> idx.toDouble() * 1.5 } } shouldBeEqualTo doubleArrayOf(0.0, 1.5, 3.0)
        charArrayOf('a', 'a', 'a').also { it.setAll { idx -> ('a'.code + idx).toChar() } } shouldBeEqualTo charArrayOf('a', 'b', 'c')
        booleanArrayOf(false, false, false, false).also { it.setAll { idx -> idx % 2 == 0 } } shouldBeEqualTo booleanArrayOf(true, false, true, false)
        intArrayOf().also { it.setAll { it + 1 } }.size shouldBeEqualTo 0
    }

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
        result[1].isFailure.shouldBeTrue()
        result[2].getOrNull() shouldBeEqualTo 5
    }

    @Test
    fun `mapCatching keeps order and handles empty`() {
        intArrayOf(5, 4, 3).mapCatching { it }.map { it.getOrNull() } shouldBeEqualTo listOf(5, 4, 3)
        intArrayOf().mapCatching { it * 2 }.size shouldBeEqualTo 0
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
    fun `forEachCatching keeps order and handles empty`() {
        val acc = mutableListOf<Int>()
        intArrayOf(3, 2, 1).forEachCatching { acc += it }
        acc shouldBeEqualTo listOf(3, 2, 1)

        val acc2 = mutableListOf<Int>()
        val result = intArrayOf().forEachCatching { acc2 += it }
        acc2.size shouldBeEqualTo 0; result.all { it.isSuccess }.shouldBeTrue()
    }

    @Test
    fun `배열의 첫 마지막 요소 제거 및 설정`() {
        assertFailsWith<IllegalStateException> { emptyArray<String>().removeFirst() }
        arrayOf("one", "two", "three").removeFirst() shouldContainSame arrayOf("two", "three")

        assertFailsWith<IllegalStateException> { emptyArray<String>().removeLast() }
        arrayOf("one", "two", "three").removeLast() shouldBeEqualTo arrayOf("one", "two")

        val arr1 = arrayOf("one", "two", "three")
        arr1.setFirst("1"); arr1 shouldContainSame arrayOf("1", "two", "three")
        assertFailsWith<IllegalStateException> { emptyArray<Int>().setFirst(1) }

        val arr2 = arrayOf("one", "two", "three")
        arr2.setLast("3"); arr2 shouldBeEqualTo arrayOf("one", "two", "3")
        assertFailsWith<IllegalStateException> { emptyArray<Int>().setLast(3) }
    }

    @Test
    fun `leadingZeros - 앞 0 개수 계산`() {
        byteArrayOf().leadingZeros() shouldBeEqualTo 0
        byteArrayOf(0, 0, 1, 0).leadingZeros() shouldBeEqualTo 2
        byteArrayOf(1, 0, 0).leadingZeros() shouldBeEqualTo 0
        byteArrayOf(0, 0, 0).leadingZeros() shouldBeEqualTo 3

        shortArrayOf().leadingZeros() shouldBeEqualTo 0
        shortArrayOf(0, 0, 1, 0).leadingZeros() shouldBeEqualTo 2
        shortArrayOf(1, 0, 0).leadingZeros() shouldBeEqualTo 0
        shortArrayOf(0, 0, 0).leadingZeros() shouldBeEqualTo 3

        intArrayOf().leadingZeros() shouldBeEqualTo 0
        intArrayOf(0, 0, 1, 0).leadingZeros() shouldBeEqualTo 2
        intArrayOf(1, 0, 0).leadingZeros() shouldBeEqualTo 0
        intArrayOf(0, 0, 0).leadingZeros() shouldBeEqualTo 3

        longArrayOf().leadingZeros() shouldBeEqualTo 0
        longArrayOf(0L, 0L, 1L, 0L).leadingZeros() shouldBeEqualTo 2
        longArrayOf(1L, 0L, 0L).leadingZeros() shouldBeEqualTo 0
        longArrayOf(0L, 0L, 0L).leadingZeros() shouldBeEqualTo 3
    }

    @Test
    fun `padTo - 다양한 배열 타입에 요소를 추가한다`() {
        arrayOf(1, 2, 3).padTo(5, 0) shouldBeEqualTo arrayOf(1, 2, 3, 0, 0)
        intArrayOf(1, 2, 3).padTo(5, 0) shouldBeEqualTo intArrayOf(1, 2, 3, 0, 0)
        byteArrayOf(1, 2, 3).padTo(5, 0) shouldBeEqualTo byteArrayOf(1, 2, 3, 0, 0)
        longArrayOf(1L, 2L, 3L).padTo(5, 0L) shouldBeEqualTo longArrayOf(1L, 2L, 3L, 0L, 0L)
        floatArrayOf(1.0f, 2.0f, 3.0f).padTo(5, 0.0f) shouldBeEqualTo floatArrayOf(1.0f, 2.0f, 3.0f, 0.0f, 0.0f)
        doubleArrayOf(1.0, 2.0, 3.0).padTo(5, 0.0) shouldBeEqualTo doubleArrayOf(1.0, 2.0, 3.0, 0.0, 0.0)
        charArrayOf('a', 'b', 'c').padTo(5, 'x') shouldBeEqualTo charArrayOf('a', 'b', 'c', 'x', 'x')
        shortArrayOf(1, 2, 3).padTo(5, 0) shouldBeEqualTo shortArrayOf(1, 2, 3, 0, 0)
    }

    @Test
    fun `padTo - 크기가 같거나 작으면 배열을 변경하지 않는다`() {
        val arr = arrayOf(1, 2, 3)
        assertTrue { arr.padTo(3, 0) === arr }; assertTrue { arr.padTo(2, 0) === arr }

        val ints = intArrayOf(1, 2, 3)
        assertTrue { ints.padTo(3, 0) === ints }; assertTrue { ints.padTo(2, 0) === ints }

        val bytes = byteArrayOf(1, 2, 3)
        assertTrue { bytes.padTo(3, 0) === bytes }; assertTrue { bytes.padTo(2, 0) === bytes }

        val longs = longArrayOf(1L, 2L, 3L)
        assertTrue { longs.padTo(3, 0L) === longs }; assertTrue { longs.padTo(2, 0L) === longs }

        val floats = floatArrayOf(1.0f, 2.0f, 3.0f)
        assertTrue { floats.padTo(3, 0.0f) === floats }; assertTrue { floats.padTo(2, 0.0f) === floats }

        val doubles = doubleArrayOf(1.0, 2.0, 3.0)
        assertTrue { doubles.padTo(3, 0.0) === doubles }; assertTrue { doubles.padTo(2, 0.0) === doubles }

        val chars = charArrayOf('a', 'b', 'c')
        assertTrue { chars.padTo(3, 'x') === chars }; assertTrue { chars.padTo(2, 'x') === chars }

        val shorts = shortArrayOf(1, 2, 3)
        assertTrue { shorts.padTo(3, 0) === shorts }; assertTrue { shorts.padTo(2, 0) === shorts }
    }
}
