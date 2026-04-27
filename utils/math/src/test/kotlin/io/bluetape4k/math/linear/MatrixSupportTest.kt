package io.bluetape4k.math.linear

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.amshove.kluent.shouldBeTrue
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MatrixSupportTest {

    companion object : KLogging()

    @Test
    fun `diagonalMatrixOf로 대각행렬을 생성할 수 있다`() {
        val m = diagonalMatrixOf(3)
        m.rowDimension.shouldBeEqualTo(3)
        m.columnDimension.shouldBeEqualTo(3)
    }

    @Test
    fun `원소 배열로 대각행렬을 생성할 수 있다`() {
        val m = diagonalMatrixOf(doubleArrayOf(1.0, 2.0, 3.0))
        m.getEntry(0, 0).shouldBeNear(1.0, 1e-10)
        m.getEntry(1, 1).shouldBeNear(2.0, 1e-10)
        m.getEntry(2, 2).shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `2차원 배열로 RealMatrix를 생성할 수 있다`() {
        val m = arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0)
        ).createRealMatrix()
        m.getEntry(0, 0).shouldBeNear(1.0, 1e-10)
        m.getEntry(1, 1).shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `realMatrixOf로 RealMatrix를 생성할 수 있다`() {
        val m = realMatrixOf(2, 3)
        m.rowDimension.shouldBeEqualTo(2)
        m.columnDimension.shouldBeEqualTo(3)
    }

    @Test
    fun `realIdentityMatrixOf로 단위행렬을 생성할 수 있다`() {
        val I = realIdentityMatrixOf(3)
        I.getEntry(0, 0).shouldBeNear(1.0, 1e-10)
        I.getEntry(1, 1).shouldBeNear(1.0, 1e-10)
        I.getEntry(2, 2).shouldBeNear(1.0, 1e-10)
        I.getEntry(0, 1).shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `realDiagonalMatrixOf로 실수 대각행렬을 생성할 수 있다`() {
        val m = realDiagonalMatrixOf(doubleArrayOf(5.0, 6.0, 7.0))
        m.getEntry(0, 0).shouldBeNear(5.0, 1e-10)
        m.getEntry(1, 1).shouldBeNear(6.0, 1e-10)
        m.getEntry(2, 2).shouldBeNear(7.0, 1e-10)
    }

    @Test
    fun `realVectorOf로 실수 벡터를 생성할 수 있다`() {
        val v = realVectorOf(doubleArrayOf(1.0, 2.0, 3.0))
        v.getDimension().shouldBeEqualTo(3)
        v.getEntry(0).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `rowRealMatrixOf로 행 행렬을 생성할 수 있다`() {
        val m = rowRealMatrixOf(doubleArrayOf(1.0, 2.0, 3.0))
        m.rowDimension.shouldBeEqualTo(1)
        m.columnDimension.shouldBeEqualTo(3)
    }

    @Test
    fun `columnRealMatrixOf로 열 행렬을 생성할 수 있다`() {
        val m = columnRealMatrixOf(doubleArrayOf(1.0, 2.0, 3.0))
        m.rowDimension.shouldBeEqualTo(3)
        m.columnDimension.shouldBeEqualTo(1)
    }

    @Test
    fun `단위행렬은 대칭이다`() {
        val I = realIdentityMatrixOf(3)
        I.isSymmetric().shouldBeTrue()
    }

    @Test
    fun `단위행렬의 역행렬은 자기 자신이다`() {
        val I = realIdentityMatrixOf(3)
        val inv = I.inverse()
        inv.getEntry(0, 0).shouldBeNear(1.0, 1e-10)
        inv.getEntry(1, 1).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `2x2 행렬의 역행렬을 계산할 수 있다`() {
        val m = Array2DRowRealMatrix(
            arrayOf(doubleArrayOf(2.0, 0.0), doubleArrayOf(0.0, 2.0))
        )
        val inv = m.inverse()
        inv.getEntry(0, 0).shouldBeNear(0.5, 1e-10)
        inv.getEntry(1, 1).shouldBeNear(0.5, 1e-10)
    }

    @Test
    fun `checkMatrixIndex는 유효한 인덱스에서 예외를 던지지 않는다`() {
        val m = realMatrixOf(3, 3)
        m.checkMatrixIndex(0, 0)
        m.checkMatrixIndex(2, 2)
    }

    @Test
    fun `checkRowIndex는 유효한 행 인덱스에서 예외를 던지지 않는다`() {
        val m = realMatrixOf(3, 3)
        m.checkRowIndex(0)
        m.checkRowIndex(2)
    }

    @Test
    fun `checkColumnIndex는 유효한 열 인덱스에서 예외를 던지지 않는다`() {
        val m = realMatrixOf(3, 3)
        m.checkColumnIndex(0)
        m.checkColumnIndex(2)
    }
}
