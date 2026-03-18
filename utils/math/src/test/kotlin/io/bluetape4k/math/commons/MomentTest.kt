package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class MomentTest {

    companion object: KLogging()

    private val data = doubleArrayOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)

    @Test
    fun `DoubleArray moment 평균이 올바르다`() {
        val m = data.moment()
        // 평균 = (2+4+4+4+5+5+7+9)/8 = 5.0
        m.average.shouldBeNear(5.0, 1e-10)
    }

    @Test
    fun `DoubleArray moment 분산이 양수이다`() {
        val m = data.moment()
        assert(m.variance > 0.0) { "분산은 양수여야 합니다" }
    }

    @Test
    fun `DoubleArray moment 평균편차가 양수이다`() {
        val m = data.moment()
        assert(m.avgDev >= 0.0) { "평균편차는 0 이상이어야 합니다" }
    }

    @Test
    fun `DoubleArray moment 대칭 분포의 skew 는 0 에 가깝다`() {
        // 대칭 데이터
        val symmetric = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0)
        val m = symmetric.moment()
        m.skew.shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `Iterable moment 가 DoubleArray moment 와 같다`() {
        val dataList = data.toList()
        val m1 = data.moment()
        val m2 = dataList.moment()

        m1.average.shouldBeNear(m2.average, 1e-10)
        m1.variance.shouldBeNear(m2.variance, 1e-10)
        m1.avgDev.shouldBeNear(m2.avgDev, 1e-10)
    }

    @Test
    fun `Sequence moment 가 DoubleArray moment 와 같다`() {
        val m1 = data.moment()
        val m2 = data.asSequence().moment()

        m1.average.shouldBeNear(m2.average, 1e-10)
        m1.variance.shouldBeNear(m2.variance, 1e-10)
    }

    @Test
    fun `moment 는 Moment 데이터 클래스를 반환한다`() {
        val m = data.moment()
        m shouldBeInstanceOf Moment::class
    }
}
