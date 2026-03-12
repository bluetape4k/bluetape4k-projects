package io.bluetape4k.math

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class SimpleRegressionTest {

    companion object: KLogging()

    // y = 2x + 1 에 해당하는 데이터
    private val linearData = listOf(
        Pair(1.0, 3.0),
        Pair(2.0, 5.0),
        Pair(3.0, 7.0),
        Pair(4.0, 9.0),
        Pair(5.0, 11.0),
    )

    @Test
    fun `simpleRegression 기울기와 절편이 올바르다`() {
        val reg = linearData.simpleRegression()

        reg.slope.shouldBeNear(2.0, 1e-10)
        reg.intercept.shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `simpleRegression 데이터 개수가 올바르다`() {
        val reg = linearData.simpleRegression()
        reg.n shouldBeEqualTo 5L
    }

    @Test
    fun `simpleRegression predict 가 올바른 값을 반환한다`() {
        val reg = linearData.simpleRegression()

        reg.predict(6.0).shouldBeNear(13.0, 1e-10)
        reg.predict(0.0).shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `simpleRegression R 제곱이 완전 선형 데이터에서 1이다`() {
        val reg = linearData.simpleRegression()
        reg.rSquare.shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `Sequence simpleRegression 이 동작한다`() {
        val reg = linearData.asSequence().simpleRegression()

        reg.slope.shouldBeNear(2.0, 1e-10)
        reg.intercept.shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `selector 기반 simpleRegression 이 동작한다`() {
        data class Point(val x: Double, val y: Double)
        val points = listOf(
            Point(1.0, 3.0), Point(2.0, 5.0), Point(3.0, 7.0),
            Point(4.0, 9.0), Point(5.0, 11.0),
        )

        val reg = points.simpleRegression(xSelector = { it.x }, ySelector = { it.y })
        reg.slope.shouldBeNear(2.0, 1e-10)
        reg.intercept.shouldBeNear(1.0, 1e-10)
    }

    @Test
    fun `simpleRegression 통계량 속성들이 유효한 값을 가진다`() {
        val reg = linearData.simpleRegression()

        assert(!reg.r.isNaN()) { "r 은 NaN 이어선 안 됩니다" }
        assert(!reg.meanSquareError.isNaN()) { "meanSquareError 는 NaN 이어선 안 됩니다" }
        assert(!reg.slopeStdErr.isNaN()) { "slopeStdErr 는 NaN 이어선 안 됩니다" }
    }
}
