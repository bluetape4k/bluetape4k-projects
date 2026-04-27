package io.bluetape4k.math.linear

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.amshove.kluent.shouldBeTrue
import org.apache.commons.math3.fraction.BigFraction
import org.apache.commons.math3.fraction.BigFractionField
import org.apache.commons.math3.fraction.Fraction
import org.apache.commons.math3.fraction.FractionField
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

    @Test
    fun `checkRowIndex는 유효 범위를 벗어난 인덱스에서 예외를 던진다`() {
        val m = realMatrixOf(3, 3)
        assertThrows<Exception> { m.checkRowIndex(99) }
        assertThrows<Exception> { m.checkRowIndex(-1) }
    }

    @Test
    fun `checkColumnIndex는 유효 범위를 벗어난 인덱스에서 예외를 던진다`() {
        val m = realMatrixOf(3, 3)
        assertThrows<Exception> { m.checkColumnIndex(99) }
    }

    @Test
    fun `checkMatrixIndex는 유효 범위를 벗어난 인덱스에서 예외를 던진다`() {
        val m = realMatrixOf(3, 3)
        assertThrows<Exception> { m.checkMatrixIndex(99, 0) }
        assertThrows<Exception> { m.checkMatrixIndex(0, 99) }
    }

    @Test
    fun `checkAdditionCompatible은 크기가 다른 행렬에서 예외를 던진다`() {
        val m1 = realMatrixOf(2, 3)
        val m2 = realMatrixOf(3, 2)
        assertThrows<Exception> { m1.checkAdditionCompatible(m2) }
    }

    @Test
    fun `checkMultiplicationCompatible은 열-행 크기가 맞지 않으면 예외를 던진다`() {
        val m1 = realMatrixOf(2, 3)
        val m2 = realMatrixOf(2, 2)
        assertThrows<Exception> { m1.checkMultiplicationCompatible(m2) }
    }

    @Test
    fun `solveLowerTriangularSystem은 하삼각 시스템을 푼다`() {
        val L = Array2DRowRealMatrix(
            arrayOf(doubleArrayOf(1.0, 0.0), doubleArrayOf(2.0, 1.0))
        )
        val b = realVectorOf(doubleArrayOf(1.0, 4.0))
        L.solveLowerTriangularSystem(b)
        // L·[1,2]=[1,4] : x0=1, x1=4-2*1=2
        b.getEntry(0).shouldBeNear(1.0, 1e-10)
        b.getEntry(1).shouldBeNear(2.0, 1e-10)
    }

    @Test
    fun `solveUpperTriangularSystem은 상삼각 시스템을 푼다`() {
        val U = Array2DRowRealMatrix(
            arrayOf(doubleArrayOf(2.0, 1.0), doubleArrayOf(0.0, 1.0))
        )
        val b = realVectorOf(doubleArrayOf(5.0, 3.0))
        U.solveUpperTriangularSystem(b)
        // x1=3, x0=(5-1*3)/2=1
        b.getEntry(0).shouldBeNear(1.0, 1e-10)
        b.getEntry(1).shouldBeNear(3.0, 1e-10)
    }

    @Test
    fun `blockInverse는 단위행렬의 역행렬로 단위행렬을 반환한다`() {
        val I4 = realIdentityMatrixOf(4)
        val inv = I4.blockInverse(2)
        inv.getEntry(0, 0).shouldBeNear(1.0, 1e-10)
        inv.getEntry(1, 1).shouldBeNear(1.0, 1e-10)
        inv.getEntry(2, 2).shouldBeNear(1.0, 1e-10)
        inv.getEntry(3, 3).shouldBeNear(1.0, 1e-10)
        inv.getEntry(0, 1).shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `fieldVectorOf로 Fraction 벡터를 생성할 수 있다`() {
        val fv = fieldVectorOf(arrayOf(Fraction(1), Fraction(2), Fraction(3)))
        fv.dimension.shouldBeEqualTo(3)
        fv.getEntry(0).shouldBeEqualTo(Fraction(1))
        fv.getEntry(2).shouldBeEqualTo(Fraction(3))
    }

    @Test
    fun `rowFieldMatrixOf로 Fraction 행 행렬을 생성할 수 있다`() {
        val m = rowFieldMatrixOf(arrayOf(Fraction(1), Fraction(2), Fraction(3)))
        m.rowDimension.shouldBeEqualTo(1)
        m.columnDimension.shouldBeEqualTo(3)
    }

    @Test
    fun `columnFieldMatrixOf로 Fraction 열 행렬을 생성할 수 있다`() {
        val m = columnFieldMatrixOf(arrayOf(Fraction(4), Fraction(5)))
        m.rowDimension.shouldBeEqualTo(2)
        m.columnDimension.shouldBeEqualTo(1)
    }

    @Test
    fun `FieldMatrix Fraction을 RealMatrix로 변환할 수 있다`() {
        val field = FractionField.getInstance()
        val fm = field.createFieldMatrix(2, 2)
        fm.setEntry(0, 0, Fraction(1))
        fm.setEntry(0, 1, Fraction(2))
        fm.setEntry(1, 0, Fraction(3))
        fm.setEntry(1, 1, Fraction(4))
        val rm = fm.toRealMatrix()
        rm.getEntry(0, 0).shouldBeNear(1.0, 1e-10)
        rm.getEntry(1, 1).shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `FieldMatrix BigFraction을 RealMatrix로 변환할 수 있다`() {
        val field = BigFractionField.getInstance()
        val fm = field.createFieldMatrix(2, 2)
        fm.setEntry(0, 0, BigFraction(5))
        fm.setEntry(1, 1, BigFraction(6))
        val rm = fm.toRealMatrix()
        rm.getEntry(0, 0).shouldBeNear(5.0, 1e-10)
        rm.getEntry(1, 1).shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `RealVector를 바이트 배열로 직렬화 후 복원할 수 있다`() {
        val original = realVectorOf(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0))
        val bytes = original.toByteArray()
        val restored = bytes.toRealVector()

        restored.dimension.shouldBeEqualTo(original.dimension)
        for (i in 0 until original.dimension) {
            restored.getEntry(i).shouldBeNear(original.getEntry(i), 1e-10)
        }
    }

    @Test
    fun `RealMatrix를 바이트 배열로 직렬화 후 복원할 수 있다`() {
        val original = Array2DRowRealMatrix(
            arrayOf(
                doubleArrayOf(1.0, 2.0, 3.0),
                doubleArrayOf(4.0, 5.0, 6.0),
                doubleArrayOf(7.0, 8.0, 9.0)
            )
        )
        val bytes = original.toByteArray()
        val restored = bytes.toRealMatrix()

        restored.rowDimension.shouldBeEqualTo(original.rowDimension)
        restored.columnDimension.shouldBeEqualTo(original.columnDimension)
        for (i in 0 until original.rowDimension) {
            for (j in 0 until original.columnDimension) {
                restored.getEntry(i, j).shouldBeNear(original.getEntry(i, j), 1e-10)
            }
        }
    }

    @Test
    fun `빈 RealVector도 직렬화 후 복원할 수 있다`() {
        val original = realVectorOf(DoubleArray(0))
        val bytes = original.toByteArray()
        val restored = bytes.toRealVector()
        restored.dimension.shouldBeEqualTo(0)
    }

    @Test
    fun `1x1 RealMatrix를 직렬화 후 복원할 수 있다`() {
        val original = Array2DRowRealMatrix(arrayOf(doubleArrayOf(42.0)))
        val bytes = original.toByteArray()
        val restored = bytes.toRealMatrix()
        restored.rowDimension.shouldBeEqualTo(1)
        restored.columnDimension.shouldBeEqualTo(1)
        restored.getEntry(0, 0).shouldBeNear(42.0, 1e-10)
    }
}
