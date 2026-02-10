package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class LongArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `index of long`() {
        val array = longArrayOf(1, 2, 3, 4, 5)
        val target = 3L

        array.indexOf(target, 0) shouldBeEqualTo 2

        emptyLongArray.indexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size)
        }
    }

    @Test
    fun `index of long array`() {
        val array = longArrayOf(1, 2, 3, 4, 5)
        val target = longArrayOf(3, 4)

        array.indexOf(target, 0, array.size - 1) shouldBeEqualTo 2

        emptyLongArray.indexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.indexOf(target, 1, array.size)
        }
    }

    @Test
    fun `lastIndex of long`() {
        val array = longArrayOf(1, 2, 3, 4, 3)
        val target = 3L

        array.lastIndexOf(target, 0, array.size - 1) shouldBeEqualTo 4

        emptyLongArray.lastIndexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, 1, array.size)
        }
    }

    @Test
    fun `lastIndex of long array`() {
        val array = longArrayOf(1, 2, 3, 4, 3, 4, 2)
        val target = longArrayOf(3, 4)

        array.lastIndexOf(target, 0, array.size - 1) shouldBeEqualTo 4

        emptyLongArray.lastIndexOf(target) shouldBeEqualTo -1

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            array.lastIndexOf(target, 1, array.size)
        }
    }

    @Test
    fun `ensure capacity`() {
        val array = longArrayOf(1, 2, 3, 4, 5)

        array.ensureCapacity(array.size, 5) shouldBeEqualTo longArrayOf(1, 2, 3, 4, 5)
        array.ensureCapacity(10, 0) shouldBeEqualTo longArrayOf(
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
    fun `concat long arrays`() {
        val array1 = longArrayOf(1, 2, 3)
        val array2 = longArrayOf(4, 5)

        concat(array1, array2) shouldBeEqualTo longArrayOf(1, 2, 3, 4, 5)
        concat(array2, array1) shouldBeEqualTo longArrayOf(4, 5, 1, 2, 3)
    }

    @Test
    fun `reverse empty long array`() {
        val array = emptyLongArray

        array.reverseTo() shouldBeEqualTo emptyLongArray
        array.reverseTo(0, 0) shouldBeEqualTo emptyLongArray

        array.reverseThis()
        array shouldBeEqualTo emptyLongArray

        array.reverseThis(0, 0)
        array shouldBeEqualTo emptyLongArray
    }

    @Test
    fun `reverse long array`() {
        val array = longArrayOf(1, 2, 3, 4, 5)

        array.reverseTo(0, array.size - 1) shouldBeEqualTo longArrayOf(5, 4, 3, 2, 1)
        array.reverseTo(1, 3) shouldBeEqualTo longArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `reverse current long array`() {
        val array1 = longArrayOf(1, 2, 3, 4, 5)
        array1.reverseThis()
        array1 shouldBeEqualTo longArrayOf(5, 4, 3, 2, 1)

        val array2 = longArrayOf(1, 2, 3, 4, 5)
        array2.reverseThis(1, 3)
        array2 shouldBeEqualTo longArrayOf(1, 4, 3, 2, 5)
    }

    @Test
    fun `rotate empty long array`() {
        val array = emptyLongArray

        array.rotateTo(2) shouldBeEqualTo emptyLongArray
        array.rotateTo(-2) shouldBeEqualTo emptyLongArray

        array.rotateThis(2)
        array shouldBeEqualTo emptyLongArray

        array.rotateThis(-2)
        array shouldBeEqualTo emptyLongArray
    }

    @Test
    fun `rotate long array elements`() {
        val array = longArrayOf(1, 2, 3, 4, 5)
        array.rotateTo(2) shouldBeEqualTo longArrayOf(4, 5, 1, 2, 3)
        array.rotateTo(-2) shouldBeEqualTo longArrayOf(3, 4, 5, 1, 2)
    }

    @Test
    fun `rotate itself long array elements`() {
        val array = longArrayOf(1, 2, 3, 4, 5)
        array.rotateThis(2)
        array shouldBeEqualTo longArrayOf(4, 5, 1, 2, 3)

        val array2 = longArrayOf(1, 2, 3, 4, 5)
        array2.rotateThis(-2)
        array2 shouldBeEqualTo longArrayOf(3, 4, 5, 1, 2)
    }
}
