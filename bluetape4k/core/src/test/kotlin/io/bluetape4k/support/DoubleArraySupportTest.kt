package io.bluetape4k.support

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.collections.reverse

class DoubleArraySupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `index of double`() {
        val doubles = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val target = 3.0

        doubles.indexOf(target, 0, doubles.size - 1) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            doubles.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            doubles.indexOf(target, 1, doubles.size + 1)
        }
    }

    @Test
    fun `index of double array`() {
        val doubles = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        val target = doubleArrayOf(3.0, 4.0)

        doubles.indexOf(target, 0, doubles.size - 1) shouldBeEqualTo 2

        assertFailsWith<IllegalArgumentException> {
            doubles.indexOf(target, -1, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            doubles.indexOf(target, 1, doubles.size + 1)
        }
    }

    @Test
    fun `ensure capacity`() {
        val doubles = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)

        doubles.ensureCapacity(doubles.size, 5) shouldBeEqualTo doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        doubles.ensureCapacity(10, 0) shouldBeEqualTo doubleArrayOf(
            1.0, 2.0, 3.0, 4.0, 5.0,
            0.0, 0.0, 0.0, 0.0, 0.0
        )

        assertFailsWith<IllegalArgumentException> {
            doubles.ensureCapacity(-1, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            doubles.ensureCapacity(0, -1)
        }
    }

    @Test
    fun `concat double arrays`() {
        val doubles1 = doubleArrayOf(1.0, 2.0, 3.0)
        val doubles2 = doubleArrayOf(4.0, 5.0)

        concat(doubles1, doubles2) shouldBeEqualTo doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        concat(doubles2, doubles1) shouldBeEqualTo doubleArrayOf(4.0, 5.0, 1.0, 2.0, 3.0)
    }

    @Test
    fun `reverse double array`() {
        val doubles = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)

        doubles.reverseTo(0, doubles.size - 1) shouldBeEqualTo doubleArrayOf(5.0, 4.0, 3.0, 2.0, 1.0)
        doubles.reverseTo(1, 4) shouldBeEqualTo doubleArrayOf(1.0, 5.0, 4.0, 3.0, 2.0)
    }

    @Test
    fun `reverse current double array`() {
        val doubles = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        doubles.reverse()
        doubles shouldBeEqualTo doubleArrayOf(5.0, 4.0, 3.0, 2.0, 1.0)

        val doubles2 = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        doubles2.reverse(1, 4)
        doubles2 shouldBeEqualTo doubleArrayOf(1.0, 4.0, 3.0, 2.0, 5.0)
    }

    @Test
    fun `rotate double array elements`() {
        val doubles = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        doubles.rotateTo(2) shouldBeEqualTo doubleArrayOf(4.0, 5.0, 1.0, 2.0, 3.0)
        doubles.rotateTo(-2) shouldBeEqualTo doubleArrayOf(3.0, 4.0, 5.0, 1.0, 2.0)
    }

    @Test
    fun `rotate itself double array elements`() {
        val doubles = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        doubles.rotate(2)
        doubles shouldBeEqualTo doubleArrayOf(4.0, 5.0, 1.0, 2.0, 3.0)

        val doubles2 = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
        doubles2.rotate(-2)
        doubles2 shouldBeEqualTo doubleArrayOf(3.0, 4.0, 5.0, 1.0, 2.0)
    }
}
