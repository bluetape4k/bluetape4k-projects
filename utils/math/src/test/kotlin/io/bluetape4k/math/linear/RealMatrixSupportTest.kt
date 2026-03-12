package io.bluetape4k.math.linear

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeNear
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.RealMatrix
import org.junit.jupiter.api.Test

class RealMatrixSupportTest {

    companion object: KLogging()

    private fun matrixOf(vararg rows: DoubleArray) = Array2DRowRealMatrix(rows)

    @Test
    fun `행렬 덧셈이 동작한다`() {
        val a = matrixOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        val b = matrixOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0))
        val result = a + b

        result.getEntry(0, 0).shouldBeNear(6.0, 1e-10)
        result.getEntry(0, 1).shouldBeNear(8.0, 1e-10)
        result.getEntry(1, 0).shouldBeNear(10.0, 1e-10)
        result.getEntry(1, 1).shouldBeNear(12.0, 1e-10)
    }

    @Test
    fun `스칼라 덧셈이 동작한다`() {
        val a = matrixOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        val result = a + 10.0

        result.getEntry(0, 0).shouldBeNear(11.0, 1e-10)
        result.getEntry(1, 1).shouldBeNear(14.0, 1e-10)
    }

    @Test
    fun `행렬 뺄셈이 동작한다`() {
        val a = matrixOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0))
        val b = matrixOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        val result = a - b

        result.getEntry(0, 0).shouldBeNear(4.0, 1e-10)
        result.getEntry(0, 1).shouldBeNear(4.0, 1e-10)
        result.getEntry(1, 0).shouldBeNear(4.0, 1e-10)
        result.getEntry(1, 1).shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `스칼라 뺄셈이 동작한다`() {
        val a = matrixOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0))
        val result = a - 2.0

        result.getEntry(0, 0).shouldBeNear(3.0, 1e-10)
        result.getEntry(1, 1).shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `행렬 곱셈이 동작한다`() {
        // [1 2] * [5 6] = [19 22]
        // [3 4]   [7 8]   [43 50]
        val a = matrixOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        val b = matrixOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0))
        val result = a * b

        result.getEntry(0, 0).shouldBeNear(19.0, 1e-10)
        result.getEntry(0, 1).shouldBeNear(22.0, 1e-10)
        result.getEntry(1, 0).shouldBeNear(43.0, 1e-10)
        result.getEntry(1, 1).shouldBeNear(50.0, 1e-10)
    }

    @Test
    fun `스칼라 곱셈이 동작한다`() {
        val a = matrixOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
        // times(scalar: N): AnyMatrix → RealMatrix 로 캐스트
        val result = (a * 2.0) as RealMatrix

        result.getEntry(0, 0).shouldBeNear(2.0, 1e-10)
        result.getEntry(1, 1).shouldBeNear(8.0, 1e-10)
    }

    @Test
    fun `스칼라 나눗셈이 동작한다`() {
        val a = matrixOf(doubleArrayOf(2.0, 4.0), doubleArrayOf(6.0, 8.0))
        // div(scalar: N): AnyMatrix → RealMatrix 로 캐스트
        val result = (a / 2.0) as RealMatrix

        result.getEntry(0, 0).shouldBeNear(1.0, 1e-10)
        result.getEntry(0, 1).shouldBeNear(2.0, 1e-10)
        result.getEntry(1, 0).shouldBeNear(3.0, 1e-10)
        result.getEntry(1, 1).shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `행렬 나눗셈이 역행렬 곱셈과 같다`() {
        // A / B = A * B^-1
        val a = matrixOf(doubleArrayOf(1.0, 0.0), doubleArrayOf(0.0, 1.0)) // 단위행렬
        val b = matrixOf(doubleArrayOf(2.0, 0.0), doubleArrayOf(0.0, 2.0))
        // div(rm: RealMatrix): RealMatrix
        val result: RealMatrix = a.div(b)

        // I / 2I = 0.5*I
        result.getEntry(0, 0).shouldBeNear(0.5, 1e-10)
        result.getEntry(1, 1).shouldBeNear(0.5, 1e-10)
    }
}
