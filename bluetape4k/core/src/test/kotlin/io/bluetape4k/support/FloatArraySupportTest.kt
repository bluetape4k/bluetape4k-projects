package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FloatArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `index of float`() {
        val array = floatArrayOf(1, 2, 3, 4, 5)
        val target = 3f

        array.indexOf(target, 0, array.size - 1) shouldBeEqualTo 2

        emptyFloatArray.indexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size)
        }
    }

    @Test
    fun `index of float array`() {
        val array = floatArrayOf(1, 2, 3, 4, 5)
        val target = floatArrayOf(3, 4)

        array.indexOf(target, 0, array.size - 1) shouldBeEqualTo 2

        emptyFloatArray.indexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size)
        }
    }

    @Test
    fun `lastIndex of float`() {
        val array = floatArrayOf(1, 2, 3, 4, 3)
        val target = 3f

        array.lastIndexOf(target, 0, array.size - 1) shouldBeEqualTo 4

        emptyFloatArray.lastIndexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, 1, array.size)
        }
    }

    @Test
    fun `lastIndex of float array`() {
        val array = floatArrayOf(1, 2, 3, 4, 3, 4, 2)
        val target = floatArrayOf(3, 4)

        array.lastIndexOf(target, 0, array.size - 1) shouldBeEqualTo 4

        emptyFloatArray.lastIndexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, 1, array.size)
        }
    }

    @Test
    fun `ensure capacity`() {
        val array = floatArrayOf(1, 2, 3, 4, 5)

        array.ensureCapacity(array.size, 5) shouldBeEqualTo floatArrayOf(1, 2, 3, 4, 5)
        array.ensureCapacity(10, 0) shouldBeEqualTo floatArrayOf(
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
    fun `concat float arrays`() {
        val array1 = floatArrayOf(1, 2, 3)
        val array2 = floatArrayOf(4, 5)

        concat(array1, array2) shouldBeEqualTo floatArrayOf(1, 2, 3, 4, 5)
        concat(array2, array1) shouldBeEqualTo floatArrayOf(4, 5, 1, 2, 3)
    }

    @Test
    fun `reverse empty float array`() {
        val array = emptyFloatArray

        array.reverseTo() shouldBeEqualTo emptyFloatArray
        array.reverseTo(0, 0) shouldBeEqualTo emptyFloatArray

        array.reverseThis()
        array shouldBeEqualTo emptyFloatArray

        array.reverseThis(0, 0)
        array shouldBeEqualTo emptyFloatArray
    }

    @Test
    fun `reverse float array`() {
        val array = floatArrayOf(1, 2, 3, 4, 5)

        array.reverseTo(0, array.size - 1) shouldBeEqualTo floatArrayOf(5, 4, 3, 2, 1)
        array.reverseTo(1, 3) shouldBeEqualTo floatArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `reverse current float array`() {
        val array1 = floatArrayOf(1, 2, 3, 4, 5)
        array1.reverseThis()
        array1 shouldBeEqualTo floatArrayOf(5, 4, 3, 2, 1)

        val array2 = floatArrayOf(1, 2, 3, 4, 5)
        array2.reverseThis(1, 3)
        array2 shouldBeEqualTo floatArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `rotate empty float array`() {
        val array = emptyFloatArray

        array.rotateTo(2) shouldBeEqualTo emptyFloatArray
        array.rotateTo(-2) shouldBeEqualTo emptyFloatArray

        array.rotateThis(2)
        array shouldBeEqualTo emptyFloatArray

        array.rotateThis(-2)
        array shouldBeEqualTo emptyFloatArray
    }

    @Test
    fun `rotate float array elements`() {
        val array = floatArrayOf(1, 2, 3, 4, 5)
        array.rotateTo(2) shouldBeEqualTo floatArrayOf(4, 5, 1, 2, 3)
        array.rotateTo(-2) shouldBeEqualTo floatArrayOf(3, 4, 5, 1, 2)
    }

    @Test
    fun `rotate itself float array elements`() {
        val array1 = floatArrayOf(1, 2, 3, 4, 5)
        array1.rotateThis(2)
        array1 shouldBeEqualTo floatArrayOf(4, 5, 1, 2, 3)

        val array2 = floatArrayOf(1, 2, 3, 4, 5)
        array2.rotateThis(-2)
        array2 shouldBeEqualTo floatArrayOf(3, 4, 5, 1, 2)
    }
}
