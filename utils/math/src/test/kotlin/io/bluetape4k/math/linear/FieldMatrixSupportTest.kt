package io.bluetape4k.math.linear

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.apache.commons.math3.fraction.Fraction
import org.apache.commons.math3.fraction.FractionField
import org.apache.commons.math3.linear.Array2DRowFieldMatrix
import org.junit.jupiter.api.Test

class FieldMatrixSupportTest {

    companion object : KLogging()

    private fun fractionMatrix(vararg rows: Array<Fraction>) =
        Array2DRowFieldMatrix(rows)

    private fun f(n: Int, d: Int = 1) = Fraction(n, d)

    @Test
    fun `인덱스로 필드 행렬의 원소를 가져올 수 있다`() {
        val m = fractionMatrix(
            arrayOf(f(1), f(2)),
            arrayOf(f(3), f(4))
        )
        m[0, 0].shouldBeEqualTo(f(1))
        m[1, 1].shouldBeEqualTo(f(4))
    }

    @Test
    fun `인덱스로 필드 행렬의 원소를 설정할 수 있다`() {
        val m = fractionMatrix(
            arrayOf(f(1), f(2)),
            arrayOf(f(3), f(4))
        )
        m[0, 0] = f(10)
        m[0, 0].shouldBeEqualTo(f(10))
    }

    @Test
    fun `두 필드 행렬을 더할 수 있다`() {
        val m1 = fractionMatrix(
            arrayOf(f(1), f(2)),
            arrayOf(f(3), f(4))
        )
        val m2 = fractionMatrix(
            arrayOf(f(5), f(6)),
            arrayOf(f(7), f(8))
        )
        val result = m1 + m2
        result[0, 0].shouldBeEqualTo(f(6))
        result[1, 1].shouldBeEqualTo(f(12))
    }

    @Test
    fun `필드 행렬에 스칼라를 더할 수 있다`() {
        val m = fractionMatrix(
            arrayOf(f(1), f(2)),
            arrayOf(f(3), f(4))
        )
        val result = m + f(10)
        result[0, 0].shouldBeEqualTo(f(11))
        result[1, 1].shouldBeEqualTo(f(14))
    }

    @Test
    fun `두 필드 행렬을 뺄 수 있다`() {
        val m1 = fractionMatrix(
            arrayOf(f(5), f(6)),
            arrayOf(f(7), f(8))
        )
        val m2 = fractionMatrix(
            arrayOf(f(1), f(2)),
            arrayOf(f(3), f(4))
        )
        val result = m1 - m2
        result[0, 0].shouldBeEqualTo(f(4))
        result[1, 1].shouldBeEqualTo(f(4))
    }

    @Test
    fun `필드 행렬에서 스칼라를 뺄 수 있다`() {
        val m = fractionMatrix(
            arrayOf(f(5), f(6)),
            arrayOf(f(7), f(8))
        )
        val result = m - f(1)
        result[0, 0].shouldBeEqualTo(f(4))
        result[1, 1].shouldBeEqualTo(f(7))
    }

    @Test
    fun `두 필드 행렬을 곱할 수 있다`() {
        val m1 = fractionMatrix(
            arrayOf(f(1), f(0)),
            arrayOf(f(0), f(2))
        )
        val m2 = fractionMatrix(
            arrayOf(f(3), f(0)),
            arrayOf(f(0), f(4))
        )
        val result = m1 * m2
        result[0, 0].shouldBeEqualTo(f(3))
        result[1, 1].shouldBeEqualTo(f(8))
    }

    @Test
    fun `필드 행렬에 스칼라를 곱할 수 있다`() {
        val m = fractionMatrix(
            arrayOf(f(1), f(2)),
            arrayOf(f(3), f(4))
        )
        val result = m * f(2)
        result[0, 0].shouldBeEqualTo(f(2))
        result[1, 1].shouldBeEqualTo(f(8))
    }

    @Test
    fun `필드 행렬을 스칼라로 나눌 수 있다`() {
        val m = fractionMatrix(
            arrayOf(f(2), f(4)),
            arrayOf(f(6), f(8))
        )
        val result = m / f(2)
        result[0, 0].shouldBeEqualTo(f(1))
        result[1, 1].shouldBeEqualTo(f(4))
    }

    @Test
    fun `createFieldMatrix로 FieldMatrix를 생성할 수 있다`() {
        val field = FractionField.getInstance()
        val m = field.createFieldMatrix(2, 3)
        m.rowDimension.shouldBeEqualTo(2)
        m.columnDimension.shouldBeEqualTo(3)
    }
}
