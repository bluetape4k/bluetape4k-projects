package io.bluetape4k.math.ml

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.ml.clustering.doublePointOf
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod.Canberra
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod.Chebyshev
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod.EarthMovers
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod.Euclidean
import io.bluetape4k.math.ml.distance.DistanceMeasureMethod.Manhattan
import org.junit.jupiter.api.Test

class DistanceMeasureMethodTest {

    companion object: KLogging() {
        private const val EPSILON = 1e-10
    }

    @Test
    fun `Euclidean 거리를 계산할 수 있다`() {
        val a = doubleArrayOf(0.0, 0.0)
        val b = doubleArrayOf(3.0, 4.0)
        val dist = Euclidean.compute(a, b)
        dist.shouldBeNear(5.0, EPSILON)
    }

    @Test
    fun `Manhattan 거리를 계산할 수 있다`() {
        val a = doubleArrayOf(0.0, 0.0)
        val b = doubleArrayOf(3.0, 4.0)
        val dist = Manhattan.compute(a, b)
        dist.shouldBeNear(7.0, EPSILON)
    }

    @Test
    fun `Chebyshev 거리를 계산할 수 있다`() {
        val a = doubleArrayOf(0.0, 0.0)
        val b = doubleArrayOf(3.0, 4.0)
        val dist = Chebyshev.compute(a, b)
        dist.shouldBeNear(4.0, EPSILON)
    }

    @Test
    fun `Canberra 거리를 계산할 수 있다`() {
        val a = doubleArrayOf(1.0, 2.0)
        val b = doubleArrayOf(3.0, 4.0)
        // |1-3|/(1+3) + |2-4|/(2+4) = 0.5 + 1/3 = 5/6
        val dist = Canberra.compute(a, b)
        dist.shouldBeNear(5.0 / 6.0, EPSILON)
    }

    @Test
    fun `EarthMovers 거리를 계산할 수 있다`() {
        val a = doubleArrayOf(1.0, 2.0, 3.0)
        val b = doubleArrayOf(4.0, 5.0, 6.0)
        val dist = EarthMovers.compute(a, b)
        dist shouldBeGreaterThan 0.0
    }

    @Test
    fun `DoublePoint를 사용하여 거리를 계산할 수 있다`() {
        val a = doublePointOf(0.0, 0.0)
        val b = doublePointOf(3.0, 4.0)
        val dist = Euclidean.compute(a, b)
        dist.shouldBeNear(5.0, EPSILON)
    }

    @Test
    fun `이름으로 DistanceMeasureMethod를 파싱할 수 있다`() {
        val method = DistanceMeasureMethod.parse("euclidean")
        method.shouldNotBeNull()
        method.shouldBeEqualTo(Euclidean)
    }

    @Test
    fun `대소문자 무시하여 파싱할 수 있다`() {
        val method = DistanceMeasureMethod.parse("MANHATTAN")
        method.shouldNotBeNull()
        method.shouldBeEqualTo(Manhattan)
    }

    @Test
    fun `존재하지 않는 이름은 null을 반환한다`() {
        val method = DistanceMeasureMethod.parse("unknown")
        method.shouldBeNull()
    }

    @Test
    fun `빈 문자열은 null을 반환한다`() {
        DistanceMeasureMethod.parse("").shouldBeNull()
    }

    @Test
    fun `공백 문자열은 null을 반환한다`() {
        DistanceMeasureMethod.parse("   ").shouldBeNull()
    }

    @Test
    fun `동일한 점 사이의 거리는 0이다`() {
        val a = doubleArrayOf(1.0, 2.0, 3.0)
        Euclidean.compute(a, a).shouldBeNear(0.0, EPSILON)
        Manhattan.compute(a, a).shouldBeNear(0.0, EPSILON)
        Chebyshev.compute(a, a).shouldBeNear(0.0, EPSILON)
    }
}
