package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DoubleArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `index of double`() {
        val array = doubleArrayOf(1, 2, 3, 4, 5)
        val target = 3.0

        array.indexOf(target, 0, array.size - 1) shouldBeEqualTo 2

        emptyDoubleArray.indexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size)
        }
    }

    @Test
    fun `index of double array`() {
        val array = doubleArrayOf(1, 2, 3, 4, 5)
        val target = doubleArrayOf(3, 4)

        array.indexOf(target, 0, array.size - 1) shouldBeEqualTo 2

        emptyDoubleArray.indexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size)
        }
    }

    @Test
    fun `lastIndex of double`() {
        val array = doubleArrayOf(1, 2, 3, 4, 3)
        val target = 3.0

        array.lastIndexOf(target, 0, array.size - 1) shouldBeEqualTo 4

        emptyDoubleArray.lastIndexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, 1, array.size)
        }
    }

    @Test
    fun `lastIndex of double array`() {
        val array = doubleArrayOf(1, 2, 3, 4, 3, 4, 2)
        val target = doubleArrayOf(3, 4)

        array.lastIndexOf(target, 0, array.size - 1) shouldBeEqualTo 4

        emptyDoubleArray.lastIndexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, 1, array.size)
        }
    }

    @Test
    fun `ensure capacity`() {
        val array = doubleArrayOf(1, 2, 3, 4, 5)

        array.ensureCapacity(array.size, 5) shouldBeEqualTo doubleArrayOf(1, 2, 3, 4, 5)
        array.ensureCapacity(10, 0) shouldBeEqualTo doubleArrayOf(
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
    fun `concat double arrays`() {
        val array1 = doubleArrayOf(1, 2, 3)
        val array2 = doubleArrayOf(4, 5)

        concat(array1, array2) shouldBeEqualTo doubleArrayOf(1, 2, 3, 4, 5)
        concat(array2, array1) shouldBeEqualTo doubleArrayOf(4, 5, 1, 2, 3)
    }

    @Test
    fun `reverse empty double array`() {
        val array = emptyDoubleArray

        array.reverseTo() shouldBeEqualTo emptyDoubleArray
        array.reverseTo(0, 0) shouldBeEqualTo emptyDoubleArray

        array.reverseThis()
        array shouldBeEqualTo emptyDoubleArray

        array.reverseThis(0, 0)
        array shouldBeEqualTo emptyDoubleArray
    }

    @Test
    fun `reverse double array`() {
        val array = doubleArrayOf(1, 2, 3, 4, 5)

        array.reverseTo(0, array.size - 1) shouldBeEqualTo doubleArrayOf(5, 4, 3, 2, 1)
        array.reverseTo(1, 3) shouldBeEqualTo doubleArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `reverse current double array`() {
        val array1 = doubleArrayOf(1, 2, 3, 4, 5)
        array1.reverseThis()
        array1 shouldBeEqualTo doubleArrayOf(5, 4, 3, 2, 1)

        val array2 = doubleArrayOf(1, 2, 3, 4, 5)
        array2.reverseThis(1, 3)
        array2 shouldBeEqualTo doubleArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `rotate empty double array`() {
        val array = emptyDoubleArray

        array.rotateTo(2) shouldBeEqualTo emptyDoubleArray
        array.rotateTo(-2) shouldBeEqualTo emptyDoubleArray

        array.rotateThis(2)
        array shouldBeEqualTo emptyDoubleArray

        array.rotateThis(-2)
        array shouldBeEqualTo emptyDoubleArray
    }

    @Test
    fun `rotate double array elements`() {
        val array = doubleArrayOf(1, 2, 3, 4, 5)
        array.rotateTo(2) shouldBeEqualTo doubleArrayOf(4, 5, 1, 2, 3)
        array.rotateTo(-2) shouldBeEqualTo doubleArrayOf(3, 4, 5, 1, 2)
    }

    @Test
    fun `rotate itself double array elements`() {
        val array1 = doubleArrayOf(1, 2, 3, 4, 5)
        array1.rotateThis(2)
        array1 shouldBeEqualTo doubleArrayOf(4, 5, 1, 2, 3)

        val array2 = doubleArrayOf(1, 2, 3, 4, 5)
        array2.rotateThis(-2)
        array2 shouldBeEqualTo doubleArrayOf(3, 4, 5, 1, 2)
    }
}
