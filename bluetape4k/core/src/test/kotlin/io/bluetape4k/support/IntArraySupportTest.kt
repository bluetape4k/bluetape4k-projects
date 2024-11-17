package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class IntArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `index of int`() {
        val array = intArrayOf(1, 2, 3, 4, 5)
        val target = 3

        array.indexOf(target, 0) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size + 1)
        }
    }

    @Test
    fun `index of int array`() {
        val array = intArrayOf(1, 2, 3, 4, 5)
        val target = intArrayOf(3, 4)

        array.indexOf(target, 0, array.size - 1) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size + 1)
        }
    }

    @Test
    fun `ensure capacity`() {
        val array = intArrayOf(1, 2, 3, 4, 5)

        array.ensureCapacity(array.size, 5) shouldBeEqualTo intArrayOf(1, 2, 3, 4, 5)
        array.ensureCapacity(10, 0) shouldBeEqualTo intArrayOf(
            1, 2, 3, 4, 5,
            0, 0, 0, 0, 0
        )

        assertFailsWith<IllegalArgumentException> {
            array.ensureCapacity(-1, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            array.ensureCapacity(0, -1)
        }
    }

    @Test
    fun `concat int arrays`() {
        val array1 = intArrayOf(1, 2, 3)
        val array2 = intArrayOf(4, 5)

        concat(array1, array2) shouldBeEqualTo intArrayOf(1, 2, 3, 4, 5)
        concat(array2, array1) shouldBeEqualTo intArrayOf(4, 5, 1, 2, 3)
    }

    @Test
    fun `reverse int array`() {
        val array = intArrayOf(1, 2, 3, 4, 5)

        array.reverseTo(0, array.size - 1) shouldBeEqualTo intArrayOf(5, 4, 3, 2, 1)
        array.reverseTo(1, 4) shouldBeEqualTo intArrayOf(1, 5, 4, 3, 2)
    }

    @Test
    fun `reverse current int array`() {
        val array = intArrayOf(1, 2, 3, 4, 5)
        array.reverse()
        array shouldBeEqualTo intArrayOf(5, 4, 3, 2, 1)

        val array2 = intArrayOf(1, 2, 3, 4, 5)
        array2.reverse(1, 4)
        array2 shouldBeEqualTo intArrayOf(1, 5, 4, 3, 2)
    }

    @Test
    fun `rotate int array elements`() {
        val array = intArrayOf(1, 2, 3, 4, 5)
        array.rotateTo(2) shouldBeEqualTo intArrayOf(4, 5, 1, 2, 3)
        array.rotateTo(-2) shouldBeEqualTo intArrayOf(3, 4, 5, 1, 2)
    }

    @Test
    fun `rotate itself int array elements`() {
        val array = intArrayOf(1, 2, 3, 4, 5)
        array.rotate(2)
        array shouldBeEqualTo intArrayOf(4, 5, 1, 2, 3)

        val array2 = intArrayOf(1, 2, 3, 4, 5)
        array2.rotate(-2)
        array2 shouldBeEqualTo intArrayOf(3, 4, 5, 1, 2)
    }
}
