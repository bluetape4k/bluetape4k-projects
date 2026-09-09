package io.bluetape4k.math.commons

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class NormalizedRootMeanSquareTest {

    @Test
    fun `normalized rmse calculates a finite result for regular values`() {
        val result = sequenceOf(0.0, 1.0).normalizedRmse(sequenceOf(0.0, 2.0))

        result shouldBeEqualTo sqrt(0.5) / 2.0
    }

    @Test
    fun `normalized rmse consumes one shot sequences once`() {
        val expected = sequenceOf(0.0, 1.0, 2.0).constrainOnce()
        val actual = sequenceOf(0.0, 2.0, 4.0).constrainOnce()

        expected.normalizedRmse(actual) shouldBeEqualTo sqrt(5.0 / 3.0) / 4.0
    }

    @Test
    fun `empty matching sequences return zero`() {
        emptySequence<Double>().normalizedRmse(emptySequence()) shouldBeEqualTo 0.0
    }

    @Test
    fun `length mismatch is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            sequenceOf(1.0).normalizedRmse(sequenceOf(1.0, 2.0))
        }

        failure.message shouldBeEqualTo "두 컬렉션의 항목 수가 같아야 합니다."
    }

    @Test
    fun `all NaN actual values are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            sequenceOf(1.0, 2.0).normalizedRmse(sequenceOf(Double.NaN, Double.NaN))
        }

        failure.message shouldBeEqualTo NAN_INPUT_MESSAGE
    }

    @Test
    fun `all NaN expected values are rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            sequenceOf(Double.NaN, Double.NaN).normalizedRmse(sequenceOf(1.0, 2.0))
        }

        failure.message shouldBeEqualTo NAN_INPUT_MESSAGE
    }

    @Test
    fun `partial NaN values propagate NaN without filtering the error`() {
        val actual = sequenceOf(1.0, Double.NaN, 3.0)
        val expected = sequenceOf(1.0, 2.0, 3.0)

        actual.normalizedRmse(expected).isNaN().shouldBeTrue()
        expected.normalizedRmse(actual).isNaN().shouldBeTrue()
    }

    @Test
    fun `nonzero error with a constant actual range returns positive infinity`() {
        val result = sequenceOf(4.0, 4.0).normalizedRmse(sequenceOf(5.0, 5.0))

        result.isInfinite().shouldBeTrue()
        (result > 0.0).shouldBeTrue()
    }

    companion object {
        private const val NAN_INPUT_MESSAGE =
            "normalizedRmse 입력에는 NaN이 아닌 expected와 actual 값이 각각 하나 이상 있어야 합니다."
    }
}
