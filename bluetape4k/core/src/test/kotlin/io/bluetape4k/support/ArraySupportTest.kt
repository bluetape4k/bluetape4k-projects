package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

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
    fun `generate array`() {
        val array = IntArray(10) { it }
        array.indices.all { idx -> array[idx] == idx }.shouldBeTrue()
    }

    @Test
    fun `remove first element`() {
        val array = arrayOf("one", "two", "three")
        val array2 = array.removeFirst()
        array2 shouldContainSame arrayOf("two", "three")
    }

    @Test
    fun `remove last element`() {
        val array = arrayOf("one", "two", "three")
        val array2 = array.removeLastValue()
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
}
