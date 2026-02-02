package io.bluetape4k.math.commons

import io.bluetape4k.collections.eclipse.toFastList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

class ClampTest {

    private val values = listOf(0.001, 1.0001, 2.0002, 5.0005)

    @Test
    fun `clamp double values`() {
        values[0].clamp(0.0011, 1e-3) shouldBeEqualTo 0.0011
        values[0].clamp(0.0011) shouldBeEqualTo values[0]
    }

    @Test
    fun `clamp sequence value`() {
        values.clamp(1.0) shouldBeEqualTo values
        values.clamp(1.0, 1e-3) shouldContain 1.0
    }

    @Test
    fun `range clamp for comparable`() {
        1.0.rangeClamp(0.5..1.5) shouldBeEqualTo 1.0
        1.0.rangeClamp(-1.0..0.0) shouldBeEqualTo 0.0
        1.0.rangeClamp(2.0..5.0) shouldBeEqualTo 2.0
    }

    @Test
    fun `clamp for list`() {
        val values = listOf(-1.0, 0.0, 1.0, 2.0)
        values.clamp(0.01) shouldBeEqualTo values
        values.clamp(0.01, 1e-1) shouldBeEqualTo listOf(-1.0, 0.01, 1.0, 2.0)
    }

    @Test
    fun `range clamp for list`() {
        val values = listOf(-1.0, 0.0, 1.0, 2.0)
        val expected = listOf(0.0, 0.0, 1.0, 1.0)

        values.rangeClamp(0.0..1.0) shouldBeEqualTo expected
        values.asSequence().rangeClamp(0.0..1.0).toFastList() shouldBeEqualTo expected
    }
}
