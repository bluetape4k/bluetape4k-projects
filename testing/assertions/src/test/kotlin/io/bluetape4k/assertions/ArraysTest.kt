package io.bluetape4k.assertions

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class ArraysTest {

    // ── IntArray ──────────────────────────────────────────────────────────────

    @Test
    fun `IntArray shouldContain passes when element exists`() {
        intArrayOf(1, 2, 3).shouldContain(2)
    }

    @Test
    fun `IntArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            intArrayOf(1, 2, 3).shouldContain(99)
        }
    }

    @Test
    fun `IntArray shouldContain fails when null`() {
        assertFailsWith<AssertionFailedError> {
            val arr: IntArray? = null
            arr.shouldContain(1)
        }
    }

    @Test
    fun `IntArray shouldNotContain passes when element absent`() {
        intArrayOf(1, 2, 3).shouldNotContain(99)
    }

    @Test
    fun `IntArray shouldNotContain passes when null`() {
        val arr: IntArray? = null
        arr.shouldNotContain(1)
    }

    @Test
    fun `IntArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            intArrayOf(1, 2, 3).shouldNotContain(2)
        }
    }

    @Test
    fun `IntArray shouldHaveSize passes with correct size`() {
        intArrayOf(1, 2, 3).shouldHaveSize(3)
    }

    @Test
    fun `IntArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            intArrayOf(1, 2, 3).shouldHaveSize(5)
        }
    }

    @Test
    fun `IntArray shouldBeEmpty passes for empty array`() {
        intArrayOf().shouldBeEmpty()
    }

    @Test
    fun `IntArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            intArrayOf(1).shouldBeEmpty()
        }
    }

    @Test
    fun `IntArray shouldNotBeEmpty passes for non-empty array`() {
        intArrayOf(1, 2).shouldNotBeEmpty()
    }

    @Test
    fun `IntArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            intArrayOf().shouldNotBeEmpty()
        }
    }

    @Test
    fun `IntArray shouldNotBeEmpty fails for null`() {
        assertFailsWith<AssertionFailedError> {
            val arr: IntArray? = null
            arr.shouldNotBeEmpty()
        }
    }

    // ── LongArray ─────────────────────────────────────────────────────────────

    @Test
    fun `LongArray shouldContain passes when element exists`() {
        longArrayOf(1L, 2L, 3L).shouldContain(2L)
    }

    @Test
    fun `LongArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            longArrayOf(1L, 2L, 3L).shouldContain(99L)
        }
    }

    @Test
    fun `LongArray shouldNotContain passes when element absent`() {
        longArrayOf(1L, 2L, 3L).shouldNotContain(99L)
    }

    @Test
    fun `LongArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            longArrayOf(1L, 2L, 3L).shouldNotContain(2L)
        }
    }

    @Test
    fun `LongArray shouldHaveSize passes with correct size`() {
        longArrayOf(1L, 2L).shouldHaveSize(2)
    }

    @Test
    fun `LongArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            longArrayOf(1L, 2L).shouldHaveSize(5)
        }
    }

    @Test
    fun `LongArray shouldBeEmpty passes for empty array`() {
        longArrayOf().shouldBeEmpty()
    }

    @Test
    fun `LongArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            longArrayOf(1L).shouldBeEmpty()
        }
    }

    @Test
    fun `LongArray shouldNotBeEmpty passes for non-empty array`() {
        longArrayOf(1L, 2L).shouldNotBeEmpty()
    }

    @Test
    fun `LongArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            longArrayOf().shouldNotBeEmpty()
        }
    }

    // ── DoubleArray ───────────────────────────────────────────────────────────

    @Test
    fun `DoubleArray shouldContain passes when element exists`() {
        doubleArrayOf(1.0, 2.0, 3.0).shouldContain(2.0)
    }

    @Test
    fun `DoubleArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            doubleArrayOf(1.0, 2.0).shouldContain(99.0)
        }
    }

    @Test
    fun `DoubleArray shouldNotContain passes when element absent`() {
        doubleArrayOf(1.0, 2.0).shouldNotContain(99.0)
    }

    @Test
    fun `DoubleArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            doubleArrayOf(1.0, 2.0).shouldNotContain(1.0)
        }
    }

    @Test
    fun `DoubleArray shouldHaveSize passes with correct size`() {
        doubleArrayOf(1.0, 2.0, 3.0).shouldHaveSize(3)
    }

    @Test
    fun `DoubleArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            doubleArrayOf(1.0).shouldHaveSize(3)
        }
    }

    @Test
    fun `DoubleArray shouldBeEmpty passes for empty array`() {
        doubleArrayOf().shouldBeEmpty()
    }

    @Test
    fun `DoubleArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            doubleArrayOf(1.0).shouldBeEmpty()
        }
    }

    @Test
    fun `DoubleArray shouldNotBeEmpty passes for non-empty array`() {
        doubleArrayOf(1.0).shouldNotBeEmpty()
    }

    @Test
    fun `DoubleArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            doubleArrayOf().shouldNotBeEmpty()
        }
    }

    // ── FloatArray ────────────────────────────────────────────────────────────

    @Test
    fun `FloatArray shouldContain passes when element exists`() {
        floatArrayOf(1.0f, 2.0f, 3.0f).shouldContain(2.0f)
    }

    @Test
    fun `FloatArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            floatArrayOf(1.0f, 2.0f).shouldContain(99.0f)
        }
    }

    @Test
    fun `FloatArray shouldNotContain passes when element absent`() {
        floatArrayOf(1.0f, 2.0f).shouldNotContain(99.0f)
    }

    @Test
    fun `FloatArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            floatArrayOf(1.0f, 2.0f).shouldNotContain(1.0f)
        }
    }

    @Test
    fun `FloatArray shouldHaveSize passes with correct size`() {
        floatArrayOf(1.0f, 2.0f).shouldHaveSize(2)
    }

    @Test
    fun `FloatArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            floatArrayOf(1.0f).shouldHaveSize(3)
        }
    }

    @Test
    fun `FloatArray shouldBeEmpty passes for empty array`() {
        floatArrayOf().shouldBeEmpty()
    }

    @Test
    fun `FloatArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            floatArrayOf(1.0f).shouldBeEmpty()
        }
    }

    @Test
    fun `FloatArray shouldNotBeEmpty passes for non-empty array`() {
        floatArrayOf(1.0f).shouldNotBeEmpty()
    }

    @Test
    fun `FloatArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            floatArrayOf().shouldNotBeEmpty()
        }
    }

    // ── ByteArray ─────────────────────────────────────────────────────────────

    @Test
    fun `ByteArray shouldContain passes when element exists`() {
        byteArrayOf(1, 2, 3).shouldContain(2)
    }

    @Test
    fun `ByteArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            byteArrayOf(1, 2, 3).shouldContain(99)
        }
    }

    @Test
    fun `ByteArray shouldNotContain passes when element absent`() {
        byteArrayOf(1, 2, 3).shouldNotContain(99)
    }

    @Test
    fun `ByteArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            byteArrayOf(1, 2, 3).shouldNotContain(1)
        }
    }

    @Test
    fun `ByteArray shouldHaveSize passes with correct size`() {
        byteArrayOf(1, 2, 3).shouldHaveSize(3)
    }

    @Test
    fun `ByteArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            byteArrayOf(1, 2).shouldHaveSize(5)
        }
    }

    @Test
    fun `ByteArray shouldBeEmpty passes for empty array`() {
        byteArrayOf().shouldBeEmpty()
    }

    @Test
    fun `ByteArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            byteArrayOf(1).shouldBeEmpty()
        }
    }

    @Test
    fun `ByteArray shouldNotBeEmpty passes for non-empty array`() {
        byteArrayOf(1, 2).shouldNotBeEmpty()
    }

    @Test
    fun `ByteArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            byteArrayOf().shouldNotBeEmpty()
        }
    }

    // ── ShortArray ────────────────────────────────────────────────────────────

    @Test
    fun `ShortArray shouldContain passes when element exists`() {
        shortArrayOf(1, 2, 3).shouldContain(2)
    }

    @Test
    fun `ShortArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            shortArrayOf(1, 2, 3).shouldContain(99)
        }
    }

    @Test
    fun `ShortArray shouldNotContain passes when element absent`() {
        shortArrayOf(1, 2, 3).shouldNotContain(99)
    }

    @Test
    fun `ShortArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            shortArrayOf(1, 2, 3).shouldNotContain(2)
        }
    }

    @Test
    fun `ShortArray shouldHaveSize passes with correct size`() {
        shortArrayOf(1, 2).shouldHaveSize(2)
    }

    @Test
    fun `ShortArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            shortArrayOf(1, 2).shouldHaveSize(5)
        }
    }

    @Test
    fun `ShortArray shouldBeEmpty passes for empty array`() {
        shortArrayOf().shouldBeEmpty()
    }

    @Test
    fun `ShortArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            shortArrayOf(1).shouldBeEmpty()
        }
    }

    @Test
    fun `ShortArray shouldNotBeEmpty passes for non-empty array`() {
        shortArrayOf(1, 2).shouldNotBeEmpty()
    }

    @Test
    fun `ShortArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            shortArrayOf().shouldNotBeEmpty()
        }
    }

    // ── CharArray ─────────────────────────────────────────────────────────────

    @Test
    fun `CharArray shouldContain passes when element exists`() {
        charArrayOf('a', 'b', 'c').shouldContain('b')
    }

    @Test
    fun `CharArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            charArrayOf('a', 'b', 'c').shouldContain('z')
        }
    }

    @Test
    fun `CharArray shouldNotContain passes when element absent`() {
        charArrayOf('a', 'b', 'c').shouldNotContain('z')
    }

    @Test
    fun `CharArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            charArrayOf('a', 'b', 'c').shouldNotContain('a')
        }
    }

    @Test
    fun `CharArray shouldHaveSize passes with correct size`() {
        charArrayOf('a', 'b', 'c').shouldHaveSize(3)
    }

    @Test
    fun `CharArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            charArrayOf('a').shouldHaveSize(3)
        }
    }

    @Test
    fun `CharArray shouldBeEmpty passes for empty array`() {
        charArrayOf().shouldBeEmpty()
    }

    @Test
    fun `CharArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            charArrayOf('a').shouldBeEmpty()
        }
    }

    @Test
    fun `CharArray shouldNotBeEmpty passes for non-empty array`() {
        charArrayOf('a', 'b').shouldNotBeEmpty()
    }

    @Test
    fun `CharArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            charArrayOf().shouldNotBeEmpty()
        }
    }

    // ── BooleanArray ──────────────────────────────────────────────────────────

    @Test
    fun `BooleanArray shouldContain passes when element exists`() {
        booleanArrayOf(true, false).shouldContain(false)
    }

    @Test
    fun `BooleanArray shouldContain fails when element missing`() {
        assertFailsWith<AssertionFailedError> {
            booleanArrayOf(true).shouldContain(false)
        }
    }

    @Test
    fun `BooleanArray shouldNotContain passes when element absent`() {
        booleanArrayOf(true).shouldNotContain(false)
    }

    @Test
    fun `BooleanArray shouldNotContain fails when element present`() {
        assertFailsWith<AssertionFailedError> {
            booleanArrayOf(true, false).shouldNotContain(true)
        }
    }

    @Test
    fun `BooleanArray shouldHaveSize passes with correct size`() {
        booleanArrayOf(true, false).shouldHaveSize(2)
    }

    @Test
    fun `BooleanArray shouldHaveSize fails with wrong size`() {
        assertFailsWith<AssertionFailedError> {
            booleanArrayOf(true).shouldHaveSize(3)
        }
    }

    @Test
    fun `BooleanArray shouldBeEmpty passes for empty array`() {
        booleanArrayOf().shouldBeEmpty()
    }

    @Test
    fun `BooleanArray shouldBeEmpty fails for non-empty array`() {
        assertFailsWith<AssertionFailedError> {
            booleanArrayOf(true).shouldBeEmpty()
        }
    }

    @Test
    fun `BooleanArray shouldNotBeEmpty passes for non-empty array`() {
        booleanArrayOf(true, false).shouldNotBeEmpty()
    }

    @Test
    fun `BooleanArray shouldNotBeEmpty fails for empty array`() {
        assertFailsWith<AssertionFailedError> {
            booleanArrayOf().shouldNotBeEmpty()
        }
    }

    // ── shouldContentEqual: IntArray ──────────────────────────────────────────

    @Test
    fun `IntArray shouldContentEqual passes for equal arrays`() {
        intArrayOf(1, 2, 3) shouldContentEqual intArrayOf(1, 2, 3)
    }

    @Test
    fun `IntArray shouldContentEqual passes when both null`() {
        val a: IntArray? = null
        val b: IntArray? = null
        a shouldContentEqual b
    }

    @Test
    fun `IntArray shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: IntArray? = null
            a shouldContentEqual intArrayOf(1, 2, 3)
        }
    }

    @Test
    fun `IntArray shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            intArrayOf(1, 2, 3) shouldContentEqual intArrayOf(3, 2, 1)
        }
    }

    // ── shouldContentEqual: LongArray ─────────────────────────────────────────

    @Test
    fun `LongArray shouldContentEqual passes for equal arrays`() {
        longArrayOf(1L, 2L, 3L) shouldContentEqual longArrayOf(1L, 2L, 3L)
    }

    @Test
    fun `LongArray shouldContentEqual passes when both null`() {
        val a: LongArray? = null
        val b: LongArray? = null
        a shouldContentEqual b
    }

    @Test
    fun `LongArray shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: LongArray? = null
            a shouldContentEqual longArrayOf(1L)
        }
    }

    @Test
    fun `LongArray shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            longArrayOf(1L, 2L) shouldContentEqual longArrayOf(2L, 1L)
        }
    }

    // ── shouldContentEqual: DoubleArray (NaN / -0.0) ──────────────────────────

    @Test
    fun `DoubleArray shouldContentEqual passes for equal arrays`() {
        doubleArrayOf(1.0, 2.0, 3.0) shouldContentEqual doubleArrayOf(1.0, 2.0, 3.0)
    }

    @Test
    fun `DoubleArray shouldContentEqual passes when both null`() {
        val a: DoubleArray? = null
        val b: DoubleArray? = null
        a shouldContentEqual b
    }

    @Test
    fun `DoubleArray shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: DoubleArray? = null
            a shouldContentEqual doubleArrayOf(1.0)
        }
    }

    @Test
    fun `DoubleArray shouldContentEqual NaN equals NaN passes`() {
        // NaN 비트 비교: NaN == NaN → true
        doubleArrayOf(Double.NaN, 1.0) shouldContentEqual doubleArrayOf(Double.NaN, 1.0)
    }

    @Test
    fun `DoubleArray shouldContentEqual minus zero not equal to zero fails`() {
        // -0.0 과 0.0 은 비트가 다르므로 실패해야 한다
        assertFailsWith<AssertionFailedError> {
            doubleArrayOf(-0.0) shouldContentEqual doubleArrayOf(0.0)
        }
    }

    @Test
    fun `DoubleArray shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            doubleArrayOf(1.0, 2.0) shouldContentEqual doubleArrayOf(2.0, 1.0)
        }
    }

    // ── shouldContentEqual: FloatArray (NaN / -0.0) ───────────────────────────

    @Test
    fun `FloatArray shouldContentEqual passes for equal arrays`() {
        floatArrayOf(1.0f, 2.0f) shouldContentEqual floatArrayOf(1.0f, 2.0f)
    }

    @Test
    fun `FloatArray shouldContentEqual passes when both null`() {
        val a: FloatArray? = null
        val b: FloatArray? = null
        a shouldContentEqual b
    }

    @Test
    fun `FloatArray shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: FloatArray? = null
            a shouldContentEqual floatArrayOf(1.0f)
        }
    }

    @Test
    fun `FloatArray shouldContentEqual NaN equals NaN passes`() {
        floatArrayOf(Float.NaN, 1.0f) shouldContentEqual floatArrayOf(Float.NaN, 1.0f)
    }

    @Test
    fun `FloatArray shouldContentEqual minus zero not equal to zero fails`() {
        assertFailsWith<AssertionFailedError> {
            floatArrayOf(-0.0f) shouldContentEqual floatArrayOf(0.0f)
        }
    }

    @Test
    fun `FloatArray shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            floatArrayOf(1.0f, 2.0f) shouldContentEqual floatArrayOf(2.0f, 1.0f)
        }
    }

    // ── shouldContentEqual: ByteArray ─────────────────────────────────────────

    @Test
    fun `ByteArray shouldContentEqual passes for equal arrays`() {
        byteArrayOf(1, 2, 3) shouldContentEqual byteArrayOf(1, 2, 3)
    }

    @Test
    fun `ByteArray shouldContentEqual passes when both null`() {
        val a: ByteArray? = null
        val b: ByteArray? = null
        a shouldContentEqual b
    }

    @Test
    fun `ByteArray shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: ByteArray? = null
            a shouldContentEqual byteArrayOf(1)
        }
    }

    @Test
    fun `ByteArray shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            byteArrayOf(1, 2) shouldContentEqual byteArrayOf(2, 1)
        }
    }

    // ── shouldContentEqual: CharArray ─────────────────────────────────────────

    @Test
    fun `CharArray shouldContentEqual passes for equal arrays`() {
        charArrayOf('a', 'b', 'c') shouldContentEqual charArrayOf('a', 'b', 'c')
    }

    @Test
    fun `CharArray shouldContentEqual passes when both null`() {
        val a: CharArray? = null
        val b: CharArray? = null
        a shouldContentEqual b
    }

    @Test
    fun `CharArray shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: CharArray? = null
            a shouldContentEqual charArrayOf('a')
        }
    }

    @Test
    fun `CharArray shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            charArrayOf('a', 'b') shouldContentEqual charArrayOf('b', 'a')
        }
    }

    // ── shouldContentEqual: BooleanArray ──────────────────────────────────────

    @Test
    fun `BooleanArray shouldContentEqual passes for equal arrays`() {
        booleanArrayOf(true, false, true) shouldContentEqual booleanArrayOf(true, false, true)
    }

    @Test
    fun `BooleanArray shouldContentEqual passes when both null`() {
        val a: BooleanArray? = null
        val b: BooleanArray? = null
        a shouldContentEqual b
    }

    @Test
    fun `BooleanArray shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: BooleanArray? = null
            a shouldContentEqual booleanArrayOf(true)
        }
    }

    @Test
    fun `BooleanArray shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            booleanArrayOf(true, false) shouldContentEqual booleanArrayOf(false, true)
        }
    }

    // ── shouldContentEqual: Array<T> ──────────────────────────────────────────

    @Test
    fun `Array shouldContentEqual passes for equal arrays`() {
        arrayOf("a", "b", "c") shouldContentEqual arrayOf("a", "b", "c")
    }

    @Test
    fun `Array shouldContentEqual passes when both null`() {
        val a: Array<String>? = null
        val b: Array<String>? = null
        a shouldContentEqual b
    }

    @Test
    fun `Array shouldContentEqual fails when one is null`() {
        assertFailsWith<AssertionFailedError> {
            val a: Array<String>? = null
            a shouldContentEqual arrayOf("x")
        }
    }

    @Test
    fun `Array shouldContentEqual fails for different content`() {
        assertFailsWith<AssertionFailedError> {
            arrayOf("a", "b") shouldContentEqual arrayOf("b", "a")
        }
    }

    @Test
    fun `Array shouldContentEqual fails for different sizes`() {
        assertFailsWith<AssertionFailedError> {
            arrayOf("a", "b") shouldContentEqual arrayOf("a")
        }
    }

    // ── chaining ──────────────────────────────────────────────────────────────

    @Test
    fun `IntArray assertions support chaining`() {
        intArrayOf(1, 2, 3)
            .shouldHaveSize(3)
            .shouldContain(1)
            .shouldNotContain(99)
    }
}
